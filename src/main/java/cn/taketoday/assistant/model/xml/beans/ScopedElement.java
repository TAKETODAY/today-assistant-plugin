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

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.model.converters.InfraBeanScopeConverter;
import cn.taketoday.assistant.model.scope.BeanScope;

public interface ScopedElement extends DomElement {
  /**
   * Returns the value of the scope child.
   * <pre>
   * <h3>Attribute null:scope documentation</h3>
   * 	The scope of this bean: typically "singleton" (one shared instance,
   * 	which will be returned by all calls to getBean() with the id),
   * 	or "prototype" (independent instance resulting from each call to
   * 	getBean(). Default is "singleton".
   * 	Singletons are most commonly used, and are ideal for multi-threaded
   * 	service objects. Further scopes, such as "request" or "session",
   * 	might be supported by extended bean factories (for example, in a
   * 	web environment).
   * 	Note: This attribute will not be inherited by child bean definitions.
   * 	Hence, it needs to be specified per concrete bean definition.
   * 	Inner bean definitions inherit the singleton status of their containing
   * 	bean definition, unless explicitly specified: The inner bean will be a
   * 	singleton if the containing bean is a singleton, and a prototype if
   * 	the containing bean has any other scope.
   *
   * </pre>
   *
   * @return the value of the scope child.
   */

  @Convert(InfraBeanScopeConverter.class)
  @Stubbed
  GenericAttributeValue<BeanScope> getScope();
}