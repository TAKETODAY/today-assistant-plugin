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

package cn.taketoday.assistant.toolWindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.ui.FinderRecursivePanel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.BeansInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.toolWindow.panels.InfraBeanPointerFinderRecursivePanel;
import cn.taketoday.assistant.toolWindow.panels.InfraBeansModulesPanel;

import static cn.taketoday.assistant.InfraBundle.message;

final class InfraBeansView extends InfraBaseView {
  private static final String TAB_ID = "Beans";
  private DefaultActionGroup myStructureViewActionGroup;
  private DefaultActionGroup myConfigurationViewActionGroup;
  private DefaultActionGroup myPresentationActionGroup;
  private InfraBeansViewSettings myInfraBeansViewSettings;

  public InfraBeansView(Project project) {
    super(project);
    installToolbar();
    installSettingsListener();
    installProjectModuleListener();
  }

  @Override
  protected FinderRecursivePanel<?> createRootPanel() {
    this.myInfraBeansViewSettings = InfraBeansViewSettings.from(this.myProject);
    if (myInfraBeansViewSettings.isShowModules()) {
      return new InfraBeansModulesPanel(this.myProject, getStructureViewActionGroup());
    }
    return createSpringBeansProjectPanel();
  }

  public static void selectIn(Project project, Object[] pathToSelect, boolean requestFocus) {
    select(project, pathToSelect, requestFocus, TAB_ID);
  }

  private InfraBeansViewSettings getSettings() {
    return this.myInfraBeansViewSettings;
  }

  private InfraBeanPointerFinderRecursivePanel createSpringBeansProjectPanel() {
    NullableFactory<CommonInfraModel> factory = () -> {
      return new BeansInfraModel(null, NotNullLazyValue.lazy(() -> {
        Set<BeanPointer<?>> allBeans = new HashSet<>();
        for (Module module : ModuleManager.getInstance(this.myProject).getModules()) {
          for (InfraModel model : InfraManager.from(this.myProject).getAllModelsWithoutDependencies(module)) {
            Collection<BeanPointer<?>> beans = model.getAllCommonBeans();
            allBeans.addAll(beans);
          }
        }
        return allBeans;
      }));
    };
    return new InfraBeanPointerFinderRecursivePanel(this.myProject, "InfraBeanPointerFinderRecursivePanel", factory);
  }

  private void installToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(getStructureViewActionGroup());
    group.addSeparator();
    group.add(getConfigurationViewActionGroup());
    group.addSeparator();
    group.add(getPresentationActionGroup());
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("InfraBeansView", group, false);
    toolbar.setTargetComponent(this);
    setToolbar(toolbar.getComponent());
  }

  private void installSettingsListener() {
    this.myProject.getMessageBus().connect(this).subscribe(InfraBeansViewSettings.TOPIC, new InfraBeansViewSettings.Listener() {
      @Override
      public void settingsChanged(InfraBeansViewSettings.ChangeType changeType) {
        if (changeType == InfraBeansViewSettings.ChangeType.FULL) {
          InfraBeansView.this.performFullUpdate();
        }
        else if (changeType == InfraBeansViewSettings.ChangeType.UPDATE_DETAILS) {
          InfraBeansView.this.performDetailsUpdate();
        }
        else if (changeType == InfraBeansViewSettings.ChangeType.UPDATE_LIST) {
          InfraBeansView.this.myRootPanel.updatePanel();
        }
        else if (changeType == InfraBeansViewSettings.ChangeType.FORCE_UPDATE_RIGHT_COMPONENT) {
          InfraBeansView.this.myRootPanel.updateRightComponent(true);
        }
      }
    });
  }

  private DefaultActionGroup getStructureViewActionGroup() {
    if (this.myStructureViewActionGroup == null) {
      this.myStructureViewActionGroup = new DefaultActionGroup();
      this.myStructureViewActionGroup.add(new ToggleAction(message("InfraBeansView.show.modules"), null, AllIcons.Actions.GroupByModule) {

        public boolean isSelected(AnActionEvent e) {
          return InfraBeansView.this.getSettings().isShowModules();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          InfraBeansViewSettings settings = InfraBeansView.this.getSettings();
          settings.setShowModules(state);
          settings.fireSettingsChanged(InfraBeansViewSettings.ChangeType.FULL);
        }
      });
    }
    return this.myStructureViewActionGroup;
  }

  private DefaultActionGroup getConfigurationViewActionGroup() {
    if (this.myConfigurationViewActionGroup == null) {
      this.myConfigurationViewActionGroup = new DefaultActionGroup();
      this.myConfigurationViewActionGroup.add(new ToggleAction(message("InfraBeansView.show.implicit.beans"), null, Icons.ImplicitBean) {

        public boolean isSelected(AnActionEvent e) {
          return InfraBeansView.this.getSettings().isShowImplicitBeans();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          InfraBeansView.this.getSettings().setShowImplicitBeans(state);
          InfraBeansView.this.getSettings().fireSettingsChanged(InfraBeansViewSettings.ChangeType.UPDATE_LIST);
        }
      });
      this.myConfigurationViewActionGroup.add(new ToggleAction(message("InfraBeansView.show.infrastructure.beans"), null, Icons.InfrastructureBean) {

        public boolean isSelected(AnActionEvent e) {
          return InfraBeansView.this.getSettings().isShowInfrastructureBeans();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          InfraBeansView.this.getSettings().setShowInfrastructureBeans(state);
          InfraBeansView.this.getSettings().fireSettingsChanged(InfraBeansViewSettings.ChangeType.UPDATE_LIST);
        }
      });
    }
    return this.myConfigurationViewActionGroup;
  }

  private DefaultActionGroup getPresentationActionGroup() {
    if (this.myPresentationActionGroup == null) {
      this.myPresentationActionGroup = new DefaultActionGroup();
      this.myPresentationActionGroup.add(new ToggleAction(InfraBundle.message("beans.view.show.bean.documentation.title"), null, AllIcons.Toolwindows.Documentation) {

        public boolean isSelected(AnActionEvent e) {
          return InfraBeansView.this.getSettings().isShowDoc();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          InfraBeansView.this.getSettings().setShowDoc(state);
          InfraBeansView.this.getSettings().fireSettingsChanged(InfraBeansViewSettings.ChangeType.UPDATE_DETAILS);
        }
      });
      if (InfraBeanPointerFinderRecursivePanel.EP_NAME.getExtensions().length != 0) {
        this.myPresentationActionGroup.add(new ToggleAction(InfraBundle.message("beans.view.show.bean.graph.title"), null, AllIcons.FileTypes.Diagram) {

          public boolean isSelected(AnActionEvent e) {
            return InfraBeansView.this.getSettings().isShowGraph();
          }

          public void setSelected(AnActionEvent e, boolean state) {
            InfraBeansView.this.getSettings().setShowGraph(state);
            InfraBeansView.this.getSettings().fireSettingsChanged(InfraBeansViewSettings.ChangeType.UPDATE_DETAILS);
          }
        });
      }
    }
    return this.myPresentationActionGroup;
  }
}
