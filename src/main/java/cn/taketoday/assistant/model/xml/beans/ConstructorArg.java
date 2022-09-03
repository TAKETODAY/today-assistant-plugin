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
package cn.taketoday.assistant.model.xml.beans;

import com.intellij.psi.PsiType;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Referencing;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.converters.ConstructorArgIndexConverter;
import cn.taketoday.assistant.model.converters.ConstructorArgNameConverter;

@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
public interface ConstructorArg extends ConstructorArgDefinition, InfraInjection {

  @Stubbed

  @Attribute(value = "name")
  @Convert(ConstructorArgNameConverter.class)
  @NameValue
  GenericAttributeValue<String> getNameAttr();

  boolean isAssignable(PsiType to);

  /**
   * Returns the value of the index child.
   * <pre>
   * <h3>Attribute null:index documentation</h3>
   * 	The exact index of thr argument in the constructor argument list.
   * 	Only needed to avoid ambiguities, e.g. in case of 2 arguments of
   * 	the exact same type.
   *
   * </pre>
   *
   * @return the value of the index child.
   */
  @Stubbed
  @Referencing(ConstructorArgIndexConverter.class)
  GenericAttributeValue<Integer> getIndex();

  /**
   * Returns the value of the type child.
   * <pre>
   * <h3>Attribute null:type documentation</h3>
   * 	The exact type of the constructor argument. Only needed to avoid
   * 	ambiguities, e.g. in case of 2 single argument constructors
   * 	that can both be converted from a String.
   *
   * </pre>
   *
   * @return the value of the type child.
   */
  @Stubbed
  GenericAttributeValue<PsiType> getType();
}
