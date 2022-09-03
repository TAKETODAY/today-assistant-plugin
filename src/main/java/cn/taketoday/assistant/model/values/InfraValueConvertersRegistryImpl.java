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

package cn.taketoday.assistant.model.values;

import com.intellij.psi.PsiType;

import cn.taketoday.assistant.model.converters.specific.MethodInvokingJobDetailFactoryBeanConverter;
import cn.taketoday.assistant.model.values.converters.BeanNameValueConverter;
import cn.taketoday.assistant.model.values.converters.BundleNameConverter;
import cn.taketoday.assistant.model.values.converters.EnumValueConverter;
import cn.taketoday.assistant.model.values.converters.FieldRetrievingFactoryBeanConverterImpl;
import cn.taketoday.assistant.model.values.converters.InfraBooleanValueConverter;
import cn.taketoday.assistant.model.values.converters.resources.ResourceValueConverter;

public class InfraValueConvertersRegistryImpl extends InfraValueConvertersRegistry {
  @Override
  protected void registerBuiltinValueConverters() {
    registerConverter(new InfraBooleanValueConverter(false), PsiType.BOOLEAN);
    registerConverter(new InfraBooleanValueConverter(true), Boolean.class);
    registerNumberValueConverters();
    registerCharacterConverter();
    registerClassValueConverters();
    registerConverter(new ResourceValueConverter(), new ResourceValueConverter.ResourceValueConverterCondition());
    registerConverter(new FieldRetrievingFactoryBeanConverterImpl(), new FieldRetrievingFactoryBeanConverterImpl.FactoryClassAndPropertyCondition());
    registerConverter(new EnumValueConverter(), new EnumValueConverter.TypeCondition());
    registerConverter(new MethodInvokingJobDetailFactoryBeanConverter(), new MethodInvokingJobDetailFactoryBeanConverter.MethodInvokingJobDetailFactoryBeanCondition());
    registerConverter(new BeanNameValueConverter(), new BeanNameValueConverter.BeanNameValueConverterCondition());
    registerConverter(new BundleNameConverter(), new BundleNameConverter.BundleNameConverterCondition());
  }
}
