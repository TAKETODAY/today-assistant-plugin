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

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.openapi.util.text.NaturalComparator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.util.SmartList;
import com.intellij.xml.util.PsiElementPointer;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.assistant.app.run.statistics.InfraRunUsageTriggerCollector;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.messagePointer;

public class LiveBeansPanel extends LifecycleFinderRecursivePanel<LiveBeansPanel.LiveBeanItem> {
  private static final String LIVE_BEANS_PANEL_GROUP_ID = "LiveBeansPanel";
  private final NullableFactory<CommonInfraModel> myModelFactory;
  private final NavigateToBeanClassAction myNavigateAction;

  public LiveBeansPanel(Project project, InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    this(project, null, LIVE_BEANS_PANEL_GROUP_ID, runConfiguration, processHandler);
  }

  public LiveBeansPanel(Project project, @Nullable FinderRecursivePanel parent, @Nullable String groupId, InfraApplicationRunConfig runConfiguration,
          ProcessHandler processHandler) {
    super(project, parent, groupId, runConfiguration, processHandler);
    this.myModelFactory = () -> {
      Module module = getRunConfiguration().getModule();
      if (module == null) {
        return null;
      }
      return InfraManager.from(getProject()).getCombinedModel(module);
    };
    this.myNavigateAction = new NavigateToBeanClassAction();
  }

  protected List<LiveBeanItem> getListItems() {
    List<LiveResource> resources;
    PsiClass beanClass;
    VirtualFile resourceFile;
    VirtualFile resourceFile2;
    FinderRecursivePanel parent = getParentPanel();
    if (parent != null) {
      if (parent.getSelectedValue() instanceof LiveContext) {
        resources = ((LiveContext) parent.getSelectedValue()).getResources();
      }
      else if (parent.getSelectedValue() instanceof LiveResourcesPanel.LiveResourceItem) {
        resources = new SmartList<>(((LiveResourcesPanel.LiveResourceItem) parent.getSelectedValue()).getResource());
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
    CommonInfraModel infraModel = this.myModelFactory.create();
    InfraApplicationRunConfig runConfiguration = getRunConfiguration();
    Module module = runConfiguration.getModule();
    PsiElement mainClass = runConfiguration.getMainClass();
    GlobalSearchScope searchScope = runConfiguration.getSearchScope();
    boolean showLibraryBeans = BeansEndpointTabSettings.getInstance(getProject()).isShowLibraryBeans();
    List<LiveBeanItem> items = new ArrayList<>();
    for (LiveResource resource : resources) {
      PsiElement resourceElement = resource.findResourceElement(getProject(), module, mainClass, searchScope);
      if (!showLibraryBeans && resourceElement != null && resourceElement.isValid() && resourceElement.getContainingFile() != null && (resourceFile2 = resourceElement.getContainingFile()
              .getVirtualFile()) != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        if (fileIndex.isInLibrary(resourceFile2)) {
        }
      }
      for (LiveBean liveBean : resource.getBeans()) {
        LiveBeanItem item = new LiveBeanItem(getProject(), liveBean);
        item.updateBeanClass(searchScope);
        if (!showLibraryBeans && !resource.hasDescription() && (beanClass = item.getBeanClass()) != null && (resourceFile = beanClass.getContainingFile().getVirtualFile()) != null) {
          ProjectFileIndex fileIndex2 = ProjectRootManager.getInstance(getProject()).getFileIndex();
          if (fileIndex2.isInLibrary(resourceFile)) {
          }
        }
        item.updateBeanPointer(resourceElement, infraModel);
        items.add(item);
      }
    }
    items.sort(Comparator.comparing(item2 -> item2.getBean().getName(), NaturalComparator.INSTANCE));
    return items;
  }

  public String getItemText(LiveBeanItem item) {
    return item.getBean().getName();
  }

  public boolean hasChildren(LiveBeanItem item) {
    return false;
  }

  @Nullable
  public VirtualFile getContainingFile(LiveBeanItem item) {
    PsiElement beanElement = item.getPointer().getPsiElement();
    if (beanElement != null && beanElement.isValid() && beanElement.getContainingFile() != null) {
      return beanElement.getContainingFile().getVirtualFile();
    }
    return null;
  }

  @Nullable
  public Object getData(String dataId) {
    PsiElement element;
    LiveBeanItem selectedValue = getSelectedValue();
    if (selectedValue != null) {
      if (CommonDataKeys.NAVIGATABLE.is(dataId) || CommonDataKeys.PSI_ELEMENT.is(dataId)) {
        return selectedValue.getPointer().getPsiElement();
      }
      if (CommonDataKeys.PSI_FILE.is(dataId) && (element = selectedValue.getPointer().getPsiElement()) != null) {
        return element.getContainingFile();
      }
    }
    Object data = super.getData(dataId);
    if (data != null) {
      return data;
    }
    for (LiveBeansPanelContent content : LiveBeansPanelContent.EP_NAME.getExtensions()) {
      Object contentData = content.getData(this, dataId);
      if (contentData != null) {
        return contentData;
      }
    }
    return null;
  }

  @Nullable
  public Icon getItemIcon(LiveBeanItem item) {
    PsiElementPointer pointer = item.getPointer();
    return pointer instanceof BeanPointer ? InfraPresentationProvider.getInfraIcon(pointer) : item.getBean().getIcon();
  }

  @Nullable
  protected JComponent createDefaultRightComponent() {
    BeansEndpointTabSettings settings = BeansEndpointTabSettings.getInstance(getProject());
    if (!settings.isShowDoc() && (!settings.isShowLiveBeansGraph() || LiveBeansPanelContent.EP_NAME.getExtensions().length == 0)) {
      return null;
    }
    return super.createDefaultRightComponent();
  }

  @Nullable
  public JComponent createRightComponent(LiveBeanItem item) {
    InfraRunUsageTriggerCollector.logRuntimeBeanSelected(getProject());
    BeansEndpointTabSettings settings = BeansEndpointTabSettings.getInstance(getProject());
    LiveBeansPanelContent[] contentExtensions = LiveBeansPanelContent.EP_NAME.getExtensions();
    if (!settings.isShowDoc() && (!settings.isShowLiveBeansGraph() || contentExtensions.length == 0)) {
      return null;
    }
    DisposablePanel panel = new DisposablePanel(new BorderLayout(), this);
    OnePixelSplitter splitter = new OnePixelSplitter(true);
    splitter.setProportion(0.5f);
    panel.add(splitter, "Center");
    if (settings.isShowDoc()) {
      PsiElement element = item.getPointer().getPsiElement();
      if ((item.getPointer() instanceof LiveBean.LiveResourcePointer) || element == null) {
        splitter.setFirstComponent(new JBPanelWithEmptyText().withEmptyText(CodeInsightBundle.message("no.documentation.found")));
      }
      else {
        JComponent documentationComponent = DocumentationComponent.createAndFetch(getProject(), element, this);
        splitter.setFirstComponent(documentationComponent);
      }
    }
    if (settings.isShowLiveBeansGraph()) {
      for (LiveBeansPanelContent content : contentExtensions) {
        SmartList smartList = new SmartList(item.getBean());
        splitter.setSecondComponent(content.createComponent(getProject(), this, panel, () -> smartList, getRunConfiguration(), true));
      }
    }
    return panel;
  }

  public void setSecondComponent(@Nullable JComponent component) {
    super.setSecondComponent(component);
    if (!BeansEndpointTabSettings.getInstance(getProject()).isShowLiveBeansGraph()) {
      return;
    }
    for (LiveBeansPanelContent content : LiveBeansPanelContent.EP_NAME.getExtensions()) {
      content.update(this);
    }
  }

  protected AnAction[] getCustomListActions() {
    return new AnAction[] { this.myNavigateAction };
  }

  @Override
  protected JBList<LiveBeanItem> createList() {
    JBList<LiveBeanItem> createList = super.createList();
    this.myNavigateAction.registerCustomShortcutSet(ActionManager.getInstance().getAction("EditSource").getShortcutSet(), createList);
    return createList;
  }

  @Nullable
  public String getItemTooltipText(LiveBeanItem item) {
    return item.getBean().getClassName();
  }

  @Override
  public boolean doUpdateItem(LiveBeanItem item) {
    LiveBean bean = item.getBean();
    InfraApplicationRunConfig runConfiguration = getRunConfiguration();
    GlobalSearchScope searchScope = runConfiguration.getSearchScope();
    PsiElement resourceElement = null;
    if (bean.getResource() != null) {
      resourceElement = bean.getResource().findResourceElement(getProject(), runConfiguration.getModule(), runConfiguration.getMainClass(), searchScope);
    }
    return item.updateItem(resourceElement, this.myModelFactory.create(), searchScope);
  }

  @Override
  public String getEditActionHintMessage(LiveBeanItem item) {
    return InfraRunBundle.message("infra.application.endpoints.bean.definition.not.found");
  }

  @Override
  protected boolean performEditAction() {
    if (getSelectedValue() != null) {
      InfraRunUsageTriggerCollector.logEditRuntimeBean(getProject(), "ToolwindowContent");
    }
    return super.performEditAction();
  }

  static class LiveBeanItem {

    private final Project myProject;

    private final LiveBean myBean;

    private PsiElementPointer myPointer;
    @Nullable
    private PsiClass myBeanClass;

    LiveBeanItem(Project project, LiveBean bean) {
      this.myPointer = () -> null;
      this.myProject = project;
      this.myBean = bean;
    }

    LiveBean getBean() {
      return this.myBean;
    }

    PsiElementPointer getPointer() {
      if (DumbService.isDumb(this.myProject) || ((this.myPointer instanceof BeanPointer beanPointer) && !beanPointer.isValid())) {
        return () -> {
          return null;
        };
      }
      return this.myPointer;
    }

    @Nullable
    PsiClass getBeanClass() {
      if (DumbService.isDumb(this.myProject)) {
        return null;
      }
      return this.myBeanClass;
    }

    boolean updateItem(PsiElement resourceElement, CommonInfraModel infraModel, GlobalSearchScope searchScope) {
      boolean changed = updateBeanClass(searchScope);
      return updateBeanPointer(resourceElement, infraModel) || changed;
    }

    boolean updateBeanClass(GlobalSearchScope searchScope) {
      PsiClass beanClass = this.myBean.findBeanClass(this.myProject, searchScope);
      boolean changed = !Comparing.equal(beanClass, this.myBeanClass);
      this.myBeanClass = beanClass;
      return changed;
    }

    boolean updateBeanPointer(@Nullable PsiElement resourceElement, @Nullable CommonInfraModel infraModel) {
      boolean changed;
      if (resourceElement != null && !resourceElement.isValid()) {
        resourceElement = null;
      }
      PsiElementPointer pointer = this.myBean.findBeanPointer(this.myBeanClass, resourceElement, infraModel);
      if (this.myPointer instanceof BeanPointer) {
        changed = !this.myPointer.equals(pointer);
      }
      else if (pointer instanceof BeanPointer) {
        changed = !pointer.equals(this.myPointer);
      }
      else {
        changed = !Comparing.equal(this.myPointer.getPsiElement(), pointer.getPsiElement());
      }
      this.myPointer = pointer;
      return changed;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof LiveBeanItem) {
        return this.myBean.equals(((LiveBeanItem) obj).myBean);
      }
      return false;
    }

    public int hashCode() {
      return this.myBean.hashCode();
    }
  }

  private class NavigateToBeanClassAction extends AnAction {

    NavigateToBeanClassAction() {
      super(messagePointer("infra.application.endpoints.navigate.to.bean.class.action.name"),
              messagePointer("infra.application.endpoints.navigate.to.bean.class.action.description"), AllIcons.Nodes.Class);
    }

    public void update(AnActionEvent e) {
      LiveBeanItem value = LiveBeansPanel.this.getSelectedValue();
      e.getPresentation().setEnabled(value != null && value.getBeanClass() != null);
    }

    public void actionPerformed(AnActionEvent e) {
      LiveBeanItem value = LiveBeansPanel.this.getSelectedValue();
      if (value == null) {
        return;
      }
      LiveBeansPanel.this.updateItem(value);
      PsiClass beanClass = value.getBeanClass();
      if (beanClass != null && beanClass.canNavigate()) {
        beanClass.navigate(true);
      }
      else {
        LiveBeansPanel.this.showHint(InfraRunBundle.message("infra.application.endpoints.bean.class.not.found"));
      }
    }
  }
}
