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

import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanReplacedMethodConverter;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.RequiredBeanType;

/**
 * http://www.springframework.org/schema/beans:replaced-methodElemType interface.
 */
public interface ReplacedMethod extends DomElement {

  String METHOD_REPLACER_CLASS = "cn.taketoday.beans.factory.support.MethodReplacer";

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * 	The name of the method whose implementation must be replaced by the
   * 	IoC container. If this method is not overloaded, there is no need
   * 	to use arg-type subelements. If this method is overloaded, arg-type
   * 	subelements must be used for all override definitions for the method.
   *
   * </pre>
   *
   * @return the value of the name child.
   */

  @Convert(value = InfraBeanReplacedMethodConverter.class)
  GenericAttributeValue<PsiMethod> getName();

  /**
   * Returns the value of the replacer child.
   * <pre>
   * <h3>Attribute null:replacer documentation</h3>
   * 	Bean name of an implementation of the MethodReplacer interface in the
   * 	current or ancestor factories. This may be a singleton or prototype
   * 	bean. If it is a prototype, a new instance will be used for each
   * 	method replacement. Singleton usage is the norm.
   *
   * </pre>
   *
   * @return the value of the replacer child.
   */

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType(METHOD_REPLACER_CLASS)
  GenericAttributeValue<BeanPointer<?>> getReplacer();

  /**
   * Returns the list of arg-type children.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:arg-type documentation</h3>
   * 	Identifies an argument for a replaced method in the event of
   * 	method overloading.
   *
   * </pre>
   *
   * @return the list of arg-type children.
   */

  List<ArgType> getArgTypes();

  /**
   * Adds new child to the list of arg-type children.
   *
   * @return created child
   */
  ArgType addArgType();
}
