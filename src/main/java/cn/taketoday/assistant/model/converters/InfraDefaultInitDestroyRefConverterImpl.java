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

package cn.taketoday.assistant.model.converters;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class InfraDefaultInitDestroyRefConverterImpl extends InfraDefaultInitDestroyRefConverter {

  public PsiReference[] createReferences(GenericDomValue<String> genericDomValue, PsiElement element, ConvertContext context) {
    return new PsiReference[] { new PsiMethodPolyVariantReference(element, genericDomValue) };
  }

  public Set<PsiMethod> fromString(@Nullable String s, ConvertContext context) {
    return getMethodsByName(DomUtil.getParentOfType(context.getInvocationElement(), Beans.class, false), s);
  }

  public String toString(@Nullable Set<PsiMethod> psiMethods, ConvertContext context) {
    return "";
  }

  private static final class PsiMethodPolyVariantReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
    private final GenericDomValue<String> myGenericDomValue;

    private PsiMethodPolyVariantReference(PsiElement element, GenericDomValue<String> genericDomValue) {
      super(element);
      this.myGenericDomValue = genericDomValue;
    }

    public PsiElement resolve() {
      ResolveResult[] resolveResults = multiResolve(false);
      if (resolveResults.length == 1) {
        return resolveResults[0].getElement();
      }
      return null;
    }

    public boolean isSoft() {
      return true;
    }

    public Object[] getVariants() {
      return getAllMethods(DomUtil.getParentOfType(this.myGenericDomValue, Beans.class, false)).toArray();
    }

    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
      return element;
    }

    public ResolveResult[] multiResolve(boolean incompleteCode) {
      Set<PsiMethod> methodsByName = getMethodsByName(
              DomUtil.getParentOfType(this.myGenericDomValue, Beans.class, false), myGenericDomValue.getStringValue());
      return ContainerUtil.map2Array(methodsByName, ResolveResult.class, PsiElementResolveResult::new);
    }

    private static Set<PsiMethod> getAllMethods(@Nullable Beans beans) {
      return getMethods(beans, psiClass -> Arrays.asList(psiClass.getAllMethods()));
    }

  }

  private static Set<PsiMethod> getMethods(@Nullable Beans beans, Function<PsiClass, Collection<PsiMethod>> fun) {
    Set<PsiMethod> methods = new HashSet<>();
    if (beans != null) {
      for (InfraBean infraBean : beans.getBeans()) {
        PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
        if (beanClass != null) {
          methods.addAll(fun.fun(beanClass));
        }
      }
    }
    return methods;
  }

  private static Set<PsiMethod> getMethodsByName(@Nullable Beans beans, @Nullable String methodName) {
    if (!StringUtil.isEmptyOrSpaces(methodName)) {
      return getMethods(beans, psiClass -> Arrays.asList(psiClass.findMethodsByName(methodName, true)));
    }
    return Collections.emptySet();
  }
}
