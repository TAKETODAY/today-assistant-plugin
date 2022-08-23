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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.spring.gutter.SpringBeansPsiElementCellRenderer;
import com.intellij.spring.settings.SpringGeneralSettings;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 17:19
 */
public class AbstractInfraAnnotator extends RelatedItemLineMarkerProvider {
  private static final Logger LOG = Logger.getInstance(AbstractInfraAnnotator.class);

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UElement element = UastUtils.getUParentForIdentifier(psiElement);
    if (element instanceof UMethod) {
      annotateMethod((UMethod) element, result);
    }
    else if (element instanceof UClass) {
      annotateClass(result, (UClass) element);
    }
  }

  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass) {
    PsiElement identifier = UElementKt.getSourcePsiElement(uClass.getUastAnchor());
    if (identifier != null) {
      annotateClass(result, uClass, identifier);
    }
  }

  protected void annotateMethod(UMethod method, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiElement identifier = getAnchorSafe(method);
    if (identifier != null) {
      annotateMethod(method, identifier, result);
    }
  }

  protected void annotateMethod(UMethod uMethod, PsiElement identifier, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

  }

  protected void annotateClass(Collection<? super RelatedItemLineMarkerInfo<?>> result, UClass uClass, PsiElement identifier) {

  }

  public final void collectNavigationMarkers(List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {
    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement != null
            && hasFacetsOrAutoConfigurationMode(psiElement)
            && InfraLibraryUtil.hasLibrary(psiElement.getProject())) {
      super.collectNavigationMarkers(elements, result, forNavigation);
    }
  }

  private static boolean hasFacetsOrAutoConfigurationMode(PsiElement psiElement) {
    return CommonUtils.hasFacets(psiElement.getProject())
            || SpringGeneralSettings.getInstance(psiElement.getProject()).isAllowAutoConfigurationMode();
  }

  protected static void addJavaBeanGutterIcon(Collection<? super RelatedItemLineMarkerInfo<?>> result, PsiElement psiIdentifier,
          NotNullLazyValue<Collection<? extends CommonModelElement>> targets, Icon icon) {
    var builder = GutterIconBuilder.create(
            icon,
            NavigationGutterIconBuilderUtil.COMMON_MODEL_ELEMENT_CONVERTOR,
            NavigationGutterIconBuilderUtil.COMMON_MODEL_ELEMENT_GOTO_PROVIDER
    );

    builder.setTargets(targets)
            .setEmptyPopupText(InfraBundle.message("gutter.navigate.no.matching.beans"))
            .setPopupTitle(InfraBundle.message("bean.class.navigate.choose.class.title"))
            .setCellRenderer(SpringBeansPsiElementCellRenderer::new)
            .setTooltipText(InfraBundle.message("bean.class.tooltip.navigate.declaration"));
    result.add(builder.createGroupLineMarkerInfo(psiIdentifier));
  }

  @Nullable
  private static PsiElement getAnchorSafe(UMethod method) {
    PsiElement identifier = UElementKt.getSourcePsiElement(method.getUastAnchor());
    if (identifier == null) {
      return null;
    }
    else if (identifier.isValid() && identifier.getContainingFile() != null) {
      return identifier;
    }
    else {
      SmartList<Attachment> attachments = new SmartList<>();
      PsiElement sourcePsi = method.getSourcePsi();
      if (sourcePsi != null) {
        attachments.add(new Attachment("uMethod.sourcePsi", sourcePsi.isValid() ? sourcePsi.getText() : "<invalid>"));
        PsiFile containingFile = sourcePsi.isValid() ? sourcePsi.getContainingFile() : null;
        if (containingFile != null) {
          attachments.add(new Attachment(containingFile.getName(), containingFile.isValid() ? containingFile.getText() : "<invalid file>"));
        }
      }

      LOG.error("invalid identifier came from " + method + " of " + method.getClass() + " is valid = " + identifier.isValid() + " is physical = " + identifier.isPhysical(),
              attachments.toArray(Attachment.EMPTY_ARRAY));
      return null;
    }
  }
}

