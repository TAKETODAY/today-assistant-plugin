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

package cn.taketoday.assistant.toolWindow.panels;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NullableFactory;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.BeansInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.profiles.ChangeActiveProfilesAction;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.toolWindow.InfraBeansViewSettings;
import cn.taketoday.assistant.toolWindow.InfraModulesPanelBase;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraBeansModulesPanel extends InfraModulesPanelBase {
  private final DefaultActionGroup myStructureViewActionGroup;
  private ToggleAction myShowApplicationContextsAction;

  public InfraBeansModulesPanel(Project project, DefaultActionGroup structureViewActionGroup) {
    super(project, "InfraBeansModulesPanel");
    this.myStructureViewActionGroup = structureViewActionGroup;
    installActions();
  }

  private void installActions() {
    this.myShowApplicationContextsAction = new ToggleAction(message("InfraBeansView.show.application.contexts"), null, Icons.FileSet) {

      public boolean isSelected(AnActionEvent e) {
        return InfraBeansViewSettings.from(getProject()).isShowFileSets();
      }

      public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(InfraBeansViewSettings.from(getProject()).isShowModules());
      }

      public void setSelected(AnActionEvent e, boolean state) {
        InfraBeansViewSettings.from(getProject()).setShowFileSets(state);
        updateRightComponent(true);
      }
    };
    this.myStructureViewActionGroup.add(this.myShowApplicationContextsAction);
  }

  public void dispose() {
    this.myStructureViewActionGroup.remove(this.myShowApplicationContextsAction);
    super.dispose();
  }

  protected AnAction[] getCustomListActions() {
    return new AnAction[] { ActionManager.getInstance().getAction(ChangeActiveProfilesAction.ACTION_ID) };
  }

  @Nullable
  public JComponent createRightComponent(Module module) {
    Project project = getProject();
    boolean autoConfigurationMode = InfraGeneralSettings.from(project).isAllowAutoConfigurationMode();
    InfraFacet infraFacet = InfraFacet.from(module);
    if (isAutoConfigurationModeAllowed(autoConfigurationMode, infraFacet)) {
      return new InfraAutoConfiguredFilesFinderRecursivePanel(this, module);
    }
    else if (infraFacet != null && InfraBeansViewSettings.from(project).isShowFileSets()) {
      return new InfraFileSetFinderRecursivePanel(this, module, this.myStructureViewActionGroup);
    }
    else {
      NullableFactory<CommonInfraModel> factory = () -> {
        return new BeansInfraModel(null, NotNullLazyValue.lazy(() -> {
          Set<BeanPointer<?>> allBeans = new HashSet<>();

          for (InfraModel model : InfraManager.from(project).getAllModelsWithoutDependencies(module)) {
            allBeans.addAll(model.getAllCommonBeans());
          }

          return allBeans;
        }));
      };
      return new InfraBeanPointerFinderRecursivePanel(this, factory);
    }
  }

  private static boolean isAutoConfigurationModeAllowed(boolean autoConfigurationMode, InfraFacet infraFacet) {
    return autoConfigurationMode && (infraFacet == null || InfraFileSetService.of().getAllSets(infraFacet).isEmpty());
  }

  @Override
  public boolean hasChildren(Module module) {
    if (!module.isDisposed() && !DumbService.isDumb(getProject())) {
      InfraFacet infraFacet = InfraFacet.from(module);
      return isAutoConfigurationModeAllowed(InfraGeneralSettings.from(getProject()).isAllowAutoConfigurationMode(), infraFacet)
             ? !InfraAutoConfiguredModels.discoverAutoConfiguredModels(
              module).isEmpty() : infraFacet != null && !InfraFileSetService.of().getAllSets(infraFacet).isEmpty();
    }
    return false;
  }
}
