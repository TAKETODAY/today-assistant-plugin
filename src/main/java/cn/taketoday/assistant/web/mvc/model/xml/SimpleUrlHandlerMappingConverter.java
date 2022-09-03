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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.converters.InfraBeanResolveConverterForDefiniteClasses;
import cn.taketoday.assistant.model.values.InfraValueConvertersProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.Prop;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public class SimpleUrlHandlerMappingConverter extends InfraBeanResolveConverterForDefiniteClasses implements InfraValueConvertersProvider, Condition<Pair<PsiType, GenericDomValue>> {
  protected String[] getClassNames(ConvertContext context) {
    return new String[] { InfraMvcConstant.SERVLET_MVC_CONTROLLER };
  }

  public Converter getConverter() {
    return this;
  }

  public Condition<Pair<PsiType, GenericDomValue>> getCondition() {
    return this;
  }

  public boolean value(Pair<PsiType, GenericDomValue> pair) {
    Prop prop;
    InfraProperty springProperty;
    DomInfraBean infraBean;
    PsiClass psiClass;
    GenericDomValue genericDomValue = pair.getSecond();
    return (!(genericDomValue instanceof GenericAttributeValue)) && (prop = DomUtil.getParentOfType(genericDomValue, Prop.class,
            false)) != null && (springProperty = DomUtil.getParentOfType(prop, InfraProperty.class, false)) != null && "mappings".equals(
            springProperty.getName().getStringValue()) && (infraBean = DomUtil.getParentOfType(springProperty, DomInfraBean.class,
            false)) != null && (psiClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType())) != null && InfraMvcConstant.SIMPLE_URL_HANDLER_MAPPING.equals(
            psiClass.getQualifiedName());
  }
}
