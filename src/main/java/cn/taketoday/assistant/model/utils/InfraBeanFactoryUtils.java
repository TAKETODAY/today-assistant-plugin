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

package cn.taketoday.assistant.model.utils;

import com.intellij.openapi.module.Module;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraBeanFactoryUtils {

  public static final String GET_BEAN_METHOD = "getBean";
  private static final UCallExpressionPattern GET_BEAN_METHOD_WITH_BEAN_TYPE_PATTERN = UastPatterns.callExpression()
          .withResolvedMethod(
                  PsiJavaPatterns.psiMethod().withName(GET_BEAN_METHOD)
                          .withParameters("java.lang.Class", "java.lang.Object...").definedInClass(InfraConstant.BEAN_FACTORY_CLASS), false);
  private static final UCallExpressionPattern GET_BEAN_METHOD_WITH_BEAN_NAME_PATTERN = UastPatterns.callExpression().withResolvedMethod(
          PsiJavaPatterns.psiMethod().withName(GET_BEAN_METHOD).withParameters("java.lang.String", "java.lang.Object...").definedInClass(InfraConstant.BEAN_FACTORY_CLASS), false);

  public static boolean couldBeInitializedByBeanFactory(@Nullable Module module, PsiType beanType, String beanName, PsiType... psiTypes) {
    if (module == null) {
      return false;
    }
    GlobalSearchScope scope = module.getModuleWithDependenciesScope();
    for (UCallExpression uCallExpression : findGetBeanMethodCalls(module, scope)) {
      if (isBeanFactoryCallsForBean(beanType, beanName, uCallExpression, psiTypes)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isBeanFactoryCallsForBean(PsiType beanType, String beanName, UCallExpression uCallExpression, PsiType... types) {
    List<UExpression> arguments = uCallExpression.getValueArguments();
    if (arguments.size() > 0) {
      UExpression expression = arguments.get(0);
      if (beanName.equals(UastUtils.evaluateString(expression)) && paramsTypesAreEqual(arguments, types)) {
        return true;
      }
      Object evaluate = expression.evaluate();
      return (evaluate instanceof PsiType) && ((PsiType) evaluate).isAssignableFrom(beanType) && paramsTypesAreEqual(arguments, types);
    }
    return false;
  }

  public static Set<PsiElement> findBeanFactoryCallsForBean(@Nullable Module module, PsiType beanType, String beanName, PsiType... paramTypes) {
    if (module == null) {
      return Collections.emptySet();
    }
    Set<UCallExpression> calls = new HashSet<>();
    for (UCallExpression uCallExpression : findGetBeanMethodCalls(module)) {
      if (isBeanFactoryCallsForBean(beanType, beanName, uCallExpression, paramTypes)) {
        calls.add(uCallExpression);
      }
    }
    return calls.stream().map(UCallExpression::getSourcePsi).collect(Collectors.toSet());
  }

  private static boolean paramsTypesAreEqual(List<UExpression> arguments, PsiType... types) {
    if (arguments.size() - 1 != types.length) {
      return false;
    }
    for (int i = 0; i < types.length; i++) {
      PsiType expressionType = arguments.get(i + 1).getExpressionType();
      if (expressionType == null || !expressionType.isAssignableFrom(types[i])) {
        return false;
      }
    }
    return true;
  }

  private static Collection<? extends UCallExpression> findGetBeanMethodCalls(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      return CachedValueProvider.Result.create(findGetBeanMethodCalls(module, GlobalSearchScope.moduleWithDependenciesScope(module)), new Object[] { PsiModificationTracker.MODIFICATION_COUNT });
    });
  }

  private static Collection<? extends UCallExpression> findGetBeanMethodCalls(Module module, GlobalSearchScope scope) {
    return InfraUtils.findMethodCallsByPattern(module.getProject(), GET_BEAN_METHOD, scope, GET_BEAN_METHOD_WITH_BEAN_NAME_PATTERN,
            GET_BEAN_METHOD_WITH_BEAN_TYPE_PATTERN);
  }

  public static PsiType[] getParamTypes(PsiMethod method) {
    return Arrays.stream(method.getParameterList().getParameters()).map(PsiParameter::getType).toArray(PsiType[]::new);
  }
}
