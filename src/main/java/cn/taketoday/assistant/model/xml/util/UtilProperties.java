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

package cn.taketoday.assistant.model.xml.util;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Referencing;

import java.util.List;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.values.converters.resources.ResourceValueConverter;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Prop;
import cn.taketoday.assistant.model.xml.beans.ScopedElement;
import cn.taketoday.assistant.model.xml.beans.TypedCollection;

@BeanType(InfraConstant.PROPERTIES_FACTORY_BEAN)
public interface UtilProperties extends InfraUtilElement, DomInfraBean, TypedCollection, ScopedElement {
  @Referencing(ResourceValueConverter.class)
  GenericAttributeValue<String> getLocation();

  List<Prop> getProps();

  GenericAttributeValue<Boolean> getIgnoreResourceNotFound();

  GenericAttributeValue<Boolean> getLocalOverride();
}
