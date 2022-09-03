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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.reflect.DomAttributeChildDescription;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinitionBase;
import cn.taketoday.assistant.model.xml.beans.PNamespaceValue;
import cn.taketoday.lang.Nullable;

public abstract class PNamespaceValueImpl extends InfraValueHolderDefinitionBase implements PNamespaceValue {

  @Override

  public List<PsiType> getRequiredTypes() {
    return getPropertyType(this, getPropertyName());
  }

  public static List<PsiType> getPropertyType(DomElement value, String name) {
    InfraBean bean = (InfraBean) value.getParent();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
    if (beanClass == null) {
      return Collections.emptyList();
    }
    List<PsiMethod> methods = PropertyUtilBase.getSetters(beanClass, name);
    return ContainerUtil.map2List(methods, PropertyUtilBase::getPropertyType);
  }

  @Override
  public String getPropertyName() {
    return getXmlElementName();
  }

  @Override
  @Nullable
  public PsiType guessTypeByValue() {
    return null;
  }

  @Override
  @Nullable
  public GenericDomValue<BeanPointer<?>> getRefElement() {
    DomAttributeChildDescription description = getParent().getGenericInfo().getAttributeChildDescription(getPropertyName() + "-ref");
    if (description == null) {
      return null;
    }
    return description.getDomAttributeValue(getParent());
  }

  @Override
  @Nullable
  public GenericDomValue<?> getValueElement() {
    return this;
  }
}
