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

import com.intellij.psi.PsiType;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.reflect.DomAttributeChildDescription;

import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinitionBase;
import cn.taketoday.assistant.model.xml.beans.PNamespaceRefValue;
import cn.taketoday.lang.Nullable;

public abstract class PNamespaceRefValueImpl extends InfraValueHolderDefinitionBase implements PNamespaceRefValue {

  @Override
  public List<PsiType> getRequiredTypes() {
    return PNamespaceValueImpl.getPropertyType(this, getPropertyName());
  }

  @Override

  public String getPropertyName() {
    String name = getXmlElementName();
    return name.substring(0, name.length() - "-ref".length());
  }

  @Override
  @Nullable
  public PsiType guessTypeByValue() {
    return null;
  }

  @Override
  @Nullable
  public GenericDomValue<BeanPointer<?>> getRefElement() {
    return this;
  }

  @Override
  @Nullable
  public GenericDomValue<?> getValueElement() {
    DomAttributeChildDescription description = getParent().getGenericInfo().getAttributeChildDescription(getPropertyName());
    if (description == null) {
      return null;
    }
    return description.getDomAttributeValue(getParent());
  }
}
