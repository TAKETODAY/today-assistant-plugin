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

package cn.taketoday.assistant.web.mvc.navigation;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.navigation.BackgroundUpdaterTask;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.WebControllerClassInfo;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;

import static cn.taketoday.assistant.InfraAppBundle.message;

public final class InfraMvcRequestMappingRelatedItemLineMarkerProvider extends RelatedItemLineMarkerProvider {

  public String getId() {
    return "InfraMvcRequestMappingRelatedItemLineMarkerProvider";
  }

  public String getName() {
    return message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.related.views");
  }

  public Icon getIcon() {
    return Icons.RequestMapping;
  }

  public void collectNavigationMarkers(List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement == null || !InfraLibraryUtil.isWebMVCEnabled(psiElement.getProject())) {
      return;
    }
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  protected void collectNavigationMarkers(PsiElement context, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    NavigatablePsiElement containingFile;
    UElement uParentForIdentifier = UastUtils.getUParentForIdentifier(context);
    if (!(uParentForIdentifier instanceof UMethod uMethod)) {
      return;
    }
    PsiMethod method = uMethod.getJavaPsi();
    if (!InfraControllerUtils.isRequestHandlerCandidate(method)) {
      return;
    }
    PsiClass psiClass = method.getContainingClass();
    if (!InfraUtils.isBeanCandidateClass(psiClass) || !InfraControllerUtils.isRequestHandler(method)) {
      return;
    }
    Set<String> views = WebControllerClassInfo.getViews(uMethod).keySet();
    if (views.isEmpty()) {
      return;
    }
    CommonProcessors.FindFirstProcessor<PsiElement> findFirstProcessor = new CommonProcessors.FindFirstProcessor<>();
    processViews(context, views, findFirstProcessor);
    if (!findFirstProcessor.isFound()) {
      return;
    }
    PsiElement psiElement = findFirstProcessor.getFoundValue();
    if (psiElement instanceof NavigatablePsiElement navigatablePsiElement) {
      containingFile = navigatablePsiElement;
    }
    else {
      containingFile = psiElement != null ? psiElement.getContainingFile() : null;
    }
    NavigatablePsiElement navigatablePsiElement2 = containingFile;
    if (navigatablePsiElement2 == null) {
      return;
    }
    GutterIconNavigationHandler<PsiElement> handler = new GutterIconNavigationHandler<PsiElement>() {
      public void navigate(MouseEvent e, PsiElement elt) {
        String methodName = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, 4097, 2, 0);
        String title = message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.request.mapping.title", methodName);
        BackgroundUpdaterTask updaterTask = new BackgroundUpdaterTask(context.getProject(),
                message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.request.mapping.task.title", methodName), null) {

          public String getCaption(int size) {
            if (isFinished()) {
              return message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.request.mapping.task.finished.caption", title, Integer.valueOf(size));
            }
            return message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.request.mapping.task.caption", title, Integer.valueOf(size));
          }

          public void run(ProgressIndicator indicator) {
            super.run(indicator);
            processViews(context, views, (file) -> {
              ProgressManager.checkCanceled();

              if (ReadAction.compute(() -> !updateComponent(file))) {
                indicator.cancel();
              }

              return true;
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
        PsiElementListNavigator.openTargets(e, new NavigatablePsiElement[] { navigatablePsiElement2 },
                updaterTask.getCaption(1), title, new DefaultPsiElementCellRenderer(), updaterTask);
      }
    };
    var builder = GutterIconBuilder.CustomNavigationHandlerBuilder.<PsiElement>createBuilder(Icons.Gutter.RequestMapping,
            message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.related.views"), handler, dom -> {
              return GotoRelatedItem.createItems(Collections.singleton(dom),
                      message("InfraMvcRequestMappingRelatedItemLineMarkerProvider.infra.view"));
            });
    builder.setTargets(NotNullLazyValue.lazy(() -> {
      var processor = new CommonProcessors.CollectProcessor<PsiElement>();
      processViews(context, views, processor);
      return processor.getResults();
    }));
    result.add(builder.withElementPresentation(message("request.mapping.gutter.views.name")).createRelatedMergeableLineMarkerInfo(context));
  }

  private static boolean processViews(PsiElement context, Set<String> views, Processor<PsiElement> processor) {
    return ReadAction.compute(() -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(context);
      if (module == null) {
        return true;
      }
      Set<ViewResolver> resolvers = WebMvcService.getInstance().getViewResolvers(module);
      for (String view : views) {
        for (ViewResolver resolver : resolvers) {
          Set<PsiElement> psiElements = ReadAction.compute(() -> resolver.resolveView(view));
          Iterator<PsiElement> it2 = psiElements.iterator();
          if (it2.hasNext()) {
            PsiElement element = it2.next();
            if (!processor.process(element)) {
              return false;
            }
          }
        }
      }
      return true;
    });
  }
}
