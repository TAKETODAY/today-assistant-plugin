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

package cn.taketoday.assistant.model.values.converters;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.util.PropertyPath;

public class BeanNameValueConverter extends InfraBeanResolveConverter {

  public static class BeanNameValueConverterCondition implements Condition<Pair<PsiType, GenericDomValue>> {
    private static final String[] CLASSES = { "cn.taketoday.aop.config.MethodLocatingFactoryBean", "cn.taketoday.aop.scope.ScopedProxyFactoryBean", "cn.taketoday.aop.target.AbstractBeanFactoryBasedTargetSource", "cn.taketoday.beans.factory.config.BeanReferenceFactoryBean", "cn.taketoday.beans.factory.config.ObjectFactoryCreatingFactoryBean", PropertyPath.CLASS_NAME, "cn.taketoday.scheduling.quartz.MethodInvokingJobDetailFactoryBean", "cn.taketoday.web.filter.DelegatingFilterProxy" };

    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      return InfraPropertyUtils.isSpecificProperty(pair.getSecond(), "targetBeanName", CLASSES)
              || InfraPropertyUtils.isSpecificProperty(pair.getSecond(), "targetName", "cn.taketoday.aop.framework.ProxyFactoryBean");
    }
  }
}
