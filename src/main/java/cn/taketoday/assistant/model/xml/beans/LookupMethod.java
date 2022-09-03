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

// Generated on Thu Nov 09 17:15:14 MSK 2006
// DTD/Schema  :    http://www.springframework.org/schema/beans

package cn.taketoday.assistant.model.xml.beans;

import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanLookupMethodConverter;
import cn.taketoday.assistant.model.converters.LookupMethodBeanConverter;

public interface LookupMethod extends DomElement {

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * 	The name of the lookup method. This method must take no arguments.
   *
   * </pre>
   *
   * @return the value of the name child.
   */
  @Convert(value = InfraBeanLookupMethodConverter.class)
  @Stubbed
  GenericAttributeValue<PsiMethod> getName();

  /**
   * Returns the value of the bean child.
   * <pre>
   * <h3>Attribute null:bean documentation</h3>
   * 	The name of the bean in the current or ancestor factories that
   * 	the lookup method should resolve to. Often this bean will be a
   * 	prototype, in which case the lookup method will return a distinct
   * 	instance on every invocation. This is useful for single-threaded objects.
   *
   * </pre>
   *
   * @return the value of the bean child.
   */
  @Convert(value = LookupMethodBeanConverter.class)
  GenericAttributeValue<BeanPointer<?>> getBean();
}
