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

import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotationTargetsSearch;
import com.intellij.util.Query;

import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.expressions.UInjectionHost;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.web.mvc.model.VariableProvider;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;

public abstract class AnnotationAttributeVariableProvider implements WebMvcVariableReferenceProvider, VariableProvider {
  private final String annotationClass;
  private final List<String> annotationAttributes;
  private final Function1<String, PomRenameableTarget<Object>> pomTargetProvider;

  public AnnotationAttributeVariableProvider(String annotationClass, List<String> list, Function1<String, PomRenameableTarget<Object>> function1) {
    this.annotationClass = annotationClass;
    this.annotationAttributes = list;
    this.pomTargetProvider = function1;
  }

  private final PsiClass getDefinitionElement(Project project) {
    return JavaPsiFacade.getInstance(project).findClass(this.annotationClass, GlobalSearchScope.allScope(project));
  }

  @Override
  @Nullable
  public CommonFakePsiVariablePomTarget getResolveTarget(PsiElement element) {
    String name;
    UInjectionHost host = UastContextKt.toUElement(element, UInjectionHost.class);
    if (host == null || (name = host.evaluateToString()) == null) {
      return null;
    }
    Project project = host.getPsiLanguageInjectionHost().getProject();
    if (getDefinitionElement(project) == null) {
      return null;
    }
    PsiType javaLangObject = PsiType.getJavaLangObject(PsiManager.getInstance(project), element.getResolveScope());
    return new CommonFakePsiVariablePomTarget(project, this.pomTargetProvider.invoke(name), javaLangObject);
  }

  private Iterable<PsiVariable> doSearch(PsiClass element) {
    CommonFakePsiVariablePomTarget commonFakePsiVariablePomTarget;
    Query<PsiModifierListOwner> search = AnnotationTargetsSearch.search(element, element.getUseScope());
    ArrayList<PsiVariable> list = new ArrayList<>();
    for (PsiModifierListOwner psiModifierListOwner : search) {
      PsiAnnotation annotation = psiModifierListOwner.getAnnotation(this.annotationClass);
      if (annotation != null) {
        for (String attribute : this.annotationAttributes) {
          PsiAnnotationMemberValue it = annotation.findDeclaredAttributeValue(attribute);
          if (it != null) {
            commonFakePsiVariablePomTarget = getResolveTarget(it);
          }
          else {
            commonFakePsiVariablePomTarget = null;
          }
          if (commonFakePsiVariablePomTarget != null) {
            list.add(commonFakePsiVariablePomTarget);
          }
        }

      }
    }
    return list;
  }

  @Override

  public Iterable<PsiVariable> getVariables(Project project) {
    PsiClass it = getDefinitionElement(project);
    if (it != null) {
      Iterable<PsiVariable> doSearch = doSearch(it);
      if (doSearch != null) {
        return doSearch;
      }
    }
    return CollectionsKt.emptyList();
  }

  @Override

  public UExpressionPattern<?, ?> getPattern() {
    UExpressionPattern uExpressionPattern = UastPatterns.injectionHostUExpression(false);
    ElementPattern oneOf = StandardPatterns.string().oneOf(this.annotationAttributes);
    return uExpressionPattern.annotationParams(this.annotationClass, oneOf);
  }
}
