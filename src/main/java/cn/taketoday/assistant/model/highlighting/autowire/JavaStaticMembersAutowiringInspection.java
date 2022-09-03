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

package cn.taketoday.assistant.model.highlighting.autowire;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;

import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.highlighting.jam.AbstractInfraJavaInspection;

public final class JavaStaticMembersAutowiringInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkMethod(PsiMethod psiMethod, InspectionManager manager, boolean isOnTheFly) {
    if (!JamCommonUtil.isPlainJavaFile(psiMethod.getContainingFile()) || !psiMethod.hasModifierProperty("static") || !isAutowiredByAnnotation(psiMethod)) {
      return null;
    }
    return new ProblemDescriptor[] { createDescriptor(psiMethod.getNameIdentifier(), manager, isOnTheFly) };
  }

  private boolean isAutowiredByAnnotation(PsiMember owner) {
    Set<String> autowireAnnotations = AutowireUtil.getAutowiredAnnotations(ModuleUtilCore.findModuleForPsiElement(owner));
    return AnnotationUtil.isAnnotated(owner, autowireAnnotations, 0);
  }

  public ProblemDescriptor[] checkField(PsiField psiField, InspectionManager manager, boolean isOnTheFly) {
    if (!JamCommonUtil.isPlainJavaFile(psiField.getContainingFile()) || !psiField.hasModifierProperty("static") || !isAutowiredByAnnotation(psiField)) {
      return null;
    }
    return new ProblemDescriptor[] { createDescriptor(psiField.getNameIdentifier(), manager, isOnTheFly) };
  }

  private static ProblemDescriptor createDescriptor(PsiElement psiElement, InspectionManager manager, boolean isOnTheFly) {
    return manager.createProblemDescriptor(psiElement, InfraBundle.message("static.members.autowiring"), isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }
}
