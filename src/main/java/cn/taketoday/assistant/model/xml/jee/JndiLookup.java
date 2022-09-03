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

package cn.taketoday.assistant.model.xml.jee;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.BeanTypeProvider;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.lang.Nullable;

@BeanType(provider = JndiLookup.JndiLookupBeanTypeProvider.class)
public interface JndiLookup extends DomInfraBean, JndiLocated {

  GenericAttributeValue<Boolean> getCache();

  GenericAttributeValue<PsiClass> getExpectedType();

  GenericAttributeValue<Boolean> getLookupOnStartup();

  GenericAttributeValue<PsiClass> getProxyInterface();

  @Convert(InfraBeanResolveConverter.class)
  GenericAttributeValue<BeanPointer<?>> getDefaultRef();

  @Required(false)
  GenericAttributeValue<String> getDefaultValue();

  class JndiLookupBeanTypeProvider implements BeanTypeProvider<JndiLookup> {

    @Override
    public String[] getBeanTypeCandidates() {
      return new String[] { InfraConstant.JNDI_OBJECT_FACTORY_BEAN };
    }

    @Override
    @Nullable
    public String getBeanType(JndiLookup lookup) {
      String stringValue = lookup.getExpectedType().getStringValue();
      if (!StringUtil.isEmptyOrSpaces(stringValue)) {
        return stringValue;
      }
      return InfraConstant.JNDI_OBJECT_FACTORY_BEAN;
    }
  }
}
