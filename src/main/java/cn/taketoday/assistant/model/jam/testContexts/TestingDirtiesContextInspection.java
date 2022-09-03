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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamEnumAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.highlighting.jam.AbstractInfraJavaInspection;
import cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts.ClassMode;
import cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts.HierarchyMode;
import cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts.InfraTestingDirtiesContext;
import cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts.MethodMode;
import cn.taketoday.lang.Nullable;

public final class TestingDirtiesContextInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
    if (module == null || !InfraTestContextUtil.of().isTestContextConfigurationClass(aClass)) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
    InfraTestingDirtiesContext dirtiesContext = InfraTestingDirtiesContext.CLASS_META.getJamElement(aClass);
    if (dirtiesContext != null) {
      checkMethodModeOnClass(holder, dirtiesContext);
      checkHierarchyMode(aClass, holder, dirtiesContext);
    }
    for (PsiMethod psiModifierListOwner : aClass.getMethods()) {
      InfraTestingDirtiesContext methodDirties = InfraTestingDirtiesContext.METHOD_META.getJamElement(psiModifierListOwner);
      if (methodDirties != null) {
        checkHierarchyMode(aClass, holder, methodDirties);
        checkClassModeOnMethod(holder, methodDirties);
      }
    }
    return holder.getResultsArray();
  }

  private static void checkMethodModeOnClass(ProblemsHolder holder, InfraTestingDirtiesContext dirtiesContext) {
    PsiAnnotationMemberValue psiElement;
    JamEnumAttributeElement<MethodMode> methodModeElement = dirtiesContext.getMethodModeElement();
    if (methodModeElement.getValue() != null && (psiElement = methodModeElement.getPsiElement()) != null) {
      holder.registerProblem(psiElement, InfraBundle.message("testing.model.dirties.method.mode.error.message"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );
    }
  }

  private static void checkClassModeOnMethod(ProblemsHolder holder, InfraTestingDirtiesContext methodDirties) {
    PsiAnnotationMemberValue psiElement;
    JamEnumAttributeElement<ClassMode> methodModeElement = methodDirties.getClassModeElement();
    if (methodModeElement.getValue() != null && (psiElement = methodModeElement.getPsiElement()) != null) {
      holder.registerProblem(psiElement, InfraBundle.message("testing.model.dirties.class.mode.error.message"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );
    }
  }

  private static void checkHierarchyMode(@Nullable PsiClass aClass, ProblemsHolder holder, InfraTestingDirtiesContext dirtiesContext) {
    PsiAnnotationMemberValue psiElement;
    if (aClass == null) {
      return;
    }
    JamEnumAttributeElement<HierarchyMode> hierarchyModeElement = dirtiesContext.getHierarchyModeElement();
    if (hierarchyModeElement.getValue() != null && InfraContextHierarchy.META.getJamElement(aClass) == null && (psiElement = hierarchyModeElement.getPsiElement()) != null) {
      holder.registerProblem(psiElement, InfraBundle.message("testing.model.dirties.hierarchy.mode.error.message"),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }
}
