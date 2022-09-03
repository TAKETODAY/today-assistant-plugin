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
package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.InheritanceUtil;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.expressions.UInjectionHost;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.converters.InfraBeanReferenceJamConverter;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class BeanPointerResolveInspection extends AbstractInfraJavaInspection {

  protected static boolean isPlainJavaFileInInfraModule(PsiElement psiElement) {
    if (!JamCommonUtil.isPlainJavaFile(psiElement.getContainingFile())) {
      return false;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    return InfraUtils.hasFacet(module) || InfraModelService.of().hasAutoConfiguredModels(module);
  }

  /**
   * Takes required bean type from associated {@link InfraBeanReferenceJamConverter}.
   *
   * @param holder Holder.
   * @param element JAM.
   */
  public static void checkBeanPointerResolve(ProblemsHolder holder, JamStringAttributeElement<BeanPointer<?>> element) {
    String beanType = null;
    JamConverter<BeanPointer<?>> converter = element.getConverter();
    if (converter instanceof InfraBeanReferenceJamConverter) {
      beanType = ((InfraBeanReferenceJamConverter) converter).getBaseClass();
    }
    checkBeanPointerResolve(holder, element, beanType);
  }

  public static void checkBeanPointerResolve(ProblemsHolder holder,
          JamStringAttributeElement<BeanPointer<?>> element, @Nullable String beanType) {
    String beanName = element.getStringValue();
    if (beanName != null) {
      PsiAnnotationMemberValue memberValue = element.getPsiElement();
      if (memberValue != null) {
        BeanPointer<?> value = element.getValue();
        if (value == null) {
          PsiElement problemPsiElement = getProblemPsiElement(memberValue);
          if (problemPsiElement != null) {
            holder.registerProblem(problemPsiElement, InfraBundle.message("model.bean.error.message", beanName),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          }
        }
        else {
          if (StringUtil.isNotEmpty(beanType)) {
            PsiClass psiClass = value.getBeanClass();
            if (psiClass != null && !InheritanceUtil.isInheritor(psiClass, beanType)) {
              PsiElement problemPsiElement = getProblemPsiElement(memberValue);
              if (problemPsiElement != null) {
                holder.registerProblem(problemPsiElement,
                        InfraBundle.message("bean.must.be.of.type", beanType),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
              }
            }
          }
        }
      }
    }
  }

  @Nullable
  private static PsiElement getProblemPsiElement(PsiElement element) {
    UElement uElement = UastContextKt.toUElement(element, UInjectionHost.class);
    if (uElement == null)
      return null;
    return uElement.getSourcePsi();
  }
}
