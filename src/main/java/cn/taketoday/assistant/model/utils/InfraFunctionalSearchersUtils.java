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

import com.intellij.model.search.SearchContext;
import com.intellij.model.search.SearchService;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.lang.Nullable;

public class InfraFunctionalSearchersUtils {

  public static Set<UCallExpression> findMethodsCalls(PsiMethod[] psiMethods, @Nullable SearchScope scope) {
    Set<UCallExpression> calls = new HashSet<>();
    for (PsiMethod method : psiMethods) {
      calls.addAll(findMethodCalls(method, scope));
    }
    return calls;
  }

  public static Set<UCallExpression> findMethodCalls(@Nullable PsiMethod psiMethod, @Nullable SearchScope scope) {
    if (psiMethod == null || scope == null)
      return Collections.emptySet();
    return MethodReferencesSearch.search(psiMethod, scope, false).findAll().stream()
            .map(reference -> {
              return
                      UastContextKt.getUastParentOfType(reference.getElement(), UCallExpression.class);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  public static Collection<? extends UCallExpression> findMethodCallsWithSearchService(@Nullable PsiMethod psiMethod,
          @Nullable SearchScope scope) {
    if (psiMethod == null || scope == null)
      return Collections.emptySet();

    return SearchService.getInstance().searchWord(psiMethod.getProject(), psiMethod.getName())
            .inContexts(SearchContext.IN_CODE)
            .inScope(scope).buildQuery(occurrence -> {
              if (occurrence.getOffsetInStart() != 0)
                return Collections.emptySet();
              UCallExpression uCallExpression = UastContextKt.getUastParentOfType(occurrence.getStart(), UCallExpression.class);
              return uCallExpression != null && psiMethod.equals(uCallExpression.resolve()) ?
                     Collections.singleton(uCallExpression) : Collections.emptySet();
            }).findAll();
  }

  @Nullable
  public static PsiMethod findMethod(String name, @Nullable PsiClass psiClass) {
    if (psiClass == null)
      return null;
    for (PsiMethod psiMethod : psiClass.findMethodsByName(name, true)) {
      if (psiMethod != null)
        return psiMethod;
    }
    return null;
  }

  public static PsiMethod[] findMethods(String name, @Nullable PsiClass psiClass) {
    if (psiClass == null)
      return PsiMethod.EMPTY_ARRAY;
    return psiClass.findMethodsByName(name, false);
  }

  public static UExpression getFirstInChain(@Nullable UExpression expression) {
    if (expression instanceof UCallExpression) {
      UExpression receiver = ((UCallExpression) expression).getReceiver();
      return receiver == null ? expression : getFirstInChain(receiver);
    }
    if (expression instanceof UQualifiedReferenceExpression) {
      return getFirstInChain(((UQualifiedReferenceExpression) expression).getReceiver());
    }
    return expression;
  }

  @Nullable
  public static UCallExpression findFirstCallExpressionInChain(@Nullable UExpression expression) {
    if (expression instanceof UCallExpression) {
      UExpression receiver = ((UCallExpression) expression).getReceiver();
      if (receiver == null || findFirstCallExpressionInChain(receiver) == null)
        return (UCallExpression) expression;
    }
    if (expression instanceof UQualifiedReferenceExpression) {
      return findFirstCallExpressionInChain(((UQualifiedReferenceExpression) expression).getSelector());
    }
    return null;
  }

  public static Collection<UCallExpression> collectCallExpressionsInChain(@Nullable UExpression expression,
          UCallExpressionPattern... patterns) {
    CommonProcessors.CollectProcessor<UCallExpression> processor = new CommonProcessors.CollectProcessor<>(new ArrayList<>());
    processCallExpressionsInChain(expression, processor, patterns);
    return processor.getResults();
  }

  @Nullable
  public static UCallExpression findFirstCallExpressionsInChain(@Nullable UExpression expression,
          UCallExpressionPattern... patterns) {
    CommonProcessors.FindFirstProcessor<UCallExpression> processor = new CommonProcessors.FindFirstProcessor<>();
    processCallExpressionsInChain(expression, processor, patterns);
    return processor.getFoundValue();
  }

  public static boolean processCallExpressionsInChain(@Nullable UExpression expression,
          Processor<UCallExpression> patternProcessor,
          UCallExpressionPattern... patterns) {
    if (expression instanceof UCallExpression) {
      if (!processCallExpression((UCallExpression) expression, patternProcessor, patterns))
        return false;
      UExpression receiver = ((UCallExpression) expression).getReceiver();
      if (receiver != null)
        return processCallExpressionsInChain(receiver, patternProcessor, patterns);
    }
    if (expression instanceof UQualifiedReferenceExpression) {
      return processCallExpressionsInChain(((UQualifiedReferenceExpression) expression).getSelector(), patternProcessor, patterns);
    }
    return true;
  }

  public static boolean processCallExpression(@Nullable UCallExpression expression,
          Processor<UCallExpression> processor,
          UCallExpressionPattern... patterns) {
    if (expression == null)
      return true;
    for (UCallExpressionPattern pattern : patterns) {
      if (pattern.accepts(expression) && !processor.process(expression))
        return false;
    }
    return true;
  }

  @Nullable
  public static String getUExpressionText(UExpression expression) {
    return UastUtils.evaluateString(expression);
  }
}
