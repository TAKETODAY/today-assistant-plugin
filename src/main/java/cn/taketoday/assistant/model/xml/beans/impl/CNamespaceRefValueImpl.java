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

package cn.taketoday.assistant.model.xml.beans.impl;

import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.dom.namespaces.AbstractBeanNamespaceDescriptor;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.CNamespaceRefValue;
import cn.taketoday.lang.Nullable;

public abstract class CNamespaceRefValueImpl extends CNamespaceValueBaseImpl implements CNamespaceRefValue {

  @Override
  public String getAttributeName() {
    return AbstractBeanNamespaceDescriptor.attributeNameToPropertyName(getXmlElementName());
  }

  @Override
  @Nullable
  public GenericDomValue<BeanPointer<?>> getRefElement() {
    return this;
  }

  @Override
  @Nullable
  public GenericDomValue<?> getValueElement() {
    return getParent().getGenericInfo().getAttributeChildDescription(getAttributeName()).getDomAttributeValue(getParent());
  }
}
