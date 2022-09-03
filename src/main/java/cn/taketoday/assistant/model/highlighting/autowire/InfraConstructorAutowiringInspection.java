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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraConstructorAutowiringInspection extends AbstractInfraLocalInspection {

  public InfraConstructorAutowiringInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass aClass, InspectionManager manager, boolean isOnTheFly) {
    Module module;
    PsiElement identifier;
    PsiElement identifier2;
    PsiClass psiClass = aClass.getJavaPsi();
    PsiElement sourcePsi = aClass.getSourcePsi();
    if (sourcePsi == null || psiClass.isInterface() || psiClass.hasModifierProperty("abstract") || !AutowireUtil.isAutowiringRelevantClass(
            psiClass) || (module = ModuleUtilCore.findModuleForPsiElement(psiClass)) == null) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
    JavaClassInfo info = JavaClassInfo.from(psiClass);
    if (info.isStereotypeJavaBean()) {
      Set<String> annotations = AutowireUtil.getAutowiredAnnotations(module);
      Map<PsiMethod, PsiAnnotation> autowiredConstructors = new HashMap<>();
      PsiMethod[] constructors = psiClass.getConstructors();
      boolean hasNoArgConstructor = constructors.length == 0;
      for (PsiMethod method : constructors) {
        if (!hasNoArgConstructor && method.getParameterList().getParametersCount() == 0) {
          hasNoArgConstructor = true;
        }
        for (String annotation : annotations) {
          PsiAnnotation autowiredAnno = AnnotationUtil.findAnnotation(method, true, annotation);
          if (autowiredAnno != null) {
            autowiredConstructors.put(method, autowiredAnno);
          }
        }
      }
      if (InfraUtils.isStereotypeComponentOrMeta(psiClass)
              && !hasNoArgConstructor
              && autowiredConstructors.size() == 0
              && constructors.length > 1
              && (identifier2 = UElementKt.getSourcePsiElement(aClass.getUastAnchor())) != null) {
        holder.registerProblem(identifier2,
                InfraBundle.message("class.without.matching.constructor.for.autowiring"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }
      Set<PsiMethod> requiredConstructors = getRequiredConstructors(autowiredConstructors);
      if (requiredConstructors.size() > 1) {
        for (PsiMethod constructor : requiredConstructors) {
          UMethod uMethod = UastContextKt.toUElement(constructor, UMethod.class);
          if (uMethod != null && (identifier = UElementKt.getSourcePsiElement(uMethod.getUastAnchor())) != null) {
            holder.registerProblem(identifier, InfraBundle.message("multiple.autowiring.constructor"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
          }
        }
      }
    }
    return holder.getResultsArray();
  }

  private static Set<PsiMethod> getRequiredConstructors(Map<PsiMethod, PsiAnnotation> constructors) {
    Set<PsiMethod> requiredConstructors = new HashSet<>();
    for (Map.Entry<PsiMethod, PsiAnnotation> constructor : constructors.entrySet()) {
      PsiAnnotation annotation = constructor.getValue();
      PsiMethod method = constructor.getKey();
      if (AnnotationConstant.AUTOWIRED.equals(annotation.getQualifiedName())) {
        Boolean required = AnnotationUtil.getBooleanAttributeValue(annotation, "required");
        if (required == null || required) {
          requiredConstructors.add(method);
        }
      }
      else {
        requiredConstructors.add(method);
      }
    }
    return requiredConstructors;
  }
}
