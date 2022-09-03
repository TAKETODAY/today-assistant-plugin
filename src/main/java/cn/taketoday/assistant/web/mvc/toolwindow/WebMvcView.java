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

package cn.taketoday.assistant.web.mvc.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CheckedActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.util.messages.MessageBusConnection;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.toolWindow.InfraBaseView;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;

import static cn.taketoday.assistant.InfraAppBundle.message;

class WebMvcView extends InfraBaseView {
  private static final String TAB_ID = "MVC";

  WebMvcView(Project project) {
    super(project);
    installToolbar();
    installContentListeners();
    installSettingsListener();
  }

  private void installContentListeners() {
    MessageBusConnection messageBusConnection = installProjectModuleListener();
    messageBusConnection.subscribe(PsiModificationTracker.TOPIC, (PsiModificationTracker.Listener) () -> {
      myRootPanel.updatePanel();
    });
  }

  protected FinderRecursivePanel<?> createRootPanel() {
    WebMvcViewSettings settings = WebMvcViewSettings.getInstance(this.myProject);
    if (settings.isShowModules()) {
      return new WebMvcModulesPanel(this.myProject);
    }
    if (settings.isShowControllers()) {
      return new WebMvcControllerPanel(this.myProject);
    }
    return new WebMvcRequestMappingsPanel(this.myProject, "WebMvcRequestMappingsPanel");
  }

  private void installToolbar() {
    WebMvcViewSettings settings = WebMvcViewSettings.getInstance(this.myProject);
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new ToggleAction(message("WebMvcView.show.modules"), null, AllIcons.Actions.GroupByModule) {

      public boolean isSelected(AnActionEvent e) {
        return settings.isShowModules();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        settings.setShowModules(state);
        WebMvcViewSettings.fireSettingsChanged(WebMvcView.this.myProject, WebMvcViewSettings.ChangeType.FULL);
      }
    });
    group.add(new ToggleAction(message("WebMvcView.show.controllers"), null, Icons.SpringBean) {

      public boolean isSelected(AnActionEvent e) {
        return settings.isShowControllers();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        settings.setShowControllers(state);
        if (WebMvcView.this.myRootPanel instanceof WebMvcModulesPanel) {
          WebMvcViewSettings.fireSettingsChanged(WebMvcView.this.myProject, WebMvcViewSettings.ChangeType.REPLACE_CHILD_UPDATE);
        }
        else {
          WebMvcViewSettings.fireSettingsChanged(WebMvcView.this.myProject, WebMvcViewSettings.ChangeType.FULL);
        }
      }
    });
    group.add(Separator.getInstance());
    group.add(new RequestMethodActionGroup(this.myProject, settings));
    group.add(Separator.getInstance());
    group.add(new ToggleAction(message("WebMvcView.designer.properties.show.javadoc"), null, AllIcons.Toolwindows.Documentation) {

      public boolean isSelected(AnActionEvent e) {
        return settings.isShowDoc();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        settings.setShowDoc(state);
        WebMvcViewSettings.fireSettingsChanged(WebMvcView.this.myProject, WebMvcViewSettings.ChangeType.UPDATE_DETAILS);
      }
    });
    group.addSeparator();
    group.add(getHelpAction());
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("WebMvcView", group, false);
    toolbar.setTargetComponent(this);
    setToolbar(toolbar.getComponent());
  }

  private void installSettingsListener() {
    this.myProject.getMessageBus().connect(this).subscribe(WebMvcViewSettings.TOPIC, (WebMvcViewSettings.Listener) changeType -> {
      if (changeType == WebMvcViewSettings.ChangeType.FULL) {
        performFullUpdate();
      }
      else if (changeType == WebMvcViewSettings.ChangeType.UPDATE_DETAILS) {
        performDetailsUpdate();
      }
      else if (changeType == WebMvcViewSettings.ChangeType.UPDATE_LIST) {
        this.myRootPanel.updatePanel();
      }
      else if (changeType == WebMvcViewSettings.ChangeType.REPLACE_CHILD_UPDATE) {
        this.myRootPanel.updateRightComponent(true);
      }
    });
  }

  static void selectIn(Project project, Object[] pathToSelect, boolean requestFocus) {
    select(project, pathToSelect, requestFocus, TAB_ID);
  }

  private static final class RequestMethodActionGroup extends DefaultActionGroup implements CheckedActionGroup {

    private RequestMethodActionGroup(Project project, WebMvcViewSettings settings) {
      setPopup(true);
      getTemplatePresentation().setText(message("WebMvcView.request.method"));
      getTemplatePresentation().setIcon(AllIcons.Nodes.Method);
      for (RequestMethod method : RequestMethod.values()) {
        String name = method.name();
        add(new ToggleAction(name) {

          public boolean isSelected(AnActionEvent e) {
            return settings.getRequestMethods().contains(method);
          }

          public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
              settings.getRequestMethods().add(method);
            }
            else {
              settings.getRequestMethods().remove(method);
            }
            WebMvcViewSettings.fireSettingsChanged(project, WebMvcViewSettings.ChangeType.UPDATE_LIST);
          }
        });
      }
    }
  }
}
