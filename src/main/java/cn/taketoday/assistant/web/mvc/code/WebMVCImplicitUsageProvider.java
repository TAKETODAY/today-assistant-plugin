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

package cn.taketoday.assistant.web.mvc.code;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.jam.JavaLibraryUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.InheritanceUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public class WebMVCImplicitUsageProvider implements ImplicitUsageProvider {
  private static final Collection<String> SPRING_MVC_IMPLICIT_USAGE_ANNOTATIONS = List.of(
          InfraMvcConstant.REQUEST_MAPPING, InfraMvcConstant.MODEL_ATTRIBUTE,
          InfraMvcConstant.INIT_BINDER, InfraMvcConstant.EXCEPTION_HANDLER
  );
  private static final Collection<String> SPRING_MVC_PARAMETER_ANNOTATIONS = List.of(
          InfraMvcConstant.PATH_VARIABLE, InfraMvcConstant.REQUEST_PARAM
  );

  public boolean isImplicitUsage(PsiElement element) {
    return isImplicitWrite(element) || isSpecialClass(element) || isImplicitParameter(element);
  }

  private static boolean isImplicitParameter(PsiElement element) {
    return (element instanceof PsiParameter) && AnnotationUtil.isAnnotated((PsiParameter) element, SPRING_MVC_PARAMETER_ANNOTATIONS, 0) && hasCustomInterceptors(
            ModuleUtilCore.findModuleForPsiElement(element));
  }

  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  private static boolean hasCustomInterceptors(@Nullable Module module) {
    PsiClass interceptorClass;
    return module != null && (interceptorClass = JavaPsiFacade.getInstance(module.getProject())
            .findClass(InfraMvcConstant.ASYNC_HANDLER_INTERCEPTOR, module.getModuleWithLibrariesScope())) != null && ClassInheritorsSearch.search(interceptorClass,
            module.getModuleWithDependenciesScope(), true).findFirst() != null;
  }

  public boolean isImplicitWrite(PsiElement element) {
    return isAnnotated(element);
  }

  private static boolean isAnnotated(PsiElement element) {
    PsiModifierListOwner modifierListOwner;
    PsiModifierList modifierList;
    if (!isRelevantAnnotationElement(element) || (modifierList = (modifierListOwner = (PsiModifierListOwner) element).getModifierList()) == null || modifierList.getAnnotations().length == 0) {
      return false;
    }
    return AnnotationUtil.isAnnotated(modifierListOwner, SPRING_MVC_IMPLICIT_USAGE_ANNOTATIONS, 0) || ((element instanceof PsiMethod) && InfraControllerUtils.isJamRequestHandler(
            (PsiMethod) element));
  }

  private static boolean isRelevantAnnotationElement(PsiElement element) {
    return (element instanceof PsiParameter) || (element instanceof PsiMethod);
  }

  private static boolean isSpecialClass(PsiElement element) {
    return (element instanceof PsiClass) && JavaLibraryUtils.hasLibraryClass(element.getProject(), InfraMvcConstant.WEB_APPLICATION_INITIALIZER) && InheritanceUtil.isInheritor((PsiClass) element,
            InfraMvcConstant.WEB_APPLICATION_INITIALIZER);
  }
}
