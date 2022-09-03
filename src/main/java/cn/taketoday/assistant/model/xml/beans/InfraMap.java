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

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Stubbed;

import java.util.List;

import cn.taketoday.assistant.InfraConstant;

/**
 * http://www.springframework.org/schema/beans:mapType interface.
 */
@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
public interface InfraMap extends DomElement, TypedCollection, Description {

  /**
   * Returns the value of the key-type child.
   * <pre>
   * <h3>Attribute null:key-type documentation</h3>
   * 	The default Java type for nested entry keys. Must be a fully qualified
   * 	class name.
   *
   * </pre>
   *
   * @return the value of the key-type child.
   */

  @Stubbed
  GenericAttributeValue<PsiClass> getKeyType();

  /**
   * Returns the list of entry children.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:entry documentation</h3>
   * 	A map entry can be an inner bean, ref, value, or collection.
   * 	The key of the entry is given by the "key" attribute or child element.
   *
   * </pre>
   *
   * @return the list of entry children.
   */

  @Stubbed
  List<InfraEntry> getEntries();

  /**
   * Adds new child to the list of entry children.
   *
   * @return created child
   */
  InfraEntry addEntry();
}
