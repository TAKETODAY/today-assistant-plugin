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

package cn.taketoday.assistant.web.mvc;

import com.intellij.microservices.utils.CommonFakeNavigatablePomTarget;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.evaluation.UEvaluationContextKt;
import org.jetbrains.uast.values.UConstant;
import org.jetbrains.uast.values.UValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.web.mvc.model.VariableProvider;
import cn.taketoday.lang.Nullable;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public abstract class ServletsAttributeHolderProvider implements WebMvcVariableReferenceProvider, VariableProvider {
  private final String SET_ATTRIBUTE_METHOD_NAME;
  private final String GET_ATTRIBUTE_METHOD_NAME;
  private final String attributeHolderClassName;

  private final Function1<String, PomRenameableTarget<Object>> pomTargetProvider;

  public ServletsAttributeHolderProvider(String attributeHolderClassName, Function1<String, PomRenameableTarget<Object>> function1) {
    this.attributeHolderClassName = attributeHolderClassName;
    this.pomTargetProvider = function1;
    this.SET_ATTRIBUTE_METHOD_NAME = "setAttribute";
    this.GET_ATTRIBUTE_METHOD_NAME = "getAttribute";
  }

  protected final Function1<String, PomRenameableTarget<Object>> getPomTargetProvider() {
    return this.pomTargetProvider;
  }

  @Override

  public UExpressionPattern<?, ?> getPattern() {
    UExpressionPattern injectionHostUExpression$default = UastPatterns.injectionHostUExpression(false);
    ElementPattern definedInClass = PsiJavaPatterns.psiMethod()
            .withName(this.SET_ATTRIBUTE_METHOD_NAME, this.GET_ATTRIBUTE_METHOD_NAME)
            .definedInClass(this.attributeHolderClassName);
    return injectionHostUExpression$default.methodCallParameter(0, definedInClass, false);
  }

  @Override
  @Nullable
  public PomTargetPsiElement getResolveTarget(PsiElement element) {
    Intrinsics.checkNotNullParameter(element, "element");
    UCallExpression var10000 = UastUtils.getUCallExpression(UastContextKt.toUElement(element), 2);
    if (var10000 == null) {
      return null;
    }
    else {
      List expressions;
      UExpression var6;
      String var7;
      label49:
      {
        UCallExpression methodCallExpression = var10000;
        expressions = methodCallExpression.getValueArguments();
        var6 = (UExpression) CollectionsKt.firstOrNull(expressions);
        if (var6 != null) {
          var7 = UastUtils.evaluateString(var6);
          if (var7 != null) {
            break label49;
          }
        }

        Object var10;
        label40:
        {
          var6 = (UExpression) CollectionsKt.firstOrNull(expressions);
          if (var6 != null) {
            UValue var8 = UEvaluationContextKt.uValueOf(var6, (List) null);
            if (var8 != null) {
              UConstant var9 = var8.toConstant();
              if (var9 != null) {
                var10 = var9.getValue();
                break label40;
              }
            }
          }

          var10 = null;
        }

        if (!(var10 instanceof String)) {
          var10 = null;
        }

        var7 = (String) var10;
      }

      if (var7 != null) {
        String name = var7;
        var6 = (UExpression) CollectionsKt.getOrNull(expressions, 1);
        PsiType type = var6 != null ? var6.getExpressionType() : null;
        Project var10002;
        if (type != null) {
          var10002 = element.getProject();
          return new CommonFakePsiVariablePomTarget(var10002, this.pomTargetProvider.invoke(name), type);
        }
        else {
          var10002 = element.getProject();
          return new CommonFakeNavigatablePomTarget(var10002, this.pomTargetProvider.invoke(name));
        }
      }
      else {
        return null;
      }
    }
  }

  private final PsiMethod getDefinitionElement(Project project) {
    PsiClass findClass = JavaPsiFacade.getInstance(project).findClass(this.attributeHolderClassName, GlobalSearchScope.allScope(project));
    if (findClass != null) {
      PsiMethod[] findMethodsByName = findClass.findMethodsByName(this.SET_ATTRIBUTE_METHOD_NAME, false);
      if (findMethodsByName != null) {
        return ArraysKt.singleOrNull(findMethodsByName);
      }
    }
    return null;
  }

  private final Iterable<PsiVariable> doSearch(PsiMethod element) {
    Iterable search = MethodReferencesSearch.search(element, element.getUseScope(), true);
    Iterable $this$mapNotNull$iv = search;
    Collection destination$iv$iv = new ArrayList();
    for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
      PsiReference it = (PsiReference) element$iv$iv$iv;
      PsiElement element2 = it.getElement();
      PomTargetPsiElement mo0getResolveTarget = getResolveTarget(element2);
      if (!(mo0getResolveTarget instanceof PsiVariable)) {
        mo0getResolveTarget = null;
      }
      PsiVariable psiVariable = (PsiVariable) mo0getResolveTarget;
      if (psiVariable != null) {
        destination$iv$iv.add(psiVariable);
      }
    }
    return (List) destination$iv$iv;
  }

  @Override
  public Iterable<PsiVariable> getVariables(Project project) {
    PsiMethod it = getDefinitionElement(project);
    if (it != null) {
      Iterable<PsiVariable> doSearch = doSearch(it);
      if (doSearch != null) {
        return doSearch;
      }
    }
    return CollectionsKt.emptyList();
  }
}
