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
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.InheritanceUtil;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UastContextKt;

import java.util.Arrays;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.InfraTestContextUtil;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraUastAutowiredMembersInspection extends AbstractInfraLocalInspection {

  public InfraUastAutowiredMembersInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    PsiElement sourcePsiElement;
    Module module;
    PsiClass aClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
    if ((sourcePsiElement = UElementKt.getSourcePsiElement(uClass)) != null
            && InfraUtils.isBeanCandidateClassInProject(aClass)
            && (module = ModuleUtilCore.findModuleForPsiElement(aClass)) != null) {
      if (InfraUtils.hasFacet(module) || InfraModelService.of().hasAutoConfiguredModels(module)) {
        ProblemsHolder holder = new ProblemsHolder(manager, sourcePsiElement.getContainingFile(), isOnTheFly);
        JavaClassInfo info = JavaClassInfo.from(aClass);
        if (!info.isStereotypeJavaBean() && !isSpringComponent(aClass, info) && !InfraTestContextUtil.of().isTestContextConfigurationClass(aClass) && !constraintValidator(aClass)) {
          for (PsiMember psiMember : aClass.getFields()) {
            checkMemberIsNotAutowired(holder, psiMember);
          }
          for (PsiMember psiMember2 : aClass.getMethods()) {
            checkMemberIsNotAutowired(holder, psiMember2);
          }
        }
        return holder.getResultsArray();
      }
      return null;
    }
    return null;
  }

  private static boolean constraintValidator(PsiClass aClass) {
    return InheritanceUtil.isInheritor(aClass, JavaeeConstant.JAVAX_CONSTRAINT_VALIDATOR)
            || InheritanceUtil.isInheritor(aClass, JavaeeConstant.JAKARTA_CONSTRAINT_VALIDATOR);
  }

  private static void checkMemberIsNotAutowired(ProblemsHolder holder, PsiMember psiMember) {
    UAnnotation uAnnotation;
    PsiElement annotationSourcePsi;
    PsiElement elementToReport;
    PsiAnnotation annotation = AnnotationUtil.findAnnotation(psiMember, true, AnnotationConstant.AUTOWIRED);
    if (annotation == null || (uAnnotation = UastContextKt.toUElement(annotation,
            UAnnotation.class)) == null || (annotationSourcePsi = uAnnotation.getSourcePsi()) == null || annotationSourcePsi.getContainingFile() != holder.getFile() || (elementToReport = UAnnotationKt.getNamePsiElement(
            uAnnotation)) == null) {
      return;
    }
    holder.registerProblem(elementToReport, InfraBundle.message("class.is.not.bean.autowired"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  private static boolean isSpringComponent(PsiClass aClass, JavaClassInfo info) {
    return info.isMapped() || isJamSpringComponent(aClass) || isAbstractBaseComponent(aClass);
  }

  private static boolean isJamSpringComponent(PsiClass aClass) {
    JamService jamService = JamService.getJamService(aClass.getProject());
    return InfraUtils.isStereotypeComponentOrMeta(aClass)
            || jamService.getJamElement(ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, aClass) != null
            || isAbstractConfiguration(aClass)
            || AnnotationUtil.isAnnotated(aClass, Arrays.asList(JavaeeConstant.JAVAX_SERVLET_WEB_SERVLET,
            JavaeeConstant.JAKARTA_SERVLET_WEB_SERVLET), 1)
            || AnnotationUtil.isAnnotated(aClass, Arrays.asList(JavaeeConstant.JAVAX_RS_PATH, JavaeeConstant.JAKARTA_RS_PATH), 1)
            || AnnotationUtil.isAnnotated(aClass, AnnotationConstant.CONTEXT_HIERARCHY, 1)
            || AnnotationUtil.isAnnotated(aClass, AnnotationConstant.CONFIGURABLE, 1);
  }

  private static boolean isAbstractConfiguration(PsiClass aClass) {
    return aClass.hasModifierProperty("abstract") && AnnotationUtil.isAnnotated(aClass, AnnotationConstant.CONFIGURATION, 1);
  }

  private static boolean isAbstractBaseComponent(PsiClass aClass) {
    Ref<Boolean> ref = new Ref<>();
    ref.set(false);
    PsiModifierList modifierList = aClass.getModifierList();
    if (modifierList != null && modifierList.hasModifierProperty("abstract")) {
      ClassInheritorsSearch.search(aClass).forEach(psiClass -> {
        if (isJamSpringComponent(psiClass)) {
          ref.set(true);
          return false;
        }
        return true;
      });
    }
    return ref.get();
  }
}
