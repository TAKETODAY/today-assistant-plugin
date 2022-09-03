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

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.CustomChildren;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.Stubbed;
import com.intellij.util.xml.SubTagList;

import java.util.List;

import cn.taketoday.assistant.model.values.ListOrSetValueConverter;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;

public interface CollectionElements extends DomElement, Description {

  @Stubbed
  List<InfraBean> getBeans();

  InfraBean addBean();

  @CustomChildren
  List<CustomBeanWrapper> getCustomBeans();

  @Stubbed
  List<InfraRef> getRefs();

  InfraRef addRef();

  @Stubbed
  List<Idref> getIdrefs();

  Idref addIdref();

  /**
   * Returns the list of value children.
   * <pre>
   * <h3>Element <a href="http://www.springframework.org/schema/beans:value">...</a> documentation</h3>
   * 	Contains a string representation of a property value.
   * 	The property may be a string, or may be converted to the required
   * 	type using the JavaBeans PropertyEditor machinery. This makes it
   * 	possible for application developers to write custom PropertyEditor
   * 	implementations that can convert strings to arbitrary target objects.
   * 	Note that this is recommended for simple objects only. Configure
   * 	more complex objects by populating JavaBean properties with
   * 	references to other beans.
   *
   * </pre>
   *
   * @return the list of value children.
   */
  @Convert(ListOrSetValueConverter.class)

  @Stubbed
  List<InfraValue> getValues();

  InfraValue addValue();

  InfraNull getNull();

  @Stubbed
  List<ListOrSet> getLists();

  @Stubbed
  List<ListOrSet> getArrays();

  @Stubbed
  List<ListOrSet> getSets();

  @Stubbed
  List<InfraMap> getMaps();

  @SubTagList("props")
  List<Props> getProps();

  @SubTagList("props")
  Props addProps();
}