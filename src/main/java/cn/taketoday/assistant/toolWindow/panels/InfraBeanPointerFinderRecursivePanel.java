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

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.openapi.util.text.NaturalComparator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.containers.ContainerUtil;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComponent;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraImplicitBeanMarker;
import cn.taketoday.assistant.model.InfrastructureBean;
import cn.taketoday.assistant.profiles.ChangeActiveProfilesAction;
import cn.taketoday.assistant.toolWindow.InfraBeansViewSettings;
import cn.taketoday.assistant.toolWindow.WebBeanPointerPanelBase;
import cn.taketoday.lang.Nullable;

public final class InfraBeanPointerFinderRecursivePanel extends WebBeanPointerPanelBase {
  public static final ExtensionPointName<BeanPointerPanelContent> EP_NAME =
          new ExtensionPointName<>("cn.taketoday.assistant.beanPointerPanelContent");

  private Set<PsiFile> modelFiles;
  private final NullableFactory<CommonInfraModel> myModelFactory;

  public interface BeanPointerPanelContent {
    JComponent createComponent(FinderRecursivePanel finderRecursivePanel, Disposable disposable, NullableFactory<CommonInfraModel> nullableFactory, BeanPointer<?> beanPointer);

    void update(FinderRecursivePanel finderRecursivePanel);

    Object getData(FinderRecursivePanel finderRecursivePanel, String str);
  }

  public InfraBeanPointerFinderRecursivePanel(Project project, @Nullable String groupId, NullableFactory<CommonInfraModel> modelFactory) {
    super(project, groupId);
    this.modelFiles = Collections.emptySet();
    this.myModelFactory = modelFactory;
    installDependenciesListener();
  }

  public InfraBeanPointerFinderRecursivePanel(FinderRecursivePanel panel, NullableFactory<CommonInfraModel> modelFactory) {
    super(panel);
    this.modelFiles = Collections.emptySet();
    this.myModelFactory = modelFactory;
    installDependenciesListener();
  }

  private void installDependenciesListener() {
    new InfraBeanPointerDependenciesListener(this);
    getProject().getMessageBus().connect(this).subscribe(InfraFileSetService.TOPIC, new InfraFileSetService.InfraFileSetListener() {
      @Override
      public void activeProfilesChanged() {
        InfraBeanPointerFinderRecursivePanel.this.updatePanel();
      }
    });
  }

  public boolean knowsAboutConfigurationFile(PsiFile file) {
    return this.modelFiles.contains(file);
  }

  public boolean hasChildren(BeanPointer<?> pointer) {
    return false;
  }

  public List<BeanPointer<?>> getListItems() {
    CommonInfraModel infraModel = this.myModelFactory.create();
    if (infraModel == null) {
      return Collections.emptyList();
    }
    this.modelFiles = new HashSet<>(InfraModelVisitorUtils.getConfigFiles(infraModel));
    List<BeanPointer<?>> localBeans = getSortedAndFilteredItems(infraModel.getAllCommonBeans());
    for (BeanPointer bean : localBeans) {
      this.modelFiles.add(bean.getContainingFile());
    }
    return localBeans;
  }

  private List<BeanPointer<?>> getSortedAndFilteredItems(Collection<BeanPointer<?>> beans) {
    SortedSet<BeanPointer<?>> pointers = new TreeSet<>(Comparator.comparing(this::getItemText, NaturalComparator.INSTANCE));
    boolean showImplicitBeans = getSettings().isShowImplicitBeans();
    boolean showInfrastructureBeans = getSettings().isShowInfrastructureBeans();
    pointers.addAll(ContainerUtil.filter(beans, springBeanPointer -> {
      if (!showImplicitBeans && (springBeanPointer.getBean() instanceof InfraImplicitBeanMarker)) {
        return false;
      }
      return showInfrastructureBeans || (!(springBeanPointer.getBean() instanceof InfrastructureBean));
    }));
    return new ArrayList<>(pointers);
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    Object data = super.getData(dataId);
    if (data != null) {
      return data;
    }
    for (BeanPointerPanelContent content : EP_NAME.getExtensions()) {
      Object contentData = content.getData(this, dataId);
      if (contentData != null) {
        return contentData;
      }
    }
    return null;
  }

  protected AnAction[] getCustomListActions() {
    return new AnAction[] { ActionManager.getInstance().getAction(ChangeActiveProfilesAction.ACTION_ID) };
  }

  protected JComponent createDefaultRightComponent() {
    InfraBeansViewSettings settings = getSettings();
    if (!settings.isShowDoc() && (!settings.isShowGraph() || EP_NAME.getExtensions().length == 0)) {
      return null;
    }
    return super.createDefaultRightComponent();
  }

  @Nullable
  public JComponent createRightComponent(BeanPointer<?> pointer) {
    InfraBeansViewSettings settings = getSettings();
    BeanPointerPanelContent[] contentExtensions = EP_NAME.getExtensions();
    if (!settings.isShowDoc() && (!settings.isShowGraph() || contentExtensions.length == 0)) {
      return null;
    }
    DisposablePanel disposablePanel = new DisposablePanel(new BorderLayout(), this);
    if (!pointer.isValid() || isDisposed()) {
      return disposablePanel;
    }
    float storedProportion = settings.getBeanDetailsProportion();
    float proportion = storedProportion != -1.0f ? storedProportion : settings.isShowGraph() ? 0.3f : 1.0f;
    OnePixelSplitter splitter = new OnePixelSplitter(true, proportion, 0.1f, 0.9f) {
      protected void saveProportion() {
        settings.setBeanDetailsProportion(getProportion());
      }
    };
    disposablePanel.add(splitter, "Center");
    if (settings.isShowDoc()) {
      PsiElement element = pointer.getBean().getIdentifyingPsiElement();
      JComponent documentationComponent = DocumentationComponent.createAndFetch(getProject(), element, this);
      splitter.setFirstComponent(documentationComponent);
    }
    if (settings.isShowGraph()) {
      for (BeanPointerPanelContent content : contentExtensions) {
        JComponent component = content.createComponent(this, disposablePanel, this.myModelFactory, pointer);
        splitter.setSecondComponent(component);
      }
    }
    return disposablePanel;
  }

  public void setSecondComponent(@Nullable JComponent component) {
    super.setSecondComponent(component);
    if (!getSettings().isShowGraph()) {
      return;
    }
    for (BeanPointerPanelContent content : EP_NAME.getExtensions()) {
      content.update(this);
    }
  }

  private InfraBeansViewSettings getSettings() {
    return InfraBeansViewSettings.from(getProject());
  }
}
