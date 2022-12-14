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

// Generated on Wed Oct 17 15:28:10 MSD 2007
// DTD/Schema  :    http://www.springframework.org/schema/context

package cn.taketoday.assistant.model.xml.context;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.model.converters.InfraContextFilterExpressionConverter;
import cn.taketoday.assistant.model.xml.DomInfraBean;

public interface Filter extends DomInfraBean, InfraContextElement {

  @Required
  @Stubbed
  GenericAttributeValue<Type> getType();

  @Required
  @Convert(InfraContextFilterExpressionConverter.class)
  @Stubbed
  GenericAttributeValue<Object> getExpression();
}
