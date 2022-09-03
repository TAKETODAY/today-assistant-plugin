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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.tab;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CheckedActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationUrlUtil;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.assistant.app.run.lifecycle.tabs.InfraEndpointsTabSettings;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

public class RequestMappingsTab extends EndpointTab<LiveRequestMappingsModel> {
  private static final String REQUEST_MAPPINGS = "REQUEST_MAPPINGS";
  private final List<LiveRequestMapping> myRequestMappings;
  private final JComponent myWrapper;
  private final RequestMappingsPanel myRequestMappingsPanel;
  private volatile boolean myMappingsEnabled;
  private final RequestMappingsEndpointTabSettings mySettings;

  public RequestMappingsTab(Endpoint<LiveRequestMappingsModel> endpoint, InfraApplicationRunConfigurationBase runConfiguration, ProcessHandler processHandler) {
    super(endpoint, runConfiguration, processHandler);
    this.myRequestMappings = new ArrayList();
    this.myMappingsEnabled = false;
    this.mySettings = RequestMappingsEndpointTabSettings.getInstance(getProject());
    this.myRequestMappingsPanel = new RequestMappingsPanel(getProject(), getRunConfiguration(), processHandler,
            ContainerUtil.newArrayList(new MarkAsDefaultAction(), new RestoreEmptyDefaultPathAction()));
    this.myRequestMappingsPanel.setDefaultPath(runConfiguration.getUrlPath());
    Disposer.register(this, this.myRequestMappingsPanel);
    this.myWrapper = DumbService.getInstance(getProject()).wrapGently(this.myRequestMappingsPanel, this);
    MessageBusConnection connection = getProject().getMessageBus().connect(this);
    connection.subscribe(RunManagerListener.TOPIC, new RunManagerListener() {

      public void runConfigurationChanged(RunnerAndConfigurationSettings settings) {
        RunConfiguration configuration = settings.getConfiguration();
        if (RequestMappingsTab.this.getRunConfiguration().getName().equals(configuration.getName())
                && (configuration instanceof InfraApplicationRunConfigurationBase applicationRunConfigurationBase)) {
          String defaultPath = applicationRunConfigurationBase.getUrlPath();
          RequestMappingsTab.this.getRunConfiguration().setUrlPath(defaultPath);
          RequestMappingsTab.this.myRequestMappingsPanel.setDefaultPath(defaultPath);
        }
      }
    });
    connection.subscribe(InfraEndpointsTabSettings.TOPIC, new InfraEndpointsTabSettings.Listener() {

      @Override
      public void settingsChanged(String changeType) {
        if (changeType.equals(REQUEST_MAPPINGS)) {
          RequestMappingsTab.this.myRequestMappingsPanel.setItems(Collections.unmodifiableList(RequestMappingsTab.this.myRequestMappings));
        }
      }
    });
  }

  @Override
  public String getTitle() {
    return InfraAppBundle.message("infra.application.endpoints.mappings.tab.title");
  }

  @Override

  public Icon getIcon() {
    Icon icon = Icons.RequestMapping;
    return icon;
  }

  @Override

  protected List<AnAction> getToolbarActions() {
    List<AnAction> actions = new ArrayList<>();
    actions.add(new OpenInBrowserAction());
    actions.add(new LiveRequestMethodActionGroup());
    actions.add(Separator.getInstance());
    actions.add(new EndpointToggleAction(InfraAppBundle.message("infra.application.endpoints.mappings.show.library.mappings.action.name"), null, AllIcons.Nodes.PpLib) {

      public boolean isSelected(AnActionEvent e) {
        return RequestMappingsTab.this.mySettings.isShowLibraryMappings();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        RequestMappingsTab.this.mySettings.setShowLibraryMappings(state);
        InfraEndpointsTabSettings.getInstance(RequestMappingsTab.this.getProject()).fireSettingsChanged(REQUEST_MAPPINGS);
      }
    });
    return actions;
  }

  @Override

  protected JComponent getEndpointComponent() {
    JComponent jComponent = this.myWrapper;
    return jComponent;
  }

  @Override
  public void doUpdateComponent(@Nullable LiveRequestMappingsModel model) {
    this.myRequestMappings.clear();
    if (model != null) {
      this.myRequestMappings.addAll(model.getRequestMappings());
    }
    this.myRequestMappingsPanel.setItems(Collections.unmodifiableList(this.myRequestMappings));
  }

  @Override
  public void checkAvailability() {
    super.checkAvailability();
    this.myMappingsEnabled = InfraLibraryUtil.hasRequestMappings(getRunConfiguration().getModule());
    if (isActuatorsEnabled() && !this.myMappingsEnabled) {
      showMessage(InfraAppBundle.message("infra.application.endpoints.mappings.disabled"));
    }
  }

  @Override
  public void updateRefreshAction(AnActionEvent e, InfraApplicationInfo info) {
    super.updateRefreshAction(e, info);
    if (!this.myMappingsEnabled) {
      e.getPresentation().setEnabled(false);
    }
  }

  private class MarkAsDefaultAction extends AnAction {

    MarkAsDefaultAction() {
      super(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.mark.default.action.name"), AllIcons.Actions.SetDefault);
    }

    public void update(AnActionEvent e) {
      LiveRequestMapping mapping = RequestMappingsTab.this.myRequestMappingsPanel.getSelectedMapping();
      String path = mapping != null ? mapping.getPath() : null;
      e.getPresentation().setEnabledAndVisible(path != null && !path.equals(RequestMappingsTab.this.getRunConfiguration().getUrlPath()) && mapping.canNavigate());
    }

    public void actionPerformed(AnActionEvent e) {
      LiveRequestMapping mapping = RequestMappingsTab.this.myRequestMappingsPanel.getSelectedMapping();
      String path = mapping != null ? mapping.getPath() : null;
      if (path == null) {
        return;
      }
      InfraApplicationUrlUtil.getInstance().updatePath(RequestMappingsTab.this.getProject(), RequestMappingsTab.this.getRunConfiguration(), path);
    }
  }

  private class RestoreEmptyDefaultPathAction extends AnAction {

    RestoreEmptyDefaultPathAction() {
      super(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.restore.empty.default.path.action.name"));
    }

    public void update(AnActionEvent e) {
      LiveRequestMapping mapping = RequestMappingsTab.this.myRequestMappingsPanel.getSelectedMapping();
      String path = mapping != null ? mapping.getPath() : null;
      e.getPresentation().setEnabledAndVisible(path != null && path.equals(RequestMappingsTab.this.getRunConfiguration().getUrlPath()));
    }

    public void actionPerformed(AnActionEvent e) {
      InfraApplicationUrlUtil.getInstance().updatePath(RequestMappingsTab.this.getProject(), RequestMappingsTab.this.getRunConfiguration(), "");
    }
  }

  private class LiveRequestMethodActionGroup extends DefaultActionGroup implements CheckedActionGroup {

    LiveRequestMethodActionGroup() {
      RequestMethod[] values;
      setPopup(true);
      getTemplatePresentation().setText(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.request.method.action.name"));
      getTemplatePresentation().setIcon(AllIcons.Nodes.Method);
      for (RequestMethod method : RequestMethod.values()) {
        String name = method.name();
        add(new EndpointToggleAction(name) {

          {
            RequestMappingsTab requestMappingsTab = RequestMappingsTab.this;
          }

          public boolean isSelected(AnActionEvent e) {
            return !RequestMappingsTab.this.mySettings.getFilteredRequestMethods().contains(method);
          }

          public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
              RequestMappingsTab.this.mySettings.getFilteredRequestMethods().remove(method);
            }
            else {
              RequestMappingsTab.this.mySettings.getFilteredRequestMethods().add(method);
            }
            InfraEndpointsTabSettings.getInstance(RequestMappingsTab.this.getProject()).fireSettingsChanged(REQUEST_MAPPINGS);
          }

          @Override
          public void update(AnActionEvent e) {
            super.update(e);
            if (!e.getPresentation().isEnabled()) {
              e.getPresentation().setVisible(false);
            }
          }
        });
      }
    }

    public void update(AnActionEvent e) {
      RequestMappingsTab.this.updateActionPresentation(e);
    }
  }

  private class OpenInBrowserAction extends AnAction {

    OpenInBrowserAction() {
      super(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.open.in.browser.action.name"), AllIcons.Nodes.PpWeb);
    }

    public void update(AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      InfraApplicationInfo info = RequestMappingsTab.this.getInfo();
      String applicationUrl = info == null ? null : info.getApplicationUrl().getValue();
      presentation.setEnabled(applicationUrl != null);
      if (applicationUrl == null) {
        presentation.setDescription(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.open.in.browser.action.description"));
      }
      else {
        presentation.setDescription(InfraAppBundle.messagePointer("infra.application.endpoints.mappings.open.url", applicationUrl));
      }
    }

    public void actionPerformed(AnActionEvent e) {
      String applicationUrl;
      InfraApplicationInfo info = RequestMappingsTab.this.getInfo();
      if (info != null && (applicationUrl = info.getApplicationUrl().getValue()) != null) {
        InfraApplicationUrlUtil urlUtil = InfraApplicationUrlUtil.getInstance();
        String mappingPath = RequestMappingsTab.this.getRunConfiguration().getUrlPath();
        String servletPath = urlUtil.getServletPath(info, mappingPath);
        BrowserUtil.browse(urlUtil.getMappingUrl(applicationUrl, servletPath, mappingPath));
      }
    }
  }
}
