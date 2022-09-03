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

import cn.taketoday.assistant.model.converters.InfraBeanDestroyMethodConverter;
import cn.taketoday.assistant.model.converters.InfraBeanInitMethodConverter;

public interface LifecycleBean extends DomElement {

  /**
   * Returns the value of the init-method child.
   * <pre>
   * <h3>Attribute null:init-method documentation</h3>
   * 	The name of the custom initialization method to invoke after setting
   * 	bean properties. The method must have no arguments, but may throw any
   * 	exception.
   * <p/>
   * </pre>
   *
   * @return the value of the init-method child.
   */

  @Convert(value = InfraBeanInitMethodConverter.class)
  @Stubbed
  GenericAttributeValue<PsiMethod> getInitMethod();

  /**
   * Returns the value of the destroy-method child.
   * <pre>
   * <h3>Attribute null:destroy-method documentation</h3>
   * 	The name of the custom destroy method to invoke on bean factory
   * 	shutdown. The method must have no arguments, but may throw any
   * 	exception.
   * 	Note: Only invoked on beans whose lifecycle is under the full
   * 	control of the factory - which is always the case for singletons,
   * 	but not guaranteed for any other scope.
   * <p/>
   * </pre>
   *
   * @return the value of the destroy-method child.
   */

  @Convert(value = InfraBeanDestroyMethodConverter.class)
  @Stubbed
  GenericAttributeValue<PsiMethod> getDestroyMethod();

}
