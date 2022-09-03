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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.jam.JamService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveHandlerMethod;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.lang.Nullable;

class LiveHandlerMethodImpl implements LiveHandlerMethod {
  static final String LAMBDA = "$$Lambda";
  private static final Set<String> MODIFIERS = ContainerUtil.immutableSet(PsiModifier.MODIFIERS);

  private final String myRawMethod;
  private String myClassName;
  private int myNameIndex;
  private String myMethodName;
  private final List<String> myParameters;

  LiveHandlerMethodImpl(String rawMethod) {
    this.myClassName = "";
    this.myNameIndex = -1;
    this.myMethodName = "";
    this.myParameters = new SmartList();
    this.myRawMethod = rawMethod;
    parseRawMethod(rawMethod);
  }

  private void parseRawMethod(String rawMethod) {
    int lambda = rawMethod.indexOf(LAMBDA);
    if (lambda > 0) {
      parseClassName(rawMethod.substring(0, lambda));
      this.myMethodName = LAMBDA;
      return;
    }
    String[] parts = rawMethod.replaceAll(",\\s", ",").split("\\s");
    int modifiersEnd = 0;
    while (modifiersEnd < parts.length && isModifier(parts[modifiersEnd])) {
      modifiersEnd++;
    }
    String rawMethod2 = parts[parts.length == modifiersEnd + 1 ? modifiersEnd : modifiersEnd + 1];
    int nameEnd = rawMethod2.indexOf(40);
    if (nameEnd < 0) {
      return;
    }
    int nameStart = rawMethod2.lastIndexOf(46, nameEnd);
    if (nameStart >= 0) {
      parseClassName(rawMethod2.substring(0, nameStart));
      this.myMethodName = rawMethod2.substring(nameStart + 1, nameEnd);
    }
    int methodEnd = rawMethod2.lastIndexOf(41);
    if (methodEnd <= nameEnd) {
      return;
    }
    String parametersString = rawMethod2.substring(nameEnd + 1, methodEnd);
    if (parametersString.isEmpty()) {
      return;
    }
    List<String> parameters = StringUtil.split(parametersString, ",");
    String previous = "";
    for (String parameter : parameters) {
      String parameterCandidate = previous.isEmpty() ? parameter : previous + "," + parameter;
      if (StringUtil.countChars(parameterCandidate, '<') == StringUtil.countChars(parameterCandidate, '>')) {
        this.myParameters.add(StringUtil.replace(parameterCandidate, "$", "."));
        previous = "";
      }
      else {
        previous = parameterCandidate;
      }
    }
  }

  private void parseClassName(String qualifiedName) {
    int nameIndex = qualifiedName.lastIndexOf(46);
    if (nameIndex >= 0 && nameIndex < qualifiedName.length() - 1) {
      this.myNameIndex = nameIndex + 1;
    }
    this.myClassName = StringUtil.replace(qualifiedName, "$", ".");
  }

  @Override
  public String getRawMethod() {
    return this.myRawMethod;
  }

  @Override

  public String getClassName() {
    return this.myClassName;
  }

  @Override

  public String getMethodName() {
    String str = this.myMethodName;
    return str;
  }

  @Override

  public List<String> getParameters() {
    List<String> unmodifiableList = Collections.unmodifiableList(this.myParameters);
    return unmodifiableList;
  }

  @Override

  public String getDisplayName() {
    if (this.myClassName.isEmpty() || this.myMethodName.isEmpty()) {
      String str = this.myRawMethod;
      return str;
    }
    String className = this.myNameIndex >= 0 ? this.myClassName.substring(this.myNameIndex) : this.myClassName;
    String str2 = className + (this.myMethodName.equals(LAMBDA) ? "" : "#") + this.myMethodName;
    return str2;
  }

  @Override
  @Nullable
  public PsiClass findContainingClass(Project project, GlobalSearchScope searchScope) {
    if (this.myClassName.isEmpty()) {
      return null;
    }
    return JavaExecutionUtil.findMainClass(project, this.myClassName, searchScope);
  }

  @Override
  @Nullable
  public PsiMethod findMethod(Project project, GlobalSearchScope searchScope) {
    PsiClass psiClass;
    if (!this.myMethodName.isEmpty() && (psiClass = findContainingClass(project, searchScope)) != null) {
      if (this.myMethodName.equals(LAMBDA)) {
        for (PsiMethod psiMethod : psiClass.getMethods()) {
          if (isRouterFunctionBean(psiMethod)) {
            return psiMethod;
          }
        }
      }
      PsiMethod[] methods2 = psiClass.findMethodsByName(this.myMethodName, false);
      for (PsiMethod method : methods2) {
        if (matchParameters(method)) {
          return method;
        }
      }
      return null;
    }
    return null;
  }

  @Override
  public boolean matches(PsiMethod psiMethod) {
    PsiClass psiClass;
    if (psiMethod.isValid() && (psiClass = psiMethod.getContainingClass()) != null && this.myClassName.equals(psiClass.getQualifiedName())) {
      if (this.myMethodName.equals(LAMBDA)) {
        return isRouterFunctionBean(psiMethod);
      }
      return this.myMethodName.equals(psiMethod.getName()) && matchParameters(psiMethod);
    }
    return false;
  }

  private boolean matchParameters(PsiMethod psiMethod) {
    PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
    if (parameters.length != this.myParameters.size()) {
      return false;
    }
    for (int i = 0; i < parameters.length; i++) {
      PsiParameter parameter = parameters[i];
      String liveParameter = this.myParameters.get(i);
      if (!isEquivalent(liveParameter, parameter.getType())) {
        return false;
      }
    }
    return true;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LiveHandlerMethodImpl) {
      return this.myRawMethod.equals(((LiveHandlerMethodImpl) obj).myRawMethod);
    }
    return false;
  }

  public int hashCode() {
    return this.myRawMethod.hashCode();
  }

  public String toString() {
    return this.myRawMethod;
  }

  private static boolean isModifier(String string) {
    return MODIFIERS.contains(string);
  }

  private static boolean isEquivalent(String type, PsiType psiType) {
    String canonicalText = psiType.getCanonicalText();
    if (type.equals(canonicalText)) {
      return true;
    }
    int genericStart = canonicalText.indexOf(60);
    return genericStart >= 0 && type.equals(canonicalText.substring(0, genericStart));
  }

  static boolean isRouterFunctionBean(PsiMethod psiMethod) {
    PsiType psiType = psiMethod.getReturnType();
    if (psiType == null) {
      return false;
    }
    return (isEquivalent("cn.taketoday.web.reactive.function.server.RouterFunction", psiType)
            || isEquivalent("cn.taketoday.web.servlet.function.RouterFunction", psiType))
            && JamService.getJamService(psiMethod.getProject()).getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiMethod) != null;
  }
}
