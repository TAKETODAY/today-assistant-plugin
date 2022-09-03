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
package cn.taketoday.assistant.model.xml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.assistant.model.converters.InfraBeanListConverter;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;

/**
 * Provides type checking for Spring bean DOM reference.
 * <p/>
 * Typical usage:
 * <pre>
 *   &#064;RequiredBeanType("com.expected.BeanType")
 *   &#064;Convert(SpringBeanResolveConverter.class)
 *   GenericAttributeValue<SpringBeanPointer<?>> getBeanRefAttribute();
 * </pre>
 * <p/>
 * Highlighting provided by {@link cn.taketoday.assistant.model.highlighting.dom.RequiredBeanTypeChecker}
 * which is called from dedicated inspection.
 * <p/>
 * NB: in 14, usage as <em>type</em> annotation has been replaced with
 * dedicated {@link BeanType}.
 *
 * @see InfraBeanResolveConverter
 * @see InfraBeanListConverter
 * @see InfraConverterUtil#getRequiredBeanTypeClasses(com.intellij.util.xml.ConvertContext)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiredBeanType {

  /**
   * Possible bean type(s) FQN (usually top-most interface).
   *
   * @return Required base class(es).
   */
  String[] value() default "UNDEFINED";
}
