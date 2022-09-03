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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.util.xml.ConvertContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class ConstructorArgNameConverterImpl extends ConstructorArgNameConverter {

  public PsiParameter fromString(@Nullable String s, ConvertContext context) {
    InfraBean bean;
    if (StringUtil.isEmpty(s) || (bean = (InfraBean) InfraConverterUtil.getCurrentBean(context)) == null) {
      return null;
    }
    ResolvedConstructorArgs resolvedArgs = bean.getResolvedConstructorArgs();
    PsiMethod resolvedMethod = resolvedArgs.getResolvedMethod();
    if (resolvedMethod != null) {
      ConstructorArg constructorArg = context.getInvocationElement().getParentOfType(ConstructorArg.class, false);
      Map<ConstructorArgDefinition, PsiParameter> args = resolvedArgs.getResolvedArgs(resolvedMethod);
      return args.get(constructorArg);
    }
    List<PsiMethod> checkedMethods = resolvedArgs.getCheckedMethods();
    for (PsiMethod method : checkedMethods) {
      for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
        if (s.equals(psiParameter.getName())) {
          return psiParameter;
        }
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable PsiParameter beanProperty, ConvertContext context) {
    return null;
  }

  public Collection<PsiParameter> getVariants(ConvertContext context) {
    return getAllConstructorParams(context);
  }

  private static Collection<PsiParameter> getAllConstructorParams(ConvertContext context) {
    Map<String, PsiParameter> params = new HashMap<>();
    InfraBean springBean = context.getInvocationElement().getParentOfType(InfraBean.class, false);
    if (springBean != null) {
      List<PsiMethod> psiMethods = springBean.getInstantiationMethods();
      for (PsiMethod psiMethod : psiMethods) {
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
          params.put(parameter.getName(), parameter);
        }
      }
    }
    return params.values();
  }

  public LookupElement createLookupElement(PsiParameter psiParameter) {
    return LookupElementBuilder.create(psiParameter).withIcon(AllIcons.Nodes.Parameter).withTypeText(psiParameter.getType().getPresentableText());
  }
}
