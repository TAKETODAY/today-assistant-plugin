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

import com.intellij.codeInsight.daemon.impl.quickfix.CreateMethodQuickFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.LookupMethod;
import cn.taketoday.lang.Nullable;

public class InfraBeanLookupMethodConverterImpl extends InfraBeanLookupMethodConverter {

  @Override
  @Nullable
  public PsiClass getPsiClass(ConvertContext context) {
    InfraBean springBean = context.getInvocationElement().getParentOfType(InfraBean.class, false);
    if (springBean != null) {
      return PsiTypesUtil.getPsiClass(springBean.getBeanType());
    }
    return null;
  }

  @Override
  protected boolean checkModifiers(PsiMethod method) {
    return method.hasModifierProperty("public") || method.hasModifierProperty("protected");
  }

  @Override
  public boolean checkReturnType(ConvertContext context, PsiMethod method, boolean forCompletion) {
    PsiType returnType = method.getReturnType();
    if (PsiType.VOID.equals(returnType) || (returnType instanceof PsiPrimitiveType)) {
      return false;
    }
    if (forCompletion) {
      PsiType[] possibleReturnTypes = getValidReturnTypes(context);
      if (possibleReturnTypes.length > 0 && returnType != null) {
        for (PsiType possibleReturnType : possibleReturnTypes) {
          if (possibleReturnType.isAssignableFrom(returnType)) {
            return true;
          }
        }
        return false;
      }
    }
    return super.checkReturnType(context, method, forCompletion);
  }

  @Override
  public LocalQuickFix[] getQuickFixes(ConvertContext context) {
    DomInfraBean springBean;
    PsiType[] validReturnTypes = getValidReturnTypes(context);
    if (validReturnTypes.length != 0 && (springBean = InfraConverterUtil.getCurrentBean(context)) != null) {
      GenericDomValue element = (GenericDomValue) context.getInvocationElement();
      PsiClass psiClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
      String elementName = element.getStringValue();
      if (psiClass == null || elementName == null || StringUtil.isEmpty(elementName)) {
        return LocalQuickFix.EMPTY_ARRAY;
      }
      List<LocalQuickFix> fixes = new ArrayList<>();
      for (PsiType returnType : validReturnTypes) {
        CreateMethodQuickFix fix = CreateMethodQuickFix.createFix(psiClass, getNewMethodSignature(elementName, returnType), getNewMethodBody());
        if (fix != null) {
          fixes.add(fix);
        }
      }
      return fixes.toArray(LocalQuickFix.EMPTY_ARRAY);
    }
    return LocalQuickFix.EMPTY_ARRAY;
  }

  private static String getNewMethodBody() {
    return "return null;";
  }

  private static String getNewMethodSignature(String elementName, PsiType psiType) {
    return "public " + psiType.getCanonicalText() + " " + elementName + "()";
  }

  private static PsiType[] getValidReturnTypes(ConvertContext context) {
    BeanPointer<?> beanPointer;
    LookupMethod lookupMethod = context.getInvocationElement().getParentOfType(LookupMethod.class, false);
    if (lookupMethod != null && (beanPointer = lookupMethod.getBean().getValue()) != null) {
      return beanPointer.getEffectiveBeanTypes();
    }
    return PsiType.EMPTY_ARRAY;
  }
}
