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

package cn.taketoday.assistant.web.mvc.client.rest;

import com.intellij.jam.JavaLibraryUtils;
import com.intellij.microservices.jvm.cache.ScopedCacheValueHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.siyeh.ig.psiutils.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SpreadBuilder;

public final class RestOperationsUtils {

  public static final RestOperationsUtils INSTANCE = new RestOperationsUtils();

  public static boolean isRestOperationsAvailable(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    return JavaLibraryUtils.hasLibraryClass(project, InfraMvcConstant.REST_OPERATIONS) || JavaLibraryUtils.hasLibraryClass(project, InfraMvcConstant.ASYNC_REST_OPERATIONS);
  }

  public static boolean isTestRestTemplateAvailable(Module module) {
    Intrinsics.checkNotNullParameter(module, "module");
    return JavaLibraryUtils.hasLibraryClass(module, InfraMvcConstant.TEST_REST_TEMPLATE);
  }

  public static boolean isTestRestTemplateAvailable(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    return JavaLibraryUtils.hasLibraryClass(project, InfraMvcConstant.TEST_REST_TEMPLATE);
  }

  public static boolean isRestOperationsAvailable(Module module) {
    Intrinsics.checkNotNullParameter(module, "module");
    return JavaLibraryUtils.hasLibraryClass(module, InfraMvcConstant.REST_OPERATIONS) || JavaLibraryUtils.hasLibraryClass(module, InfraMvcConstant.ASYNC_REST_OPERATIONS);
  }

  public List<PsiMethod> getRestOperationsMethods(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    Object cachedValue = CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider() {
      @Nullable
      public CachedValueProvider.Result<List<PsiMethod>> compute() {
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        List methods = INSTANCE.findRestOperationsUrlMethods(project, allScope);
        return Result.createSingleDependency(methods, ProjectRootManager.getInstance(project));
      }
    });
    return (List) cachedValue;
  }

  public List<PsiMethod> getTestRestTemplateMethods(Project project) {
    Object cachedValue = CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider() {
      @Nullable
      public CachedValueProvider.Result<List<PsiMethod>> compute() {
        List methods;
        RestOperationsUtils restOperationsUtils = INSTANCE;
        Project project2 = project;
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        methods = restOperationsUtils.findTestRestTemplateUrlMethods(project2, allScope);
        return Result.createSingleDependency(methods, ProjectRootManager.getInstance(project));
      }
    });
    return (List) cachedValue;
  }

  public List<PsiClass> getRestOperationsApiClasses(ScopedCacheValueHolder<?> scopedCacheValueHolder) {
    return scopedCacheValueHolder.getCachedValue(new CachedValueProvider<>() {
      public CachedValueProvider.Result<List<PsiClass>> compute() {
        List<PsiClass> classes = INSTANCE.findRestOperationsApiClasses(scopedCacheValueHolder.getProject(), scopedCacheValueHolder.getApiSearchScope());
        return Result.createSingleDependency(classes, ProjectRootManager.getInstance(scopedCacheValueHolder.getProject()));
      }
    });
  }

  public List<PsiClass> getTestRestTemplateApiClasses(ScopedCacheValueHolder<?> scopedCacheValueHolder) {
    Intrinsics.checkNotNullParameter(scopedCacheValueHolder, "query");
    return (List) scopedCacheValueHolder.getCachedValue(new CachedValueProvider() {
      @Nullable
      public CachedValueProvider.Result<List<PsiClass>> compute() {
        List classes;
        classes = INSTANCE.findTestRestTemplateApiClasses(scopedCacheValueHolder.getProject(), scopedCacheValueHolder.getApiSearchScope());
        return Result.createSingleDependency(classes, ProjectRootManager.getInstance(scopedCacheValueHolder.getProject()));
      }
    });
  }

  public List<PsiClass> findRestOperationsApiClasses(Project project, GlobalSearchScope restOperationsSearchScope) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    SpreadBuilder spreadBuilder = new SpreadBuilder(2);
    PsiClass[] findClasses = javaPsiFacade.findClasses(InfraMvcConstant.REST_OPERATIONS, restOperationsSearchScope);
    Intrinsics.checkNotNullExpressionValue(findClasses, "javaPsiFacade.findClasse…estOperationsSearchScope)");
    spreadBuilder.addSpread(findClasses);
    PsiClass[] findClasses2 = javaPsiFacade.findClasses(InfraMvcConstant.ASYNC_REST_OPERATIONS, restOperationsSearchScope);
    Intrinsics.checkNotNullExpressionValue(findClasses2, "javaPsiFacade.findClasse…estOperationsSearchScope)");
    spreadBuilder.addSpread(findClasses2);
    return CollectionsKt.mutableListOf((PsiClass[]) spreadBuilder.toArray(new PsiClass[spreadBuilder.size()]));
  }

  public List<PsiClass> findTestRestTemplateApiClasses(Project project, GlobalSearchScope restOperationsSearchScope) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    PsiClass[] findClasses = javaPsiFacade.findClasses(InfraMvcConstant.TEST_REST_TEMPLATE, restOperationsSearchScope);
    Intrinsics.checkNotNullExpressionValue(findClasses, "javaPsiFacade.findClasse…estOperationsSearchScope)");
    return CollectionsKt.mutableListOf(Arrays.copyOf(findClasses, findClasses.length));
  }

  public List<PsiMethod> findRestOperationsUrlMethods(Project project, GlobalSearchScope apiSearchScope) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    List methods = new ArrayList();
    addMethods(javaPsiFacade, InfraMvcConstant.REST_OPERATIONS, apiSearchScope, methods, stringReceiver());
    addMethods(javaPsiFacade, InfraMvcConstant.ASYNC_REST_OPERATIONS, apiSearchScope, methods, stringReceiver());
    return methods;
  }

  public List<PsiMethod> findTestRestTemplateUrlMethods(Project project, GlobalSearchScope apiSearchScope) {
    List methods = new ArrayList();
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    addMethods(javaPsiFacade, InfraMvcConstant.TEST_REST_TEMPLATE, apiSearchScope, methods, stringReceiver());
    return methods;
  }

  private Function1<PsiMethod, Boolean> stringReceiver() {
    return new Function1<PsiMethod, Boolean>() {
      @Override
      public Boolean invoke(PsiMethod it) {
        if (it.hasParameters()) {
          PsiParameterList parameterList = it.getParameterList();
          PsiParameter psiParameter = parameterList.getParameters()[0];
          return TypeUtils.isJavaLangString(psiParameter.getType());
        }
        return false;
      }
    };
  }

  private void addMethods(JavaPsiFacade javaPsiFacade, String restOperations, GlobalSearchScope apiSearchScope, List<PsiMethod> list, Function1<? super PsiMethod, Boolean> function1) {
    for (PsiClass httpUrlClass : javaPsiFacade.findClasses(restOperations, apiSearchScope)) {
      for (RestOperation restOperationsMethod : RestOperationsConstants.INSTANCE.getREST_OPERATIONS_METHODS()) {
        PsiMethod[] findMethodsByName = httpUrlClass.findMethodsByName(restOperationsMethod.getMethod(), true);
        Collection destination$iv$iv = new ArrayList();
        for (PsiMethod psiMethod : findMethodsByName) {
          if (function1.invoke(psiMethod).booleanValue()) {
            destination$iv$iv.add(psiMethod);
          }
        }
        list.addAll(destination$iv$iv);
      }
    }
  }
}
