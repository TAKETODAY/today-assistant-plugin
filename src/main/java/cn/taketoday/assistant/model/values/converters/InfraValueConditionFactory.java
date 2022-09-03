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
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;

public final class InfraValueConditionFactory {

  public static Condition<GenericDomValue> createPropertyNameCondition(String propertyName) {
    return genericDomValue -> checkPropertyName(genericDomValue, propertyName);
  }

  public static Condition<GenericDomValue> createBeanPropertyCondition(
          String beanClass, String... propertyNames) {
    return genericDomValue -> {
      for (String propertyName : propertyNames) {
        if (checkPropertyName(genericDomValue, propertyName)) {
          return checkBeanClass(genericDomValue, beanClass);
        }
      }
      return false;
    };
  }

  public static Condition<GenericDomValue> createBeanClassCondition(String beanClass) {
    return genericDomValue -> checkBeanClass(genericDomValue, beanClass);
  }

  public static Condition<GenericDomValue> createBeanClassConstructorArgCondition(String beanClass) {
    return genericDomValue -> isConstructorArg(genericDomValue) && checkBeanClass(genericDomValue, beanClass);
  }

  private static boolean isConstructorArg(GenericDomValue genericDomValue) {
    return genericDomValue.getParentOfType(ConstructorArg.class, false) != null;
  }

  private static boolean checkBeanClass(DomElement element, String beanClassName) {
    DomInfraBean bean = InfraConverterUtil.getCurrentBean(element);
    if (bean == null)
      return false;

    PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
    return InheritanceUtil.isInheritor(beanClass, beanClassName);
  }

  private static boolean checkPropertyName(DomElement element, String propertyName) {
    InfraProperty property = element.getParentOfType(InfraProperty.class, false);
    return property != null && propertyName.equals(property.getName().getStringValue());
  }
}
