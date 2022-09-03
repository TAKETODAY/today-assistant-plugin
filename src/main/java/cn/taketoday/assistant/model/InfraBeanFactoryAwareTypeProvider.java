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

package cn.taketoday.assistant.model;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;

public class InfraBeanFactoryAwareTypeProvider extends BeanEffectiveTypeProvider {

  @Override
  public boolean processEffectiveTypes(CommonInfraBean bean, Processor<PsiType> processor) {
    PsiClass aClass;
    PsiType beanType = bean.getBeanType();
    if ((beanType instanceof PsiClassType psiClassType)
            && (aClass = psiClassType.resolve()) != null && isBeanFactoryAwareClass(aClass)) {
      for (PsiType psiType : getBeanFactoryAwareTypes(aClass)) {
        if (!processor.process(psiType)) {
          return false;
        }
      }
      return true;
    }
    return true;
  }

  private static Collection<PsiType> getBeanFactoryAwareTypes(PsiClass psiClass) {
    Set<PsiType> types = new HashSet<>();
    PsiClass factory = JavaPsiFacade.getInstance(psiClass.getProject())
            .findClass(InfraConstant.CONFIGURABLE_BEAN_FACTORY, psiClass.getResolveScope());
    if (factory != null) {
      PsiMethod[] resolvableDependencies = factory.findMethodsByName("registerDependency", false);
      if (resolvableDependencies.length > 0) {
        PsiMethod registerMethod = resolvableDependencies[0];
        PsiElement[] findMethodsByName = psiClass.findMethodsByName("setBeanFactory", false);
        if (findMethodsByName.length > 0) {
          MethodReferencesSearch.search(registerMethod, new LocalSearchScope(findMethodsByName[0]), true).forEach(psiReference -> {
            PsiClass genericClass;
            PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiMethodCallExpression.class);
            if (methodCallExpression != null) {
              PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
              if (expressions.length > 0) {
                PsiExpression dependencyTypeExpression = expressions[0];
                PsiType expressionType = dependencyTypeExpression.getType();
                if (expressionType instanceof PsiClassType) {
                  PsiType substituteTypeParameter = PsiUtil.substituteTypeParameter(expressionType, "java.lang.Class", 0, true);
                  if ((substituteTypeParameter instanceof PsiClassType psiClassType)
                          && (genericClass = psiClassType.resolve()) != null) {
                    types.add(PsiTypesUtil.getClassType(genericClass));
                  }
                }
              }
            }
          });
        }
      }
    }
    return types;
  }

  private static boolean isBeanFactoryAwareClass(PsiClass psiClass) {
    return CachedValuesManager.getCachedValue(psiClass, () -> {
      boolean isBeanFactory = InheritanceUtil.isInheritor(psiClass, InfraConstant.BEAN_FACTORY_AWARE);
      return CachedValueProvider.Result.createSingleDependency(isBeanFactory, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }
}
