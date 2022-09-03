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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.xml.RequiredBeanType;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public interface PathMatching extends DomElement {
  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ InfraMvcConstant.PATH_MATCHER })
  GenericAttributeValue<BeanPointer<?>> getPathMatcher();

  @Convert(InfraBeanResolveConverter.class)
  @RequiredBeanType({ "cn.taketoday.web.util.UrlPathHelper" })
  GenericAttributeValue<BeanPointer<?>> getPathHelper();

  GenericAttributeValue<Boolean> getRegisteredSuffixesOnly();

  GenericAttributeValue<Boolean> getSuffixPattern();

  GenericAttributeValue<Boolean> getTrailingSlash();
}
