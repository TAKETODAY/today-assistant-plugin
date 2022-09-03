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

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.XmlDomBundle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class ConstructorArgIndexConverterImpl extends ConstructorArgIndexConverter {

  public PsiReference[] createReferences(GenericDomValue<Integer> index, PsiElement element, ConvertContext context) {
    return new PsiReference[] { new MyReference(element, index, context) };
  }

  private static class MyReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {
    private final GenericDomValue<Integer> myGenericDomValue;
    private final ConvertContext myContext;

    MyReference(PsiElement element, GenericDomValue<Integer> index, ConvertContext context) {
      super(element);
      this.myGenericDomValue = index;
      this.myContext = context;
    }

    public PsiParameter resolve() {
      InfraBean bean = (InfraBean) InfraConverterUtil.getCurrentBean(this.myContext);
      if (bean == null) {
        return null;
      }
      return ConstructorArgIndexConverterImpl.resolve(this.myGenericDomValue, bean);
    }

    public boolean isSoft() {
      return true;
    }

    public Object[] getVariants() {
      InfraBean bean = (InfraBean) InfraConverterUtil.getCurrentBean(this.myContext);
      if (bean == null) {
        return EMPTY_ARRAY;
      }
      List<PsiMethod> psiMethods = bean.getInstantiationMethods();
      int maxParams = 0;
      for (PsiMethod method : psiMethods) {
        PsiParameterList parameterList = method.getParameterList();
        maxParams = Math.max(maxParams, parameterList.getParametersCount());
      }
      if (maxParams > 0) {
        Object[] objects = new Object[maxParams];
        for (int i = 0; i < maxParams; i++) {
          objects[i] = Integer.toString(i);
        }
        return objects;
      }
      return EMPTY_ARRAY;
    }

    public String getUnresolvedMessagePattern() {
      String message;
      Integer value = this.myGenericDomValue.getValue();
      if (value != null) {
        InfraBean bean = (InfraBean) InfraConverterUtil.getCurrentBean(this.myContext);
        assert bean != null;
        PsiClass clazz = bean.getInstantiationClass();
        if (clazz != null) {
          if (bean.getFactoryMethod().getValue() != null) {
            message = message("cannot.find.factory.method.index", value, clazz.getQualifiedName());
          }
          else {
            message = message("cannot.find.constructor.arg.index.in.class", value, clazz.getQualifiedName());
          }
          return message;
        }
        return message("cannot.find.constructor.arg.index", value);
      }
      return XmlDomBundle.message("dom.converter.value.should.be.integer");
    }
  }

  @Nullable
  public static PsiParameter resolve(GenericDomValue<Integer> i, InfraBean bean) {
    int index;
    Integer value = i.getValue();
    if (value != null && (index = value) >= 0) {
      ResolvedConstructorArgs resolvedArgs = bean.getResolvedConstructorArgs();
      PsiMethod resolvedMethod = resolvedArgs.getResolvedMethod();
      if (resolvedMethod != null) {
        return Objects.requireNonNull(resolvedArgs.getResolvedArgs(resolvedMethod)).get(i.getParent());
      }
      List<PsiMethod> checkedMethods = resolvedArgs.getCheckedMethods();
      for (PsiMethod method : checkedMethods) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() > index) {
          return parameterList.getParameters()[index];
        }
      }
      return null;
    }
    return null;
  }

  public static Set<PsiParameter> multiResolve(GenericDomValue<Integer> i, InfraBean bean) {
    int index;
    Set<PsiParameter> set = new LinkedHashSet<>();
    Integer value = i.getValue();
    if (value != null && (index = value) >= 0) {
      ResolvedConstructorArgs resolvedArgs = bean.getResolvedConstructorArgs();
      List<PsiMethod> candidates = resolvedArgs.getCandidates();
      for (PsiMethod method : candidates) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() > index) {
          set.add(parameterList.getParameters()[index]);
        }
      }
    }
    return set;
  }
}
