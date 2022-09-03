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

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.scope.processor.MethodResolveProcessor;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import static com.intellij.codeInsight.daemon.impl.quickfix.CreateMethodFixKt.createVoidMethodFixes;

public abstract class InfraBeanMethodConverter extends PsiMethodConverter {

  @Override
  protected MethodAccepter getMethodAccepter(final ConvertContext context, final boolean forCompletion) {
    return new MethodAccepter() {

      @Override
      public boolean accept(PsiMethod method) {
        if (method.isConstructor())
          return false;

        final PsiClass containingClass = method.getContainingClass();
        if (containingClass == null)
          return false;
        final String containing = containingClass.getQualifiedName();
        if (CommonClassNames.JAVA_LANG_OBJECT.equals(containing))
          return false;

        return checkParameterList(method) && checkModifiers(method) && checkReturnType(context, method, forCompletion);
      }
    };
  }

  protected boolean checkModifiers(final PsiMethod method) {
    return method.hasModifierProperty(PsiModifier.PUBLIC) && !method.hasModifierProperty(PsiModifier.ABSTRACT);
  }

  protected boolean checkParameterList(final PsiMethod method) {
    return method.getParameterList().getParametersCount() == 0;
  }

  protected boolean checkReturnType(final ConvertContext context, final PsiMethod method, final boolean forCompletion) {
    return true;
  }

  @Override
  public LocalQuickFix[] getQuickFixes(final ConvertContext context) {
    final GenericDomValue element = (GenericDomValue) context.getInvocationElement();

    final String elementName = element.getStringValue();
    final PsiClass beanClass = getPsiClass(context);
    if (elementName != null && elementName.length() > 0 && beanClass != null) {
      return createVoidMethodFixes(beanClass, elementName, JvmModifier.PRIVATE);
    }

    return LocalQuickFix.EMPTY_ARRAY;
  }

  @Override
  protected String getMethodIdentificator(PsiMethod method) {
    return method.getName();
  }

  @Override
  protected PsiMethod[] getMethodCandidates(String methodIdentificator, PsiClass psiClass) {
    return MethodResolveProcessor.findMethod(psiClass, methodIdentificator);
  }
}
