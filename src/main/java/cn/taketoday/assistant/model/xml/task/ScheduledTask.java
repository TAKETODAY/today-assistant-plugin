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

package cn.taketoday.assistant.model.xml.task;

import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.RequiredBeanType;
import cn.taketoday.assistant.model.xml.task.converter.ScheduledTaskMethodConverter;

@Namespace(InfraConstant.TASK_NAMESPACE_KEY)
public interface ScheduledTask extends DomElement {

  GenericAttributeValue<String> getCron();

  GenericAttributeValue<Integer> getFixedDelay();

  GenericAttributeValue<Integer> getFixedRate();

  @Required
  @Convert(InfraBeanResolveConverter.class)
  GenericAttributeValue<BeanPointer<?>> getRef();

  @Required
  @Convert(ScheduledTaskMethodConverter.class)
  GenericAttributeValue<PsiMethod> getMethod();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.scheduling.Trigger" })
  GenericAttributeValue<BeanPointer<?>> getTrigger();
}
