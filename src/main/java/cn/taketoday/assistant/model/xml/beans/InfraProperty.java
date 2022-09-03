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

import com.intellij.ide.presentation.Presentation;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Stubbed;

import java.util.List;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.converters.BeanPropertyConverter;

import static cn.taketoday.assistant.PresentationConstant.SPRING_PROPERTY;

@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
@Presentation(typeName = SPRING_PROPERTY, icon = "cn.taketoday.assistant.Icons.SpringProperty")
public interface InfraProperty extends InfraInjection, InfraPropertyDefinition {

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * 	The name of the property, following JavaBean naming conventions.
   * <p/>
   * </pre>
   *
   * @return the value of the name child.
   */
  @Required
  @Convert(BeanPropertyConverter.class)
  @NameValue(referencable = false)
  @Stubbed
  GenericAttributeValue<List<BeanProperty>> getName();
}
