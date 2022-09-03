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
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.impl.InfraCombinedModelFactory;
import cn.taketoday.assistant.profiles.ChangeActiveProfilesAction;
import cn.taketoday.assistant.toolWindow.InfraBeansViewSettings;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraFileSetFinderRecursivePanel extends FinderRecursivePanel<InfraFileSet> {
  private static final Condition<InfraFileSet> FILESET_NOT_REMOVED_CONDITION;
  private final Module myModule;

  private final DefaultActionGroup myStructureViewActionGroup;
  private final ToggleAction myShowConfigurationAction;

  static {
    FILESET_NOT_REMOVED_CONDITION = set -> !set.isRemoved();
  }

  public InfraFileSetFinderRecursivePanel(InfraBeansModulesPanel panel, Module module, DefaultActionGroup structureViewActionGroup) {
    super(panel);
    this.myModule = module;
    this.myStructureViewActionGroup = structureViewActionGroup;
    this.myShowConfigurationAction = new ToggleAction(message("beans.view.show.configuration.files"), null, Icons.SpringConfig) {
      public boolean isSelected(AnActionEvent e) {
        return InfraBeansViewSettings.from(InfraFileSetFinderRecursivePanel.this.getProject()).isShowFiles();
      }

      public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(InfraBeansViewSettings.from(InfraFileSetFinderRecursivePanel.this.getProject()).isShowModules() && InfraBeansViewSettings.from(
                InfraFileSetFinderRecursivePanel.this.getProject()).isShowFileSets());
      }

      public void setSelected(AnActionEvent e, boolean state) {
        InfraBeansViewSettings.from(InfraFileSetFinderRecursivePanel.this.getProject()).setShowFiles(state);
        InfraFileSetFinderRecursivePanel.this.updateRightComponent(true);
      }
    };
    this.myStructureViewActionGroup.add(this.myShowConfigurationAction);
  }

  protected List<InfraFileSet> getListItems() {
    InfraFacet infraFacet = InfraFacet.from(this.myModule);
    Set<InfraFileSet> fileSets = InfraFileSetService.of().getAllSets(infraFacet);
    return ContainerUtil.filter(fileSets, FILESET_NOT_REMOVED_CONDITION);
  }

  @Nullable
  public Object getData(String dataId) {
    if (PlatformCoreDataKeys.SELECTED_ITEM.is(dataId)) {
      return getSelectedValue();
    }
    if (PlatformCoreDataKeys.MODULE.is(dataId)) {
      InfraFileSet value = getSelectedValue();
      if (value != null) {
        return value.getFacet().getModule();
      }
      return null;
    }
    return super.getData(dataId);
  }

  protected AnAction[] getCustomListActions() {
    return new AnAction[] { ActionManager.getInstance().getAction(ChangeActiveProfilesAction.ACTION_ID) };
  }

  public void dispose() {
    this.myStructureViewActionGroup.remove(this.myShowConfigurationAction);
    super.dispose();
  }

  protected String getListEmptyText() {
    return InfraBundle.message("InfraBeansView.config.no.contexts.defined");
  }

  public String getItemText(InfraFileSet fileSet) {
    return fileSet.getName();
  }

  @Nullable
  public Icon getItemIcon(InfraFileSet fileSet) {
    return fileSet.getIcon();
  }

  public void doCustomizeCellRenderer(SimpleColoredComponent comp, JList list, InfraFileSet value, int index, boolean selected, boolean hasFocus) {
    comp.clear();
    comp.setIcon(getItemIcon(value));
    comp.append(getItemText(value));
    if (value.isAutodetected()) {
      comp.append(" " + InfraBundle.message("facet.context.autodetected.suffix"), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
  }

  protected boolean performEditAction() {
    InfraFacet infraFacet = InfraFacet.from(this.myModule);
    if (infraFacet != null) {
      ModulesConfigurator.showFacetSettingsDialog(infraFacet, null);
      return true;
    }
    return true;
  }

  @Nullable
  public JComponent createRightComponent(InfraFileSet fileSet) {

    if (InfraBeansViewSettings.from(this.getProject()).isShowFiles()) {
      return new InfraConfigFilesFinderRecursivePanel(this, fileSet, this.myModule);
    }
    else {
      NullableFactory<CommonInfraModel> factory = () -> {
        return InfraCombinedModelFactory.createModel(fileSet, this.myModule);
      };
      return new InfraBeanPointerFinderRecursivePanel(this, factory);
    }
  }

  public boolean hasChildren(InfraFileSet fileSet) {
    return !fileSet.getFiles().isEmpty();
  }
}
