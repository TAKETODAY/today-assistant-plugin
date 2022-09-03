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
import com.intellij.psi.PsiType;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Stubbed;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.converters.KeyInfraBeanResolveConverter;
import cn.taketoday.assistant.model.values.EntryKeyConverter;
import cn.taketoday.lang.Nullable;

/**
 * http://www.springframework.org/schema/beans:entryType interface.
 */
public interface InfraEntry extends InfraValueHolder {

  @Nullable
  PsiClass getRequiredKeyClass();

  @Nullable
  PsiType getRequiredKeyType();

  @Nullable
  PsiType getRequiredValueType();

  /**
   * Returns the value of the key child.
   * <pre>
   * <h3>Attribute null:key documentation</h3>
   * 	Each map element must specify its key as attribute or as child element.
   * 	A key attribute is always a String value.
   *
   * </pre>
   *
   * @return the value of the key child.
   */

  @Attribute(value = "key")
  @Convert(EntryKeyConverter.class)
  GenericAttributeValue<String> getKeyAttr();

  /**
   * Returns the value of the key-ref child.
   * <pre>
   * <h3>Attribute null:key-ref documentation</h3>
   * 	A short-cut alternative to a to a "key" element with a nested
   * 	"<ref bean='...'/>".
   *
   * </pre>
   *
   * @return the value of the key-ref child.
   */
  @Convert(value = KeyInfraBeanResolveConverter.class)
  GenericAttributeValue<BeanPointer<?>> getKeyRef();

  /**
   * Returns the value of the value-ref child.
   * <pre>
   * <h3>Attribute null:value-ref documentation</h3>
   * 	A short-cut alternative to a nested "<ref bean='...'/>".
   *
   * </pre>
   *
   * @return the value of the value-ref child.
   */

  @Attribute(value = "value-ref")
  @Convert(value = InfraBeanResolveConverter.PropertyBean.class)
  GenericAttributeValue<BeanPointer<?>> getValueRef();

  // since 3.2

  @Stubbed
  GenericAttributeValue<PsiClass> getValueType();

  /**
   * Returns the value of the key child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:key documentation</h3>
   * 	A key element can contain an inner bean, ref, value, or collection.
   *
   * </pre>
   *
   * @return the value of the key child.
   */

  InfraKey getKey();
}
