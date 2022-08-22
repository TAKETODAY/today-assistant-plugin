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

package cn.taketoday.assistant.code.event;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemService;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.gutter.groups.SpringGutterIconBuilder;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.TodayLibraryUtil;
import cn.taketoday.assistant.code.event.beans.PublishEventPointDescriptor;
import cn.taketoday.assistant.code.event.jam.EventListenerElement;
import cn.taketoday.assistant.code.event.jam.EventModelUtils;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 21:00
 */
public class EventListenerAnnotator extends RelatedItemLineMarkerProvider {
  private static final NotNullFunction<PublishEventPointDescriptor, Collection<? extends PsiElement>> PUBLISH_EVENT_CONVERTOR = (descriptor) -> {
    return ContainerUtil.createMaybeSingletonList(descriptor.getNavigatableElement());
  };
  private static final NotNullFunction<EventListenerElement, Collection<? extends PsiElement>> EVENT_LISTENER_CONVERTOR = (descriptor) -> {
    return ContainerUtil.createMaybeSingletonList(descriptor.getPsiElement());
  };

  @Override
  public String getId() {
    return "EventListenerAnnotator";
  }

  @Override
  public String getName() {
    return InfraBundle.message("core.event.listener.annotator.name");
  }

  @Override
  public Icon getIcon() {
    return SpringApiIcons.Listener;
  }

  @Override
  public void collectNavigationMarkers(List<? extends PsiElement> elements,
          Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {

    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement != null
            && CommonUtils.hasFacets(psiElement.getProject())
            && TodayLibraryUtil.hasLibrary(psiElement.getProject())) {

      super.collectNavigationMarkers(elements, result, forNavigation);
    }
  }

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UElement uElement = UastContextKt.toUElementOfExpectedTypes(psiElement,
            UMethod.class, UCallExpression.class, UQualifiedReferenceExpression.class);

    if (uElement instanceof UMethod uMethod) {
      PsiMethod psiMethod = UElementKt.getAsJavaPsiElement(uMethod, PsiMethod.class);
      if (psiMethod == null) {
        return;
      }

      if (!psiMethod.hasModifierProperty("public")
              || psiMethod.hasModifierProperty("static")
              || psiMethod.isConstructor()) {
        return;
      }

      Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
      if (module == null) {
        return;
      }

      SemService semService = SemService.getSemService(psiElement.getProject());
      List<EventListenerElement> semElements = semService.getSemElements(EventListenerElement.EVENT_LISTENER_ROOT_JAM_KEY, psiMethod);
      for (EventListenerElement eventListener : semElements) {
        PsiAnnotation psiAnnotation = eventListener.getAnnotation();
        if (psiAnnotation != null) {
          annotateEventListenerMethod(() -> findPublishPoints(module, eventListener), uMethod, result);
        }
        else if (eventListener.getPsiElement() != null && "onApplicationEvent".equals(psiMethod.getName())) {
          annotateOnApplicationEventMethod(() -> findPublishPoints(module, eventListener), uMethod, result);
        }

        PsiType returnType = EventModelUtils.getEventType(psiMethod.getReturnType(), module);
        if (returnType != null && !PsiType.VOID.equals(returnType)) {
          PsiElement identifier = UElementKt.getSourcePsiElement(uMethod.getUastAnchor());
          if (identifier != null) {
            annotatePublishPoints(module.getProject(), module, result, returnType, identifier);
          }
        }
      }
    }
    else {
      UCallExpression callExpression = UastContextKt.toUElement(psiElement, UCallExpression.class);
      if (callExpression != null
              && callExpression.getSourcePsi() == psiElement
              && (EventModelUtils.isPublishEventExpression(callExpression)
              || EventModelUtils.isMulticastEventExpression(callExpression))) {
        annotateMethodCallExpression(callExpression, result);
      }
    }
  }

  private static Collection<PublishEventPointDescriptor> findPublishPoints(
          Module module, EventListenerElement eventListener) {
    if (!eventListener.isValid()) {
      return Collections.emptyList();
    }
    else {
      var points = new LinkedHashSet<PublishEventPointDescriptor>();
      for (PsiType handledType : EventModelUtils.getEventListenerHandledType(eventListener)) {
        points.addAll(EventModelUtils.getPublishPoints(module, handledType));
      }
      return points;
    }
  }

  private static void annotateOnApplicationEventMethod(
          Supplier<Collection<PublishEventPointDescriptor>> supplier,
          UMethod uMethod, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

    PsiElement identifier = UElementKt.getSourcePsiElement(uMethod.getUastAnchor());
    if (identifier != null) {
      result.add(createEventListenerMarker(supplier, identifier));
    }
  }

  private static void annotateEventListenerMethod(
          Supplier<Collection<PublishEventPointDescriptor>> supplier,
          UMethod uMethod, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

    PsiElement identifier = UElementKt.getSourcePsiElement(uMethod.getUastAnchor());
    if (identifier != null) {
      result.add(createEventListenerMarker(supplier, identifier));
    }
  }

  private static RelatedItemLineMarkerInfo<?> createEventListenerMarker(Supplier<Collection<PublishEventPointDescriptor>> supplier, PsiElement identifier) {

    var builder = SpringGutterIconBuilder.createBuilder(SpringApiIcons.Gutter.Publisher, PUBLISH_EVENT_CONVERTOR, null);
    builder.setTargets(NotNullLazyValue.lazy(supplier)).setPopupTitle(InfraBundle.message("event.publisher.choose.title"))
            .setTooltipText(InfraBundle.message("event.publisher.tooltip.text"))
            .setEmptyPopupText(InfraBundle.message("event.publisher.empty.tooltip.text"))
            .setCellRenderer(EventListenerAnnotator::getPublishEventRenderer);
    return builder.createSpringRelatedMergeableLineMarkerInfo(identifier);
  }

  private static DefaultPsiElementCellRenderer getPublishEventRenderer() {
    return new DefaultPsiElementCellRenderer() {
      protected Icon getIcon(PsiElement element) {
        return UastContextKt.toUElement(element, UCallExpression.class) != null ? SpringApiIcons.Gutter.Publisher : super.getIcon(element);
      }

      public String getContainerText(PsiElement element, String name) {
        if (UastContextKt.toUElement(element, UCallExpression.class) != null) {
          UMethod uMethod = UastContextKt.getUastParentOfType(element, UMethod.class);
          if (uMethod != null) {
            PsiMethod methodJavaPsi = uMethod.getJavaPsi();
            PsiClass containingClass = methodJavaPsi.getContainingClass();
            if (containingClass != null) {
              String var10000 = SymbolPresentationUtil.getSymbolPresentableText(containingClass);
              return var10000 + "." + SymbolPresentationUtil.getSymbolPresentableText(methodJavaPsi);
            }
          }
        }

        return super.getContainerText(element, name);
      }
    };
  }

  private static DefaultPsiElementCellRenderer getEventListenerRenderer() {
    return new DefaultPsiElementCellRenderer() {
      protected Icon getIcon(PsiElement element) {
        UMethod uMethod = UastContextKt.toUElement(element, UMethod.class);
        return uMethod != null && uMethod.getSourcePsi() != null ? super.getIcon(uMethod.getSourcePsi()) : super.getIcon(element);
      }
    };
  }

  private static void annotateMethodCallExpression(UCallExpression expression, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

    List<UExpression> expressions = expression.getValueArguments();
    if (expressions.size() > 0) {
      PsiType publishedType = expressions.get(0).getExpressionType();
      if (publishedType != null) {
        PsiElement sourcePsi = expression.getSourcePsi();
        if (sourcePsi == null) {
          return;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(sourcePsi);
        PsiElement identifier = UElementKt.getSourcePsiElement(expression.getMethodIdentifier());
        if (identifier != null) {
          annotatePublishPoints(sourcePsi.getProject(), module, result, publishedType, PsiTreeUtil.getDeepestFirst(identifier));
        }
      }
    }

  }

  private static void annotatePublishPoints(Project project, @Nullable Module module,
          Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiType publishedType, PsiElement element) {
    var builder = SpringGutterIconBuilder.createBuilder(SpringApiIcons.Gutter.Listener, EVENT_LISTENER_CONVERTOR, null);
    builder.setTargets(NotNullLazyValue.lazy(() -> EventModelUtils.getEventListeners(project, module, publishedType)))
            .setPopupTitle(InfraBundle.message("event.listener.choose.title"))
            .setTooltipText(InfraBundle.message("event.listener.tooltip.text"))
            .setEmptyPopupText(InfraBundle.message("event.listener.empty.tooltip.text"))
            .setCellRenderer(EventListenerAnnotator::getEventListenerRenderer);
    result.add(builder.createSpringRelatedMergeableLineMarkerInfo(element));
  }
}

