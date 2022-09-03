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

package cn.taketoday.assistant.web.mvc.model.mappings;

import com.intellij.microservices.MicroservicesBundle;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.utils.PomTargetUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.Nls;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.Objects;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.lang.Nullable;

public class UrlMappingPsiBasedElement implements UrlMappingElement {
  @Nullable
  private final PomTarget predefinedPomTarget;
  private final SmartPsiElementPointer<PsiElement> myDefinition;
  private final UrlPath myUrlPath;
  private final String presentation;
  private final RequestMethod[] method;

  public UrlMappingPsiBasedElement(String url, PsiElement navigationElement, @Nullable PomTarget predefinedPomTarget, @Nullable String presentation, RequestMethod[] method) {
    this.predefinedPomTarget = predefinedPomTarget;
    Logger.getInstance(UrlMappingPsiBasedElement.class).assertTrue(navigationElement.isPhysical() || (navigationElement instanceof PsiModifierListOwner), navigationElement);
    this.myDefinition = SmartPointerManager.createPointer(navigationElement);
    this.myUrlPath = InfraMvcUrlPathSpecification.INSTANCE.parsePath(url);
    this.presentation = (presentation == url || presentation == null) ? this.myUrlPath.getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION) : presentation;
    this.method = method;
  }

  public String toString() {
    return this.myUrlPath + " (" + this.presentation + ")";
  }

  @Override
  public PsiElement getNavigationTarget() {
    return this.myDefinition.getElement();
  }

  @Override
  @Nullable
  public PsiElement getDocumentationPsiElement() {
    UDeclaration uDeclaration;
    UElement uElement = UastContextKt.toUElement(this.myDefinition.getElement());
    if (uElement != null && (uDeclaration = UastUtils.getParentOfType(uElement, UDeclaration.class, false)) != null) {
      return uDeclaration.getSourcePsi();
    }
    return getNavigationTarget();
  }

  @Override
  @Deprecated
  public String getURL() {
    return this.myUrlPath.getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION);
  }

  @Override

  public UrlPath getUrlPath() {
    UrlPath urlPath = this.myUrlPath;
    return urlPath;
  }

  @Override
  public String getPresentation() {
    return this.presentation;
  }

  @Override
  public RequestMethod[] getMethod() {
    return this.method;
  }

  @Override
  @Nullable
  public PomNamedTarget getPomTarget() {
    if (this.predefinedPomTarget != null) {
      return PomTargetUtils.toPomRenameableTarget(this.predefinedPomTarget, presentation, Icons.RequestMapping, getTypeName());
    }
    PsiElement navigationTarget = getNavigationTarget();
    if (navigationTarget != null) {
      return toPomTarget(navigationTarget);
    }
    return null;
  }

  protected PomRenameableTarget<?> toPomTarget(PsiElement psiElement) {
    return PomTargetUtils.toPomRenameableTarget(psiElement, presentation, Icons.RequestMapping, getTypeName());
  }

  @Nls
  protected String getTypeName() {
    return MicroservicesBundle.message("microservices.url.path.segment");
  }

  @Override
  public boolean isDefinedInBean(BeanPointer<? extends CommonInfraBean> controllerBeanPointer) {
    PsiClass variantPsiClass;
    PsiElement psiElement = this.myDefinition.getElement();
    if (psiElement != null && (variantPsiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class, false)) != null && (controllerBeanPointer instanceof JamBeanPointer)) {
      PsiManager psiManager = variantPsiClass.getManager();
      if (psiManager.areElementsEquivalent(variantPsiClass, controllerBeanPointer.getBeanClass())) {
        return true;
      }
      PsiClass controllerClass = controllerBeanPointer.getBeanClass();
      if (controllerClass != null && controllerClass.isValid()) {
        for (PsiElement psiElement2 : controllerClass.getSupers()) {
          if (psiManager.areElementsEquivalent(variantPsiClass, psiElement2)) {
            return true;
          }
        }
        return false;
      }
      return false;
    }
    return false;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UrlMappingPsiBasedElement element = (UrlMappingPsiBasedElement) o;
    return Objects.equals(this.predefinedPomTarget, element.predefinedPomTarget) && Objects.equals(this.myDefinition.getElement(), element.myDefinition.getElement()) && this.myUrlPath.equals(
            element.myUrlPath);
  }

  public int hashCode() {
    return Objects.hash(this.predefinedPomTarget, this.myDefinition.getElement(), this.myUrlPath);
  }
}
