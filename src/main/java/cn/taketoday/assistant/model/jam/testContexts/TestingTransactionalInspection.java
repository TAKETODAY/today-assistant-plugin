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

package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.highlighting.jam.AbstractInfraJavaInspection;

public final class TestingTransactionalInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
    if (module == null || !InfraTestContextUtil.of().isTestContextConfigurationClass(aClass)) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
    for (PsiMethod psiModifierListOwner : aClass.getMethods()) {
      PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiModifierListOwner,
              AnnotationConstant.TEST_BEFORE_TRANSACTION, AnnotationConstant.TEST_AFTER_TRANSACTION);
      if (annotation != null) {
        checkLifecycleTransactionMethod(holder, psiModifierListOwner, annotation);
      }
    }
    return holder.getResultsArray();
  }

  private static void checkLifecycleTransactionMethod(ProblemsHolder holder, PsiMethod method, PsiAnnotation annotation) {
    if (method.getParameterList().getParametersCount() > 0) {
      holder.registerProblem(annotation, InfraBundle.message("testing.transactional.wrong.number.of.arguments.error.message"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
    if (!PsiType.VOID.equals(method.getReturnType())) {
      holder.registerProblem(annotation, InfraBundle.message("testing.transactional.void.method.return.type.error.message"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }
}
