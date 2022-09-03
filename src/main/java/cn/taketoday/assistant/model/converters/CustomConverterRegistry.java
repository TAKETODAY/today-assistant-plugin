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
package cn.taketoday.assistant.model.converters;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Condition;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.lang.Nullable;

public class CustomConverterRegistry {

  public interface Provider extends Condition<GenericDomValue> {

    ExtensionPointName<Provider> EXTENSION_POINT_NAME = ExtensionPointName.create("cn.taketoday.assistant.customConverterProvider");

    Converter getConverter();

    Class getConverterClass();
  }

  public static CustomConverterRegistry getRegistry() {
    return ourInstance;
  }

  private static final CustomConverterRegistry ourInstance = new CustomConverterRegistry();

  @Nullable
  public Converter<?> getCustomConverter(Class aClass, GenericDomValue context) {
    for (Provider provider : Provider.EXTENSION_POINT_NAME.getExtensionList()) {
      if (aClass.equals(provider.getConverterClass()) && provider.value(context))
        return provider.getConverter();
    }
    return null;
  }
}
