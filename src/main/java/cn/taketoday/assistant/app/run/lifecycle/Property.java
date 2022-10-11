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
package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.openapi.Disposable;

import cn.taketoday.lang.Nullable;

/**
 * @author konstantin.aleev
 */
public interface Property<T> extends Disposable {
  void compute();

  @Nullable
  T getValue();

  /**
   * @return time stamp of the last computation or {@code -1} if property value has not been computed yet.
   */
  long getTimeStamp();

  void addPropertyListener(PropertyListener listener);

  void removePropertyListener(PropertyListener listener);

  interface PropertyListener {
    void propertyChanged();

    default void computationFailed(Exception e) {
    }

    default void computationFinished() {
    }
  }
}
