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

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.Namespace;

import java.util.List;

import cn.taketoday.assistant.InfraConstant;

/**
 * http://www.springframework.org/schema/beans:propsType interface.
 */
@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
public interface Props extends DomElement, BaseCollection {

  /**
   * Returns the list of prop children.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:prop documentation</h3>
   * 	The string value of the property. Note that whitespace is trimmed
   * 	off to avoid unwanted whitespace caused by typical XML formatting.
   *
   * </pre>
   *
   * @return the list of prop children.
   */

  List<Prop> getProps();

  /**
   * Adds new child to the list of prop children.
   *
   * @return created child
   */
  Prop addProp();
}
