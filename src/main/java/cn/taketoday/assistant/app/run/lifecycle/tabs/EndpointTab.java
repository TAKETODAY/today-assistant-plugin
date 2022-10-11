/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.assistant.app.run.lifecycle.tabs;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.text.DateFormatUtil;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConfigurationException;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.Property;
import cn.taketoday.lang.Nullable;

/**
 * @author konstantin.aleev
 */
public abstract class EndpointTab<T> implements Disposable, DataProvider {
  private static final String MESSAGE_CARD = "message";
  private static final String ENDPOINT_CARD = "endpoint";

  private final Endpoint<T> endpoint;
  private final InfraApplicationRunConfig runConfiguration;

  private final ProcessHandler processHandler;
  private final Property.PropertyListener endpointListener;

  private final JPanel rootPanel;
  private final JLabel messageLabel;
  private final CardLayout rootPanelLayout;
  private final JBLoadingPanel messagePanel;

  private TooltipChangeListener tooltipChangeListener;

  private volatile boolean actuatorsEnabled;

  protected EndpointTab(Endpoint<T> endpoint, InfraApplicationRunConfig runConfiguration,
          ProcessHandler processHandler) {
    this.endpoint = endpoint;
    this.runConfiguration = runConfiguration;
    this.processHandler = processHandler;

    endpointListener = new Property.PropertyListener() {
      @Override
      public void propertyChanged() {
        AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
          if (messagePanel.isLoading()) {
            messagePanel.stopLoading();
          }
          rootPanelLayout.show(rootPanel, ENDPOINT_CARD);
        });
        updateComponent();
      }

      @Override
      public void computationFailed(Exception e) {
        StringBuilder messageBuilder = new StringBuilder();
        if (!(e instanceof InfraApplicationConfigurationException)) {
          messageBuilder.append(e.getClass().getName()).append(": ");
        }
        messageBuilder.append(e.getLocalizedMessage());

        HashSet<Throwable> causes = new HashSet<>();
        Throwable parent = e;
        Throwable cause = e.getCause();
        causes.add(parent);
        while (cause != null && !causes.contains(cause)) {
          messageBuilder.append("<br>")
                  .append(InfraAppBundle.message("infra.app.endpoints.error.caused.by",
                          cause.getClass().getName(),
                          cause.getLocalizedMessage()));
          parent = cause;
          cause = parent.getCause();
          causes.add(parent);
        }
        showMessage(getErrorMessage(messageBuilder.toString()));
      }
    };

    rootPanelLayout = new CardLayout();
    rootPanel = new JPanel(rootPanelLayout);

    messagePanel = new JBLoadingPanel(new GridBagLayout(), this);
    String name = StringUtil.shortenTextWithEllipsis(this.runConfiguration.getName(), 30, 3);
    messagePanel.setLoadingText(InfraAppBundle.message("infra.app.endpoints.application.is.starting", name));
    messagePanel.startLoading();

    messageLabel = new JBLabel();
    messageLabel.setForeground(JBColor.GRAY);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 0.66;
    messagePanel.add(messageLabel, gbc);

    // Add bottom spacer
    gbc.weighty = 0.33;
    gbc.gridy = 1;
    messagePanel.add(new JBLabel(), gbc);

    rootPanel.add(MESSAGE_CARD, ScrollPaneFactory.createScrollPane(messagePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
  }

  public String getId() {
    return endpoint.getId();
  }

  public Endpoint<T> getEndpoint() {
    return endpoint;
  }

  public abstract String getTitle();

  public abstract Icon getIcon();

  @Override
  public void dispose() {
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      getLiveProperty(info).removePropertyListener(endpointListener);
    }
    this.tooltipChangeListener = null;
  }

  @Nullable
  @Override
  public Object getData(String dataId) {
    return null;
  }

  public final JComponent getComponent() {
    rootPanel.add(ENDPOINT_CARD, getEndpointComponent());
    return rootPanel;
  }

  public final void refresh() {
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      getLiveProperty(info).compute();
    }
  }

  public final void initPropertyListeners(InfraApplicationInfo info) {
    getLiveProperty(info).addPropertyListener(endpointListener);
    doInitPropertyListeners(info);
  }

  public final void showMessage(@Nullable String message) {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      if (messagePanel.isLoading()) {
        messagePanel.stopLoading();
      }
      rootPanelLayout.show(rootPanel, MESSAGE_CARD);
      messageLabel.setText(message);
    });
  }

  public final void showLoading() {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      messageLabel.setText(null);
      messagePanel.startLoading();
      rootPanelLayout.show(rootPanel, MESSAGE_CARD);
    });
  }

  public final void setTooltipChangeListener(@Nullable TooltipChangeListener tooltipChangeListener) {
    this.tooltipChangeListener = tooltipChangeListener;
  }

  protected final Project getProject() {
    return runConfiguration.getProject();
  }

  protected final boolean isActuatorsEnabled() {
    return actuatorsEnabled;
  }

  public void checkAvailability() {
    actuatorsEnabled = InfraLibraryUtil.hasActuators(runConfiguration.getModule());
    if (!actuatorsEnabled) {
      showMessage(InfraAppBundle.message("infra.app.endpoints.error.actuator.starter.disabled"));
    }
  }

  public void updateRefreshAction(AnActionEvent e, InfraApplicationInfo info) {
    e.getPresentation().setEnabled(actuatorsEnabled);
  }

  protected final InfraApplicationRunConfig getRunConfiguration() {
    return runConfiguration;
  }

  protected final ProcessHandler getProcessHandler() {
    return processHandler;
  }

  protected final JPanel getRootPanel() {
    return rootPanel;
  }

  private void setTimeStampTooltip(long timeStamp) {
    if (tooltipChangeListener != null) {
      String message = null;
      if (timeStamp > 0) {
        message = InfraAppBundle.message(
                "infra.app.endpoints.updated.at", DateFormatUtil.formatTimeWithSeconds(timeStamp));
      }
      tooltipChangeListener.tooltipChanged(message);
    }
  }

  protected final boolean isTabLoading() {
    return messagePanel.isLoading();
  }

  @Nullable
  protected final InfraApplicationInfo getInfo() {
    Project project = getProject();
    if (!project.isOpen() || project.isDisposed()) {
      return null;
    }
    return InfraApplicationLifecycleManager.from(project).getInfraApplicationInfo(processHandler);
  }

  protected final void infoRemoved() {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      if (messagePanel.isLoading()) {
        messagePanel.stopLoading();
      }
    });
  }

  protected final void updateActionPresentation(AnActionEvent e) {
    InfraApplicationInfo info = getInfo();
    e.getPresentation().setEnabled(info != null && Boolean.TRUE.equals(info.getReadyState().getValue()) && !isTabLoading());
  }

  protected List<AnAction> getToolbarActions() {
    return Collections.emptyList();
  }

  protected void doInitPropertyListeners(InfraApplicationInfo info) { }

  protected String getErrorMessage(String cause) {
    return InfraAppBundle.message("infra.app.endpoints.error.failed.to.retrieve.endpoint.data.detailed",
            endpoint.getId(), cause);
  }

  protected Property<T> getLiveProperty(InfraApplicationInfo info) {
    return info.getEndpointData(endpoint);
  }

  protected abstract JComponent getEndpointComponent();

  private void updateComponent() {
    InfraApplicationInfo info = InfraApplicationLifecycleManager.from(getProject())
            .getInfraApplicationInfo(processHandler);
    Property<T> property = info != null ? getLiveProperty(info) : null;
    T value = property != null ? property.getValue() : null;
    long timeStamp = property != null ? property.getTimeStamp() : -1L;
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      setTimeStampTooltip(timeStamp);
      doUpdateComponent(value);
    });
  }

  protected abstract void doUpdateComponent(@Nullable T value);

  interface TooltipChangeListener {

    void tooltipChanged(@Nullable String tooltip);
  }

  protected abstract class EndpointToggleAction extends ToggleAction {

    protected EndpointToggleAction(@Nullable @NlsActions.ActionText String text) {
      super(text);
    }

    protected EndpointToggleAction(@Nullable @NlsActions.ActionText String text,
            @Nullable @NlsActions.ActionDescription String description,
            @Nullable Icon icon) {
      super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      updateActionPresentation(e);
    }
  }
}
