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

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.BeanType;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.RequiredBeanType;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

@BeanType("cn.taketoday.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter")
@Namespace(InfraMvcConstant.MVC_NAMESPACE_KEY)
public interface AnnotationDriven extends DomInfraBean {

  MessageConverters getMessageConverters();

  ArgumentResolvers getArgumentResolvers();

  ReturnValueHandlers getReturnValueHandlers();

  AsyncSupport getAsyncSupport();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.core.convert.ConversionService" })
  GenericAttributeValue<BeanPointer<?>> getConversionService();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.validation.Validator" })
  GenericAttributeValue<BeanPointer<?>> getValidator();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.validation.MessageCodesResolver" })
  GenericAttributeValue<BeanPointer<?>> getMessageCodesResolver();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.web.accept.ContentNegotiationManager" })
  GenericAttributeValue<BeanPointer<?>> getContentNegotiationManager();

  @Attribute("ignoreDefaultModelOnRedirect")
  GenericAttributeValue<Boolean> getIgnoreDefaultModelOnRedirect_3();

  GenericAttributeValue<Boolean> getIgnoreDefaultModelOnRedirect();

  @Attribute("enableMatrixVariables")
  GenericAttributeValue<Boolean> getEnableMatrixVariables_3();

  GenericAttributeValue<Boolean> getEnableMatrixVariables();

  PathMatching getPathMatching();
}
