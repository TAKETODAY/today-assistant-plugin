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
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.converters.PropertyLocalInfraBeanResolveConverter;

/**
 * @author Dmitry Avdeev
 */
@Namespace(InfraConstant.BEANS_NAMESPACE_KEY)
public interface RefBase extends DomElement {
  /**
   * Returns the value of the bean child.
   * <pre>
   * <h3>Attribute null:bean documentation</h3>
   * 	The name of the referenced bean.
   *
   * </pre>
   *
   * @return the value of the bean child.
   */

  @Convert(value = InfraBeanResolveConverter.PropertyBean.class)
  @Stubbed
  GenericAttributeValue<BeanPointer<?>> getBean();

  /**
   * Returns the value of the local child.
   * <pre>
   * <h3>Attribute null:local documentation</h3>
   * 	The name of the referenced bean. The value must be a bean ID,
   * 	and thus can be checked by the XML parser, thus should be preferred
   * 	for references within the same bean factory XML file.
   *
   * </pre>
   *
   * @return the value of the local child.
   */

  @Convert(value = PropertyLocalInfraBeanResolveConverter.class)
  @Stubbed
  GenericAttributeValue<BeanPointer<?>> getLocal();
}
