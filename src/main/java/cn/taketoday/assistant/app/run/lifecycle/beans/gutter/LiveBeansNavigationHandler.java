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

package cn.taketoday.assistant.app.run.lifecycle.beans.gutter;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.ListPopupStepEx;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.SeparatorWithText;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.OpaquePanel;
import com.intellij.ui.popup.WizardPopup;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.NotNullFunction;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.PsiElementPointer;

import org.jetbrains.annotations.Nls;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.InfraRunIcons;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.beans.BeansEndpoint;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.statistics.InfraRunUsageTriggerCollector;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

final class LiveBeansNavigationHandler implements GutterIconNavigationHandler<PsiElement> {

  private static final NotNullFunction<BeanNavigationItem, Collection<? extends GotoRelatedItem>> GOTO_RELATED_ITEMS_PROVIDER = item -> {
    SmartList smartList = new SmartList();
    String injectedIntoGroupId = message("infra.application.endpoints.bean.item.group.dependent");
    for (LiveBean liveBean : item.bean.getInjectedInto()) {
      PsiElement psiElement = findBeanPointer(item, liveBean).getPsiElement();
      if (psiElement != null) {
        smartList.add(new GotoRelatedItem(psiElement, injectedIntoGroupId));
      }
    }
    String dependenciesGroupId = message("infra.application.endpoints.bean.item.group.injected");
    for (LiveBean liveBean2 : item.bean.getDependencies()) {
      PsiElement psiElement2 = findBeanPointer(item, liveBean2).getPsiElement();
      if (psiElement2 != null) {
        smartList.add(new GotoRelatedItem(psiElement2, dependenciesGroupId));
      }
    }
    return smartList;
  };
  private final List<? extends BeanNavigationItem> myItems;

  private LiveBeansNavigationHandler(List<? extends BeanNavigationItem> items) {
    this.myItems = items;
  }

  private String getLiveMarkerInfoTooltipText() {
    if (this.myItems.size() == 1) {
      return this.myItems.get(0).getDisplayText();
    }
    return message("infra.application.endpoints.bean.popup.title");
  }

  public void navigate(MouseEvent e, PsiElement element) {
    if (this.myItems.isEmpty()) {
      return;
    }
    if (this.myItems.size() == 1) {
      BeanNavigationItem item = this.myItems.get(0);
      if (item.bean.getInjectedInto().size() + item.bean.getDependencies().size() == 0) {
        JComponent label = HintUtil.createInformationLabel(message("infra.application.endpoints.bean.not.injected"));
        label.setBorder(JBUI.Borders.empty(2, 7));
        JBPopupFactory.getInstance().createBalloonBuilder(label).setFadeoutTime(3000L).setFillColor(HintUtil.getInformationColor()).createBalloon().show(new RelativePoint(e), Balloon.Position.above);
        return;
      }
      showPopup(e, createDependenciesPopup(item.project, null, item), item.project);
      return;
    }
    Project project = this.myItems.get(0).project;
    showPopup(e, createBeansPopup(project, this.myItems), project);
  }

  private static void showPopup(MouseEvent e, ListPopup popup, Project project) {
    NavigationUtil.hidePopupIfDumbModeStarts(popup, project);
    popup.show(new RelativePoint(e));
  }

  private static ListPopup createBeansPopup(Project project, List<? extends BeanNavigationItem> items) {
    return new ListPopupImpl(project, new BeansPopupStep(items)) {
      protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
        if (!(step instanceof ListPopupStep)) {
          throw new IllegalArgumentException("Step: " + step.getClass().toString());
        }
        if (!(parentValue instanceof BeanNavigationItem)) {
          throw new IllegalArgumentException("Parent value: " + step.getClass().toString());
        }
        return createDependenciesPopup(getProject(), parent, (BeanNavigationItem) parentValue);
      }
    };
  }

  private static ListPopupImpl createDependenciesPopup(Project project, WizardPopup parent, BeanNavigationItem item) {
    Trinity<List<DependencyNavigationItem>, DependencyNavigationItem, DependencyNavigationItem> relatedItems = getRelatedBeansWithSeparators(item);
    DependenciesPopupStep step = new DependenciesPopupStep(item, relatedItems.first, relatedItems.second, relatedItems.third);
    return new ListPopupImpl(project, parent, step, item) {
      protected ListCellRenderer getListElementRenderer() {
        return new DependencyPopupRenderer(step);
      }
    };
  }

  private static Trinity<List<DependencyNavigationItem>, DependencyNavigationItem, DependencyNavigationItem> getRelatedBeansWithSeparators(BeanNavigationItem item) {
    List<DependencyNavigationItem> injected = ContainerUtil.map(item.bean.getInjectedInto(), dependency -> {
      return new DependencyNavigationItem(item, dependency);
    });
    List<DependencyNavigationItem> dependencies = ContainerUtil.map(item.bean.getDependencies(), dependency2 -> {
      return new DependencyNavigationItem(item, dependency2);
    });
    return Trinity.create(ContainerUtil.concat(injected, dependencies), ContainerUtil.getFirstItem(injected),
            ContainerUtil.getFirstItem(dependencies));
  }

  private static void navigateToBean(BeanNavigationItem item, LiveBean liveBean) {
    PsiElementPointer pointer = findBeanPointer(item, liveBean);
    if (pointer.getPsiElement() instanceof NavigatablePsiElement navigatablePsiElement) {
      navigatablePsiElement.navigate(true);
      InfraRunUsageTriggerCollector.logRuntimeBeansNavigationHandler(item.project, "ICON_NAVIGATION");
    }
  }

  public static PsiElementPointer findBeanPointer(BeanNavigationItem item, LiveBean liveBean) {
    if (!(item.info.getRunProfile() instanceof InfraApplicationRunConfigurationBase runConfiguration)) {
      return () -> null;
    }
    InfraModel combinedModel = InfraManager.from(item.project).getCombinedModel(runConfiguration.getModule());
    GlobalSearchScope searchScope = runConfiguration.getSearchScope();
    PsiElement resourceElement = liveBean.getResource() == null ? null : liveBean.getResource()
            .findResourceElement(item.project, runConfiguration.getModule(), runConfiguration.getMainClass(), searchScope);
    PsiClass beanClass = liveBean.findBeanClass(item.project, searchScope);
    return liveBean.findBeanPointer(beanClass, resourceElement, combinedModel);
  }

  private static class BeansPopupStep extends BaseListPopupStep<BeanNavigationItem> {

    BeansPopupStep(List<? extends BeanNavigationItem> items) {
      super(message("infra.application.endpoints.bean.popup.title"), items);
    }

    public String getTextFor(BeanNavigationItem item) {
      return item.getDisplayText();
    }

    public boolean isSpeedSearchEnabled() {
      return true;
    }

    public boolean hasSubstep(BeanNavigationItem item) {
      return true;
    }

    public Icon getIconFor(BeanNavigationItem item) {
      return item.bean.getIcon();
    }

    public PopupStep onChosen(BeanNavigationItem item, boolean finalChoice) {
      Trinity<List<DependencyNavigationItem>, DependencyNavigationItem, DependencyNavigationItem> relatedItems = getRelatedBeansWithSeparators(item);
      return new DependenciesPopupStep(item, relatedItems.first, relatedItems.second, relatedItems.third);
    }
  }

  private static class DependenciesPopupStep extends BaseListPopupStep<DependencyNavigationItem> implements ListPopupStepEx<DependencyNavigationItem> {
    private final BeanNavigationItem myItem;
    private final DependencyNavigationItem myInjectedSeparator;
    private final DependencyNavigationItem myDependencySeparator;

    DependenciesPopupStep(BeanNavigationItem item, List<DependencyNavigationItem> items, DependencyNavigationItem injectedSeparator, DependencyNavigationItem dependencySeparator) {
      super(item.getDisplayText(), items);
      this.myItem = item;
      this.myInjectedSeparator = injectedSeparator;
      this.myDependencySeparator = dependencySeparator;
    }

    public boolean isSpeedSearchEnabled() {
      return true;
    }

    public PopupStep onChosen(DependencyNavigationItem item, boolean finalChoice) {
      navigateToBean(this.myItem, item.bean);
      return PopupStep.FINAL_CHOICE;
    }

    @Nullable
    public String getTooltipTextFor(DependencyNavigationItem item) {
      return null;
    }

    public void setEmptyText(StatusText emptyText) {
      emptyText.setText(message("infra.application.endpoints.bean.not.injected"));
    }

    @Nullable
    public ListSeparator getSeparatorAbove(DependencyNavigationItem value) {
      if (this.myInjectedSeparator != null && value.bean.equals(this.myInjectedSeparator.bean)) {
        return new ListSeparator(message("infra.application.endpoints.bean.injected.into"));
      }
      if (this.myDependencySeparator != null && value.bean.equals(this.myDependencySeparator.bean)) {
        return new ListSeparator(message("infra.application.endpoints.bean.depends.on"));
      }
      return super.getSeparatorAbove(value);
    }
  }

  private static class DependencyPopupRenderer implements ListCellRenderer<DependencyNavigationItem> {
    private final ListPopupStep<DependencyNavigationItem> myPopupStep;
    private final JPanel myRendererPanel;
    private final SeparatorWithText mySeparatorComponent;
    private final SimpleListCellRenderer<DependencyNavigationItem> myNotFoundBeanRenderer = SimpleListCellRenderer.create((label, value, index) -> {
      label.setIcon(value.bean.getIcon());
      label.setText(value.bean.getName());
      label.setEnabled(false);
    });
    private final BeanPointerCellRenderer myElementCellRenderer = new BeanPointerCellRenderer();

    DependencyPopupRenderer(ListPopupStep<DependencyNavigationItem> popupStep) {
      this.myPopupStep = popupStep;
      OpaquePanel opaquePanel = new OpaquePanel(new BorderLayout(), UIUtil.getListBackground());
      opaquePanel.add(this.myNotFoundBeanRenderer, "North");
      opaquePanel.add(this.myElementCellRenderer, "Center");
      this.mySeparatorComponent = new SeparatorWithText();
      this.myRendererPanel = new OpaquePanel(new BorderLayout(), UIUtil.getListBackground());
      this.myRendererPanel.add(this.mySeparatorComponent, "North");
      this.myRendererPanel.add(opaquePanel, "Center");
    }

    public Component getListCellRendererComponent(JList<? extends DependencyNavigationItem> list, DependencyNavigationItem value, int index, boolean isSelected, boolean cellHasFocus) {
      ListSeparator separator = this.myPopupStep.getSeparatorAbove(value);
      this.mySeparatorComponent.setVisible(separator != null);
      if (separator != null) {
        this.mySeparatorComponent.setCaption(separator.getText());
      }
      PsiElementPointer beanPointer = value.pointer.getValue();
      boolean found = beanPointer.getPsiElement() != null;
      this.myElementCellRenderer.setVisible(found);
      this.myNotFoundBeanRenderer.setVisible(!found);
      if (found) {
        this.myElementCellRenderer.setBean(value.bean);
        Component component = this.myElementCellRenderer.getListCellRendererComponent(list, beanPointer.getPsiElement(), index, isSelected, cellHasFocus);
        if (component != this.myElementCellRenderer) {
          this.myElementCellRenderer.removeAll();
          this.myElementCellRenderer.add(component, "Center");
        }
      }
      else {
        this.myNotFoundBeanRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }
      return this.myRendererPanel;
    }
  }

  private static class BeanPointerCellRenderer extends DefaultPsiElementCellRenderer {
    LiveBean myBean;

    private BeanPointerCellRenderer() {
    }

    void setBean(LiveBean bean) {
      this.myBean = bean;
    }

    public String getElementText(PsiElement element) {
      if (this.myBean != null) {
        return this.myBean.getName();
      }
      return super.getElementText(element);
    }

    protected Icon getIcon(PsiElement element) {
      if (this.myBean != null) {
        return this.myBean.getIcon();
      }
      return super.getIcon(element);
    }
  }

  private static class DependencyNavigationItem {
    final LiveBean bean;
    final NotNullLazyValue<PsiElementPointer> pointer;

    DependencyNavigationItem(BeanNavigationItem item, LiveBean bean) {
      this.bean = bean;
      this.pointer = NotNullLazyValue.lazy(() -> {
        return findBeanPointer(item, bean);
      });
    }
  }

  private static class BeanNavigationItem {
    final Project project;
    final InfraApplicationInfo info;
    final LiveBean bean;

    BeanNavigationItem(Project project, InfraApplicationInfo info, LiveBean bean) {
      this.project = project;
      this.info = info;
      this.bean = bean;
    }

    @Nls
    String getDisplayText() {
      return this.bean.getName() + " [" + this.info.getRunProfile().getName() + "]";
    }
  }

  static boolean hasLiveBeansModels(Project project) {
    if ((!InfraUtils.hasFacets(project)
            && !InfraGeneralSettings.from(project).isAllowAutoConfigurationMode())
            || !InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return false;
    }
    Collection<InfraApplicationInfo> infos = InfraApplicationLifecycleManager.from(project).getInfraApplicationInfos();
    return infos.stream().anyMatch(info -> {
      return info.getEndpointData(BeansEndpoint.getInstance()).getValue() != null;
    });
  }

  static void addLiveBeansGutterIcon(String beanName, Predicate<? super LiveBean> beanMatcher,
          Project project, PsiElement nameIdentifier, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    SmartList<BeanNavigationItem> smartList = new SmartList<>();
    Collection<InfraApplicationInfo> infos = InfraApplicationLifecycleManager.from(project).getInfraApplicationInfos();
    for (InfraApplicationInfo info : infos) {
      LiveBeansModel beansModel = info.getEndpointData(BeansEndpoint.getInstance()).getValue();
      if (beansModel != null) {
        beansModel.getBeansByName(beanName).stream().filter(beanMatcher).forEach(liveBean -> {
          smartList.add(new BeanNavigationItem(project, info, liveBean));
        });
      }
    }
    if (smartList.isEmpty()) {
      return;
    }
    LiveBeansNavigationHandler navigationHandler = new LiveBeansNavigationHandler(smartList);
    var createBuilder = GutterIconBuilder.CustomNavigationHandlerBuilder.createBuilder(InfraRunIcons.Gutter.LiveBean,
            navigationHandler.getLiveMarkerInfoTooltipText(), navigationHandler, GOTO_RELATED_ITEMS_PROVIDER);
    createBuilder.setTargets(NotNullLazyValue.createConstantValue(smartList));
    result.add(createBuilder.createRelatedMergeableLineMarkerInfo(nameIdentifier));
  }
}
