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

package cn.taketoday.assistant.spel;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.light.DefiniteLightVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.lang.Nullable;
import kotlin.Pair;

final class CacheableContextVariables extends SpringElContextsExtension {
  private static final List<String> CACHE_ANNOTATIONS = List.of(
          "cn.taketoday.cache.annotation.Cacheable",
          "cn.taketoday.cache.annotation.CacheEvict",
          "cn.taketoday.cache.annotation.CachePut"
  );

  @Override
  public Collection<PsiMethod> getRootMethods(PsiElement context) {
    PsiAnnotation annotation = getCacheableAnnotationContext(context);
    if (annotation == null) {
      return Collections.emptyList();
    }
    else {
      PsiClass rootObjectClass = getRootObjectClass(annotation.getProject());
      if (rootObjectClass != null) {
        Set<PsiMethod> methods = new HashSet<>();
        PsiMethod[] var5 = rootObjectClass.getAllMethods();
        for (PsiMethod psiMethod : var5) {
          if (psiMethod.hasModifierProperty("public") && !psiMethod.isConstructor()) {
            PsiClass containingClass = psiMethod.getContainingClass();
            if (containingClass != null && !"java.lang.Object".equals(containingClass.getQualifiedName())) {
              methods.add(psiMethod);
            }
          }
        }

        return methods;
      }
      else {
        return super.getRootMethods(context);
      }
    }
  }

  @Override
  public Collection<? extends PsiVariable> getContextVariables(PsiElement context) {
    PsiAnnotation annotation = getCacheableAnnotationContext(context);
    if (annotation == null) {
      return Collections.emptyList();
    }
    else {
      Collection<PsiVariable> variables = new SmartList<>();
      PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
      if (method != null) {
        PsiType returnType = method.getReturnType();
        PsiTypeElement returnTypeElement = method.getReturnTypeElement();
        if (returnType != null && returnTypeElement != null) {
          variables.add(new ElContextVariable("result", returnType, returnTypeElement));
        }

        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; ++i) {
          PsiParameter parameter = parameters[i];
          PsiType parameterType = parameter.getType();
          variables.add(parameter);
          variables.add(new ElContextVariable("a" + i, parameterType, parameter));
          variables.add(new ElContextVariable("p" + i, parameterType, parameter));
        }
      }

      ContainerUtil.addIfNotNull(variables, getCacheRootObject(context.getProject()));

      return variables;
    }
  }

  @Nullable
  private static PsiAnnotation getCacheableAnnotationContext(PsiElement context) {
    PsiElement element = context.getContext();
    if (element == null) {
      return null;
    }
    else {
      Pair<PsiAnnotation, String> annotationEntry = UAnnotationUtils.getContainingAnnotationEntry(UastContextKt.toUElement(element));
      if (annotationEntry == null) {
        return null;
      }
      else {
        PsiAnnotation annotation = annotationEntry.getFirst();
        Module module = ModuleUtilCore.findModuleForPsiElement(annotation);
        if (!InfraLibraryUtil.hasLibrary(module)) {
          return null;
        }
        else {
          String annoFqn = annotation.getQualifiedName();
          return annoFqn != null && isCacheAnnotation(module, annoFqn) ? annotation : null;
        }
      }
    }
  }

  private static boolean isCacheAnnotation(Module module, String annoFqn) {
    if (!CACHE_ANNOTATIONS.contains(annoFqn)) {
      Iterator<String> var2 = CACHE_ANNOTATIONS.iterator();
      String cacheAnnotation;
      do {
        if (!var2.hasNext()) {
          return false;
        }
        cacheAnnotation = var2.next();
      }
      while (!AliasedAttributeInjectionContext.isCustomAnnotation(module, annoFqn, cacheAnnotation));
    }
    return true;
  }

  private static PsiVariable getCacheRootObject(Project project) {
    return (PsiVariable) CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      PsiClass rootObjectClass = getRootObjectClass(project);
      return rootObjectClass == null
             ? Result.create((Object) null, ProjectRootManager.getInstance(project))
             : Result.create(
                     new DefiniteLightVariable("root", PsiTypesUtil.getClassType(rootObjectClass), rootObjectClass),
                     new Object[] { rootObjectClass, ProjectRootManager.getInstance(project) });
    });
  }

  private static PsiClass getRootObjectClass(Project project) {
    return JavaPsiFacade.getInstance(project).findClass("cn.taketoday.cache.interceptor.CacheExpressionRootObject", GlobalSearchScope.allScope(project));
  }
}
