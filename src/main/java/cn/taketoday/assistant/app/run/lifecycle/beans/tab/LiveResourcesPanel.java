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

package cn.taketoday.assistant.app.run.lifecycle.beans.tab;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.NaturalComparator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.FinderRecursivePanel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.assistant.app.run.statistics.InfraRunUsageTriggerCollector;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

class LiveResourcesPanel extends LifecycleFinderRecursivePanel<LiveResourcesPanel.LiveResourceItem> {
  private static final String LIVE_RESOURCES_PANEL_GROUP_ID = "LiveResourcesPanel";

  LiveResourcesPanel(Project project, InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    this(project, null, LIVE_RESOURCES_PANEL_GROUP_ID, runConfiguration, processHandler);
  }

  LiveResourcesPanel(Project project, @Nullable FinderRecursivePanel parent,
          @Nullable String groupId, InfraApplicationRunConfig runConfiguration,
          ProcessHandler processHandler) {
    super(project, parent, groupId, runConfiguration, processHandler);
  }

  protected List<LiveResourceItem> getListItems() {
    List<LiveResource> resources;
    FinderRecursivePanel<?> parent = getParentPanel();
    if (parent != null) {
      if (parent.getSelectedValue() instanceof LiveContext) {
        resources = ((LiveContext) parent.getSelectedValue()).getResources();
      }
      else {
        return Collections.emptyList();
      }
    }
    else {
      LiveBeansModel model = getModel();
      if (model == null) {
        return Collections.emptyList();
      }
      resources = model.getResources();
    }
    InfraApplicationRunConfig runConfiguration = getRunConfiguration();
    Module module = runConfiguration.getModule();
    PsiClass mainClass = runConfiguration.getMainClass();
    GlobalSearchScope searchScope = getRunConfiguration().getSearchScope();
    Stream<LiveResourceItem> map = resources.stream().map(resource -> {
      return new LiveResourceItem(getProject(), resource, module, mainClass, searchScope);
    });
    if (!BeansEndpointTabSettings.getInstance(getProject()).isShowLibraryBeans()) {
      map = map.filter(item -> {
        VirtualFile virtualFile = getContainingFile(item);
        if (virtualFile == null) {
          return false;
        }
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        return !fileIndex.isInLibrary(virtualFile);
      });
    }
    return map.sorted(Comparator.comparing(item2 -> item2.getResource().getDisplayName(), NaturalComparator.INSTANCE))
            .collect(Collectors.toList());
  }

  public String getItemText(LiveResourceItem item) {
    return item.getResource().getDisplayName();
  }

  public boolean hasChildren(LiveResourceItem item) {
    return true;
  }

  @Nullable
  public Icon getItemIcon(LiveResourceItem item) {
    Navigatable navigatable = item.getNavigatable();
    if (navigatable instanceof PsiFile psiFile) {
      return psiFile.getIcon(0);
    }
    Icon icon = item.getResource().getIcon();
    return icon == null ? Icons.SpringJavaConfig : icon;
  }

  @Nullable
  public JComponent createRightComponent(LiveResourceItem item) {
    InfraRunUsageTriggerCollector.logRuntimeResourceSelected(getProject());
    return new LiveBeansPanel(getProject(), this, getGroupId(), getRunConfiguration(), getProcessHandler());
  }

  @Nullable
  public String getItemTooltipText(LiveResourceItem item) {
    if (item.getResource().hasDescription()) {
      return item.getResource().getDescription();
    }
    return item.getResource().getDisplayName();
  }

  @Nullable
  public VirtualFile getContainingFile(LiveResourceItem item) {
    Navigatable navigatable = item.getNavigatable();
    if (!(navigatable instanceof PsiElement psiElement)) {
      return null;
    }
    if (psiElement.isValid() && psiElement.getContainingFile() != null) {
      return psiElement.getContainingFile().getVirtualFile();
    }
    return null;
  }

  @Nullable
  public Object getData(String dataId) {
    LiveResourceItem selectedValue = getSelectedValue();
    if (selectedValue != null) {
      Navigatable navigatable = selectedValue.getNavigatable();
      if (CommonDataKeys.NAVIGATABLE.is(dataId) || (CommonDataKeys.PSI_ELEMENT.is(dataId) && (navigatable instanceof PsiElement))) {
        return navigatable;
      }
      if (CommonDataKeys.PSI_FILE.is(dataId) && (navigatable instanceof PsiElement psiElement)) {
        if (psiElement.isValid()) {
          return psiElement.getContainingFile();
        }
      }
    }
    return super.getData(dataId);
  }

  @Override
  public boolean doUpdateItem(LiveResourceItem item) {
    InfraApplicationRunConfig runConfiguration = getRunConfiguration();
    Module module = runConfiguration.getModule();
    PsiClass mainClass = runConfiguration.getMainClass();
    GlobalSearchScope searchScope = getRunConfiguration().getSearchScope();
    return item.updateItem(module, mainClass, searchScope);
  }

  @Override
  public String getEditActionHintMessage(LiveResourceItem item) {
    return message("infra.application.endpoints.config.file.not.found");
  }

  @Override
  protected boolean performEditAction() {
    if (getSelectedValue() != null) {
      InfraRunUsageTriggerCollector.logEditRuntimeResource(getProject(), "ToolwindowContent");
    }
    return super.performEditAction();
  }

  static class LiveResourceItem {

    private final Project myProject;

    private final LiveResource myResource;
    @Nullable
    private Navigatable myNavigatable;

    LiveResourceItem(Project project, LiveResource resource, @Nullable Module module, @Nullable PsiClass mainClass, GlobalSearchScope searchScope) {
      this.myProject = project;
      this.myResource = resource;
      updateItem(module, mainClass, searchScope);
    }

    public LiveResource getResource() {
      return this.myResource;
    }

    @Nullable
    public Navigatable getNavigatable() {
      if (DumbService.isDumb(this.myProject)) {
        return null;
      }
      return this.myNavigatable;
    }

    public boolean updateItem(@Nullable Module module, @Nullable PsiClass mainClass, GlobalSearchScope searchScope) {
      Navigatable navigatable = this.myResource.findResourceNavigatable(this.myProject, module, mainClass, searchScope);
      boolean changed = !Comparing.equal(this.myNavigatable, navigatable);
      this.myNavigatable = navigatable;
      return changed;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof LiveResourceItem) {
        return this.myResource.equals(((LiveResourceItem) obj).myResource);
      }
      return false;
    }

    public int hashCode() {
      return this.myResource.hashCode();
    }
  }
}
