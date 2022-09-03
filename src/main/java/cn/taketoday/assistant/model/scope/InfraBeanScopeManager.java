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
package cn.taketoday.assistant.model.scope;

import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraBeanScopeManager {

  /**
   * Returns all <em>custom</em> bean scopes applicable in the current context.
   *
   * @param element Context.
   * @return Custom bean scopes.
   * @see InfraCustomBeanScope
   */
  public static List<BeanScope> getCustomBeanScopes(@Nullable PsiElement element) {
    if (element == null || DumbService.isDumb(element.getProject())) {
      return Collections.emptyList();
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return Collections.emptyList();
    }
    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(element.getProject());
    PsiFile containingFile = element.getContainingFile();
    Set<InfraModel> models = InfraManager.from(element.getProject()).getInfraModelsByFile(containingFile);
    List<BeanScope> customScopes = new SmartList<>();
    for (InfraCustomBeanScope customBeanScope : InfraCustomBeanScope.EP_NAME.getExtensions()) {
      PsiClass scopeClass = javaPsiFacade.findClass(customBeanScope.getScopeClassName(), searchScope);
      if (scopeClass != null && !customBeanScope.process(customScopes, models, scopeClass, element)) {
        break;
      }
    }
    customScopes.addAll(ContainerUtil.map(getRegisteredScopes(module, models), BeanScope::new));
    return customScopes;
  }

  private static Collection<String> getRegisteredScopes(Module module, Set<InfraModel> models) {
    PsiClass beanFactoryPostProcessorClass = InfraUtils.findLibraryClass(module, InfraConstant.BEAN_FACTORY_POST_PROCESSOR);
    if (beanFactoryPostProcessorClass == null) {
      return Collections.emptySet();
    }
    PsiClass factory = InfraUtils.findLibraryClass(module, InfraConstant.CONFIGURABLE_BEAN_FACTORY);
    if (factory == null) {
      return Collections.emptySet();
    }
    PsiMethod[] registerScopeMethods = factory.findMethodsByName("registerScope", false);
    if (registerScopeMethods.length != 1) {
      return Collections.emptySet();
    }
    ModelSearchParameters.BeanClass searchParams = ModelSearchParameters.byClass(beanFactoryPostProcessorClass).withInheritors();
    Set<String> scopes = new HashSet<>();
    PsiMethod registerScopeMethod = registerScopeMethods[0];
    for (InfraModel model : models) {
      for (BeanPointer pointer : InfraModelSearchers.findBeans(model, searchParams)) {
        PsiClass beanClass = pointer.getBeanClass();
        if (beanClass != null) {
          PsiElement[] findMethodsByName = beanClass.findMethodsByName("postProcessBeanFactory", false);
          if (findMethodsByName.length > 0) {
            MethodReferencesSearch.search(registerScopeMethod, new LocalSearchScope(findMethodsByName[0]), true).forEach(psiReference -> {
              Pair<PsiElement, String> evaluatedExpression;
              PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiMethodCallExpression.class);
              if (methodCallExpression != null) {
                PsiElement[] expressions = methodCallExpression.getArgumentList().getExpressions();
                if (expressions.length > 0 && (evaluatedExpression = StringExpressionHelper.evaluateExpression(expressions[0])) != null) {
                  ContainerUtil.addIfNotNull(scopes, evaluatedExpression.getSecond());
                }
              }
            });
          }
        }
      }
    }
    return scopes;
  }
}
