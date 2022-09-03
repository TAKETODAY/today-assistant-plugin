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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.navigation.BackgroundUpdaterTask;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.NotNullFunction;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.application.config.InfraConfigurationPropertyReferenceSearcher;
import cn.taketoday.assistant.gutter.GutterIconBuilder.CustomNavigationHandlerBuilder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.message;

public class ConfigurationPropertyRelatedItemLineMarkerProvider extends RelatedItemLineMarkerProvider {

  private static final NotNullFunction<PsiReference, Collection<? extends GotoRelatedItem>> RELATED_ITEM_CONVERTER = reference -> {
    NavigatablePsiElement psiElement = getNavigationElement(reference);
    return Collections.singleton(new GotoRelatedItem(psiElement, message("configuration.properties.related.config.file.line.marker.popup.title")) {
      @Nullable
      public String getCustomContainerName() {
        ItemPresentation presentation = psiElement.getPresentation();
        if (presentation != null) {
          return presentation.getLocationString();
        }
        PsiFile file = psiElement.getContainingFile();
        if (file == null) {
          return null;
        }
        return SymbolPresentationUtil.getFilePathPresentation(file);
      }

      @Nullable
      public Icon getCustomIcon() {
        PsiFile file = psiElement.getContainingFile();
        if (file != null) {
          return psiElement.getContainingFile().getIcon(0);
        }
        return null;
      }
    });
  };

  private static final Processor<PsiReference> HAS_META_CONFIG_KEY_PROCESSOR = reference -> {
    return !(reference instanceof MetaConfigKeyReference);
  };

  public String getId() {
    return "ConfigurationPropertyRelatedItemLineMarkerProvider";
  }

  public String getName() {
    return message("configuration.properties.gutter.icon.name");
  }

  @Nullable
  public Icon getIcon() {
    return Icons.Today;
  }

  public void collectNavigationMarkers(List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement == null
            || !InfraUtils.hasFacets(psiElement.getProject())
            || !InfraLibraryUtil.hasFrameworkLibrary(psiElement.getProject())) {
      return;
    }
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  public static NavigatablePsiElement getNavigationElement(PsiReference reference) {
    if (reference instanceof MetaConfigKeyReference) {
      return ((MetaConfigKeyReference) reference).createNavigationElement();
    }
    PsiElement element = reference.getElement();
    if (element instanceof NavigatablePsiElement psi) {
      return psi;
    }
    return new FakePsiElement() {

      public PsiElement getParent() {
        return element;
      }

      public PsiElement getNavigationElement() {
        return element;
      }

      public ItemPresentation getPresentation() {
        PsiFile file = element.getContainingFile();
        return new PresentationData(SymbolPresentationUtil.getSymbolPresentableText(element),
                file == null
                ? null
                : "(" + SymbolPresentationUtil.getFilePathPresentation(element.getContainingFile()) + ")",
                file == null ? EmptyIcon.ICON_16 : file.getIcon(0), null);
      }
    };
  }

  protected void collectNavigationMarkers(PsiElement element, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiMethod psiMethod;
    String prefix;
    PsiIdentifier nameIdentifier;
    if (!(element instanceof PsiIdentifier)) {
      return;
    }
    PsiElement parent = element.getParent();
    if (!(parent instanceof PsiMethod method)
            || (prefix = InfraConfigurationPropertyReferenceSearcher.getPrefixIfRelevantPropertyMethod((psiMethod = method), true)) == null) {
      return;
    }
    GlobalSearchScope fakeScope = GlobalSearchScope.EMPTY_SCOPE;
    Query<PsiReference> search = MethodReferencesSearch.search(psiMethod, fakeScope, true);
    if (search.forEach(HAS_META_CONFIG_KEY_PROCESSOR) || (nameIdentifier = psiMethod.getNameIdentifier()) == null) {
      return;
    }
    NavigatablePsiElement firstTarget = getNavigationElement(search.findFirst());
    GutterIconNavigationHandler<PsiElement> navHandler = new GutterIconNavigationHandler<>() {
      @Override
      public void navigate(MouseEvent e, PsiElement elt) {
        if (!psiMethod.isValid()) {
          return;
        }
        String title = message("configuration.properties.related.config.file.task.base.title", prefix, getPropertyName(psiMethod));
        BackgroundUpdaterTask updaterTask = new BackgroundUpdaterTask(element.getProject(), message("configuration.properties.related.config.file.task.title", title), null) {

          public String getCaption(int size) {
            String message;
            if (isFinished()) {
              message = message("configuration.properties.related.config.file.task.details.finished", size);
            }
            else {
              message = message("configuration.properties.related.config.file.task.details", size);
            }
            String details = message;
            return message("configuration.properties.related.config.file.task.caption", title, details);
          }

          public void run(ProgressIndicator indicator) {
            super.run(indicator);
            search.forEach((reference) -> {
              ProgressManager.checkCanceled();
              PsiElement navigationElement = ConfigurationPropertyRelatedItemLineMarkerProvider.getNavigationElement(reference);
              if (ReadAction.compute(() -> !this.updateComponent(navigationElement))) {
                indicator.cancel();
              }

            });
          }

          public void onSuccess() {
            super.onSuccess();
            PsiElement theOnlyOneElement = getTheOnlyOneElement();
            if (theOnlyOneElement instanceof NavigatablePsiElement navigatablePsiElement) {
              navigatablePsiElement.navigate(true);
              this.myPopup.cancel();
            }
          }
        };
        DefaultPsiElementCellRenderer renderer = new DefaultPsiElementCellRenderer() {
          protected Icon getIcon(PsiElement element2) {
            PsiFile file = element2.getContainingFile();
            if (file != null) {
              return element2.getContainingFile().getIcon(0);
            }
            return super.getIcon(element2);
          }

          public String getContainerText(PsiElement element2, String name) {
            if (element2 instanceof NavigationItem) {
              ItemPresentation presentation = ((NavigationItem) element2).getPresentation();
              if (presentation != null) {
                return presentation.getLocationString();
              }
              PsiFile file = element2.getContainingFile();
              if (file != null) {
                return "(" + SymbolPresentationUtil.getFilePathPresentation(file) + ")";
              }
              return null;
            }
            return null;
          }
        };
        PsiElementListNavigator.openTargets(e, new NavigatablePsiElement[] { firstTarget }, updaterTask.getCaption(1), title, renderer, updaterTask);
      }
    };
    var createBuilder = CustomNavigationHandlerBuilder.createBuilder(Icons.Today,
            message("configuration.properties.related.config.file.line.marker.tooltip"), navHandler, RELATED_ITEM_CONVERTER);
    createBuilder.setTargets(NotNullLazyValue.lazy(search::findAll));
    result.add(createBuilder.createRelatedMergeableLineMarkerInfo(nameIdentifier));
  }

  private static String getPropertyName(PsiMethod psiMethod) {
    if (psiMethod.isConstructor()) {
      PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
      if (parameters.length == 1) {
        return parameters[0].getName();
      }
      return "*";
    }
    return PropertyUtilBase.getPropertyName(psiMethod, true);
  }
}
