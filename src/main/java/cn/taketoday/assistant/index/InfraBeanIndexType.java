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

package cn.taketoday.assistant.index;

import com.intellij.openapi.util.Pair;

enum InfraBeanIndexType {
  BEAN_NAME(false),
  BEAN_CLASS(false),
  COMPONENT_SCAN(true),
  FACTORY_BEAN(true),
  FACTORY_BEAN_CLASS(true),
  FACTORY_METHOD(true),
  CUSTOM_BEAN_WRAPPER(true),
  ALIAS(false),
  BEAN_TYPE_PROVIDER(true),
  ABSTRACT_BEAN(true),
  BEAN_NAME_PROVIDER(true);

  private final Pair<InfraBeanIndexType, String> myDefaultKey;

  InfraBeanIndexType(boolean hasDefaultKey) {
    this.myDefaultKey = hasDefaultKey ? Pair.create(this, "") : null;
  }

  public Pair<InfraBeanIndexType, String> key() {
    return this.myDefaultKey;
  }
}
