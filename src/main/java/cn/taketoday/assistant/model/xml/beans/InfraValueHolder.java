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

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.values.PropertyValueConverter;

/**
 * @author Dmitry Avdeev
 */
public interface InfraValueHolder extends InfraElementsHolder, InfraValueHolderDefinition {

  /**
   * Returns the value of the ref child.
   * <pre>
   * <h3>Attribute null:ref documentation</h3>
   * 	A short-cut alternative to a nested "<ref bean='...'/>".
   * <p/>
   * </pre>
   *
   * @return the value of the ref child.
   */

  @Attribute(value = "ref")
  @Convert(value = InfraBeanResolveConverter.PropertyBean.class)
  @Stubbed
  GenericAttributeValue<BeanPointer<?>> getRefAttr();

  /**
   * Returns the value of the value child.
   * <pre>
   * <h3>Attribute null:value documentation</h3>
   * 	A short-cut alternative to a nested "<value>...</value>"
   * 	element.
   * <p/>
   * </pre>
   *
   * @return the value of the value child.
   */

  @Attribute(value = "value")
  @Convert(PropertyValueConverter.class)
  @Stubbed
  GenericAttributeValue<String> getValueAttr();
}
