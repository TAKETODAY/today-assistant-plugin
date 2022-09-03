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
import com.intellij.openapi.util.NlsContexts;
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
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConfigurationException;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.LiveProperty;
import cn.taketoday.lang.Nullable;

/**
 * @author konstantin.aleev
 */
public abstract class EndpointTab<T> implements Disposable, DataProvider {
  private static final String MESSAGE_CARD = "message";
  private static final String ENDPOINT_CARD = "endpoint";

  private final Endpoint<T> myEndpoint;
  private final InfraApplicationRunConfigurationBase myRunConfiguration;
  private final ProcessHandler myProcessHandler;
  private final LiveProperty.LivePropertyListener myEndpointListener;

  private final CardLayout myRootPanelLayout;
  private final JPanel myRootPanel;
  private final JBLoadingPanel myMessagePanel;
  private final JLabel myMessageLabel;

  private TooltipChangeListener myTooltipChangeListener;

  private volatile boolean myActuatorsEnabled = false;

  protected EndpointTab(Endpoint<T> endpoint, InfraApplicationRunConfigurationBase runConfiguration,
          ProcessHandler processHandler) {
    myEndpoint = endpoint;
    myRunConfiguration = runConfiguration;
    myProcessHandler = processHandler;

    myEndpointListener = new LiveProperty.LivePropertyListener() {
      @Override
      public void propertyChanged() {
        AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
          if (myMessagePanel.isLoading()) {
            myMessagePanel.stopLoading();
          }
          myRootPanelLayout.show(myRootPanel, ENDPOINT_CARD);
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

        Set<Throwable> causes = new HashSet<>();
        Throwable parent = e;
        Throwable cause = e.getCause();
        causes.add(parent);
        while (cause != null && !causes.contains(cause)) {
          messageBuilder.append("<br>").append(InfraAppBundle.message("infra.application.endpoints.error.caused.by",
                  cause.getClass().getName(),
                  cause.getLocalizedMessage()));
          parent = cause;
          cause = parent.getCause();
          causes.add(parent);
        }
        showMessage(getErrorMessage(messageBuilder.toString()));
      }
    };

    myRootPanelLayout = new CardLayout();
    myRootPanel = new JPanel(myRootPanelLayout);

    myMessagePanel = new JBLoadingPanel(new GridBagLayout(), this);
    final String name = StringUtil.shortenTextWithEllipsis(myRunConfiguration.getName(), 30, 3);
    myMessagePanel.setLoadingText(InfraAppBundle.message("infra.application.endpoints.application.is.starting", name));
    myMessagePanel.startLoading();

    myMessageLabel = new JBLabel();
    myMessageLabel.setForeground(JBColor.GRAY);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 0.66;
    myMessagePanel.add(myMessageLabel, gbc);

    // Add bottom spacer
    gbc.weighty = 0.33;
    gbc.gridy = 1;
    myMessagePanel.add(new JBLabel(), gbc);

    myRootPanel.add(MESSAGE_CARD, ScrollPaneFactory.createScrollPane(myMessagePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
  }

  public String getId() {
    return myEndpoint.getId();
  }

  public Endpoint<T> getEndpoint() {
    return myEndpoint;
  }

  public abstract String getTitle();

  public abstract Icon getIcon();

  @Override
  public void dispose() {
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      getLiveProperty(info).removePropertyListener(myEndpointListener);
    }
    myTooltipChangeListener = null;
  }

  @Nullable
  @Override
  public Object getData(String dataId) {
    return null;
  }

  public final JComponent getComponent() {
    myRootPanel.add(ENDPOINT_CARD, getEndpointComponent());
    return myRootPanel;
  }

  public final void refresh() {
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      getLiveProperty(info).compute();
    }
  }

  public final void initPropertyListeners(InfraApplicationInfo info) {
    getLiveProperty(info).addPropertyListener(myEndpointListener);
    doInitPropertyListeners(info);
  }

  public final void showMessage(@Nullable String message) {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      if (myMessagePanel.isLoading()) {
        myMessagePanel.stopLoading();
      }
      myRootPanelLayout.show(myRootPanel, MESSAGE_CARD);
      myMessageLabel.setText(message);
    });
  }

  public final void showLoading() {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      myMessageLabel.setText(null);
      myMessagePanel.startLoading();
      myRootPanelLayout.show(myRootPanel, MESSAGE_CARD);
    });
  }

  public final void setTooltipChangeListener(@Nullable TooltipChangeListener tooltipChangeListener) {
    myTooltipChangeListener = tooltipChangeListener;
  }

  protected final Project getProject() {
    return myRunConfiguration.getProject();
  }

  protected final boolean isActuatorsEnabled() {
    return myActuatorsEnabled;
  }

  public void checkAvailability() {
    myActuatorsEnabled = InfraLibraryUtil.hasActuators(getRunConfiguration().getModule());
    if (!myActuatorsEnabled) {
      showMessage(InfraAppBundle.message("infra.application.endpoints.error.actuator.starter.disabled"));
    }
  }

  public void updateRefreshAction(AnActionEvent e, InfraApplicationInfo info) {
    e.getPresentation().setEnabled(myActuatorsEnabled);
  }

  protected final InfraApplicationRunConfigurationBase getRunConfiguration() {
    return myRunConfiguration;
  }

  protected final ProcessHandler getProcessHandler() {
    return myProcessHandler;
  }

  protected final JPanel getRootPanel() {
    return myRootPanel;
  }

  private void setTimeStampTooltip(long timeStamp) {
    if (myTooltipChangeListener != null) {
      String message = null;
      if (timeStamp > 0) {
        message =
                InfraAppBundle.message("infra.application.endpoints.updated.at", DateFormatUtil.formatTimeWithSeconds(timeStamp));
      }
      myTooltipChangeListener.tooltipChanged(message);
    }
  }

  protected final boolean isTabLoading() {
    return myMessagePanel.isLoading();
  }

  @Nullable
  protected final InfraApplicationInfo getInfo() {
    Project project = getProject();
    if (!project.isOpen() || project.isDisposed()) {
      return null;
    }
    return InfraApplicationLifecycleManager.from(project).getInfraApplicationInfo(myProcessHandler);
  }

  protected final void infoRemoved() {
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      if (myMessagePanel.isLoading()) {
        myMessagePanel.stopLoading();
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

  protected void doInitPropertyListeners(InfraApplicationInfo info) {
  }

  protected @NlsContexts.Label String getErrorMessage(String cause) {
    return InfraAppBundle.message("infra.application.endpoints.error.failed.to.retrieve.endpoint.data.detailed",
            myEndpoint.getId(),
            cause);
  }

  protected LiveProperty<T> getLiveProperty(InfraApplicationInfo info) {
    return info.getEndpointData(myEndpoint);
  }

  protected abstract JComponent getEndpointComponent();

  private void updateComponent() {
    InfraApplicationInfo info = InfraApplicationLifecycleManager.from(getProject())
            .getInfraApplicationInfo(getProcessHandler());
    LiveProperty<T> liveProperty = info != null ? getLiveProperty(info) : null;
    final T value = liveProperty != null ? liveProperty.getValue() : null;
    final long timeStamp = liveProperty != null ? liveProperty.getTimeStamp() : -1L;
    AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
      setTimeStampTooltip(timeStamp);
      doUpdateComponent(value);
    });
  }

  protected abstract void doUpdateComponent(@Nullable T value);

  interface TooltipChangeListener {
    void tooltipChanged(@Nullable @NlsContexts.Tooltip String tooltip);
  }

  protected abstract class EndpointToggleAction extends ToggleAction {
    protected EndpointToggleAction(@Nullable @NlsActions.ActionText String text) {
      super(text);
    }

    protected EndpointToggleAction(@Nullable @NlsActions.ActionText String text,
            @Nullable @NlsActions.ActionDescription String description,
            @Nullable final Icon icon) {
      super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      updateActionPresentation(e);
    }
  }
}
