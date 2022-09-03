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

package cn.taketoday.assistant.model.converters.specific;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.factories.resolvers.MethodInvokingFactoryBeanTypeResolver;
import cn.taketoday.assistant.model.converters.InfraBeanMethodConverter;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class MethodInvokingJobDetailFactoryBeanConverter extends InfraBeanMethodConverter {
  @Override
  @Nullable
  public PsiClass getPsiClass(ConvertContext context) {
    InfraBean infraBean = (InfraBean) InfraConverterUtil.getCurrentBean(context);
    if (infraBean != null) {
      return MethodInvokingFactoryBeanTypeResolver.getMethodInvokingPsiClass(context.getSearchScope(), context.getProject(), infraBean);
    }
    return null;
  }

  public static class MethodInvokingJobDetailFactoryBeanCondition implements Condition<Pair<PsiType, GenericDomValue>> {
    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      return InfraPropertyUtils.isSpecificProperty(pair.getSecond(),
              "targetMethod", "cn.taketoday.scheduling.quartz.MethodInvokingJobDetailFactoryBean",
              "cn.taketoday.scheduling.timer.MethodInvokingTimerTaskFactoryBean", InfraConstant.METHOD_INVOKING_FACTORY_BEAN_CLASS);
    }
  }

  @Override
  protected boolean checkParameterList(PsiMethod method) {
    return true;
  }
}
