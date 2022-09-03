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

import com.intellij.util.xml.NamedEnum;

import cn.taketoday.lang.Nullable;

public enum DependencyCheck implements NamedEnum {

  ALL("all"),
  DEFAULT("default"),
  NONE("none"),
  OBJECTS("objects"),
  SIMPLE("simple");

  private final String value;

  DependencyCheck(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  public static DependencyCheck fromDefault(@Nullable DefaultDependencyCheck def) {
    if (def == null) {
      return DEFAULT;
    }
    return switch (def) {
      case ALL -> ALL;
      case NONE -> NONE;
      case OBJECTS -> OBJECTS;
      case SIMPLE -> SIMPLE;
    };
  }
}
