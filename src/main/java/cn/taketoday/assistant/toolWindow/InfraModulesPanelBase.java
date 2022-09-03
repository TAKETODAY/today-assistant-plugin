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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.ui.FinderRecursivePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class InfraModulesPanelBase extends FinderRecursivePanel<Module> {

  protected InfraModulesPanelBase(Project project, String groupId) {
    super(project, groupId);

    setNonBlockingLoad(true);
  }

  @Override
  public List<Module> getListItems() {
    boolean autoConfigurationMode = InfraGeneralSettings.from(getProject())
            .isAllowAutoConfigurationMode();

    if (!InfraUtils.hasFacets(getProject()) && !autoConfigurationMode) {
      return Collections.emptyList();
    }

    List<Module> items = new ArrayList<>();
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    for (Module module : modules) {
      if (autoConfigurationMode || InfraUtils.hasFacet(module)) {
        items.add(module);
      }
    }
    items.sort(ModulesAlphaComparator.INSTANCE);
    return items;
  }

  @Override
  public String getItemText(Module module) {
    return module.getName();
  }

  @Nullable
  @Override
  public Icon getItemIcon(Module module) {
    return module.isDisposed() ? null : ModuleType.get(module).getIcon();
  }

  @Override
  public boolean performEditAction() {
    Module module = getSelectedValue();
    if (module != null) {
      ModulesConfigurator.showDialog(getProject(), module.getName(), null);
    }
    return true;
  }

  @Override
  public boolean hasChildren(Module module) {
    if (module.isDisposed())
      return false;
    if (DumbService.isDumb(getProject()))
      return false;

    InfraFacet infraFacet = InfraFacet.from(module);
    if (infraFacet == null)
      return false;
    return !InfraFileSetService.of().getAllSets(infraFacet).isEmpty();
  }
}