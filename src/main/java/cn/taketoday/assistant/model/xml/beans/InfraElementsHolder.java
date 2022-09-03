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
import com.intellij.util.xml.CustomChildren;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Stubbed;
import com.intellij.util.xml.SubTag;

import java.util.List;

import cn.taketoday.assistant.model.values.PropertyValueConverter;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.InfraModelElement;

/**
 * @author Dmitry Avdeev
 */
public interface InfraElementsHolder extends InfraModelElement, TypeHolder {

  /**
   * Returns the value of the bean child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:bean documentation</h3>
   * 	Defines a single (usually named) bean.
   * 	A bean definition may contain nested tags for constructor arguments,
   * 	property values, lookup methods, and replaced methods. Mixing constructor
   * 	injection and setter injection on the same bean is explicitly supported.
   * <p/>
   * </pre>
   *
   * @return the value of the bean child.
   */

  @Stubbed
  InfraBean getBean();

  /**
   * Returns the value of the ref child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:ref documentation</h3>
   * 	Defines a reference to another bean in this factory or an external
   * 	factory (parent or included factory).
   * <p/>
   * </pre>
   *
   * @return the value of the ref child.
   */

  @Stubbed
  InfraRef getRef();

  /**
   * Returns the value of the idref child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:idref documentation</h3>
   * 	The id of another bean in this factory or an external factory
   * 	(parent or included factory).
   * 	While a regular 'value' element could instead be used for the
   * 	same effect, using idref in this case allows validation of local
   * 	bean ids by the XML parser, and name completion by supporting tools.
   * <p/>
   * </pre>
   *
   * @return the value of the idref child.
   */

  @Stubbed
  Idref getIdref();

  /**
   * Returns the value of the value child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:value documentation</h3>
   * 	Contains a string representation of a property value.
   * 	The property may be a string, or may be converted to the required
   * 	type using the JavaBeans PropertyEditor machinery. This makes it
   * 	possible for application developers to write custom PropertyEditor
   * 	implementations that can convert strings to arbitrary target objects.
   * 	Note that this is recommended for simple objects only. Configure
   * 	more complex objects by populating JavaBean properties with
   * 	references to other beans.
   * <p/>
   * </pre>
   *
   * @return the value of the value child.
   */

  @Convert(PropertyValueConverter.class)
  @Stubbed
  InfraValue getValue();

  /**
   * Returns the value of the null child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:null documentation</h3>
   * 	Denotes a Java null value. Necessary because an empty "value" tag
   * 	will resolve to an empty String, which will not be resolved to a
   * 	null value unless a special PropertyEditor does so.
   * <p/>
   * </pre>
   *
   * @return the value of the null child.
   */

  @SubTag(value = "null", indicator = true)
  @Stubbed
  GenericDomValue<Boolean> getNull();

  /**
   * Returns the value of the list child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:list documentation</h3>
   * 	A list can contain multiple inner bean, ref, collection, or value
   * 	elements. Java lists are untyped, pending generics support in Java5,
   * 	although references will be strongly typed. A list can also map to
   * 	an array type. The necessary conversion is automatically performed
   * 	by the BeanFactory.
   * <p/>
   * </pre>
   *
   * @return the value of the list child.
   */

  @Stubbed
  ListOrSet getList();

  /**
   * Returns the value of the set child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:set documentation</h3>
   * 	A set can contain multiple inner bean, ref, collection, or value
   * 	elements. Java sets are untyped, pending generics support in Java5,
   * 	although references will be strongly typed.
   * <p/>
   * </pre>
   *
   * @return the value of the set child.
   */

  @Stubbed
  ListOrSet getSet();

  @Stubbed
  ListOrSet getArray();

  /**
   * Returns the value of the map child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:map documentation</h3>
   * 	A mapping from a key to an object. Maps may be empty.
   * <p/>
   * </pre>
   *
   * @return the value of the map child.
   */

  @Stubbed
  InfraMap getMap();

  /**
   * Returns the value of the props child.
   * <pre>
   * <h3>Element http://www.springframework.org/schema/beans:props documentation</h3>
   * 	Props elements differ from map elements in that values must be strings.
   * 	Props may be empty.
   * <p/>
   * </pre>
   *
   * @return the value of the props child.
   */

  @Stubbed
  Props getProps();

  @CustomChildren
  List<CustomBeanWrapper> getCustomBeans();
}
