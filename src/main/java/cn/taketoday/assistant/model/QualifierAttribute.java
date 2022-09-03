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
package cn.taketoday.assistant.model;

import com.intellij.openapi.util.Comparing;
import com.intellij.util.containers.HashingStrategy;

import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public interface QualifierAttribute {
  @Nullable
  String getAttributeKey();

  @Nullable
  Object getAttributeValue();

  HashingStrategy<QualifierAttribute> HASHING_STRATEGY = new HashingStrategy<>() {
    @Override
    public int hashCode(QualifierAttribute object) {
      if (object == null) {
        return 0;
      }

      String key = object.getAttributeKey();
      Object value = object.getAttributeValue();
      return (key == null ? 0 : key.hashCode()) + (value == null ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(QualifierAttribute o1, QualifierAttribute o2) {
      return o1 == o2 ||
              (o1 != null &&
                      o2 != null &&
                      Objects.equals(o1.getAttributeKey(), o2.getAttributeKey()) &&
                      Comparing.equal(o1.getAttributeValue(), o2.getAttributeValue()));
    }
  };
}
