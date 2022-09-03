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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.ConvertContext;

import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.LifecycleBean;

public class InfraBeanDestroyMethodConverterImpl extends InfraBeanDestroyMethodConverter {
  @Override
  protected boolean checkParameterList(PsiMethod method) {
    PsiParameterList parameterList = method.getParameterList();
    return parameterList.getParametersCount() == 0 || (parameterList.getParametersCount() == 1 && PsiType.BOOLEAN.equals(parameterList.getParameters()[0].getType()));
  }

  @Override
  protected boolean checkModifiers(PsiMethod method) {
    return true;
  }

  @Override
  public PsiClass getPsiClass(ConvertContext context) {
    LifecycleBean springBean = context.getInvocationElement().getParentOfType(LifecycleBean.class, false);
    if (springBean instanceof DomInfraBean) {
      return PsiTypesUtil.getPsiClass(((DomInfraBean) springBean).getBeanType());
    }
    return null;
  }
}
