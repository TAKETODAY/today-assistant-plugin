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

import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.lang.Nullable;

abstract class AbstractLiveProperty<T> implements LiveProperty<T> {
  private final AtomicReference<T> myValue = new AtomicReference<>();
  private volatile long myTimeStamp = -1;
  private final List<LivePropertyListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  protected AbstractLiveProperty(T defaultValue) {
    this.myValue.set(defaultValue);
  }

  @Override
  @Nullable
  public T getValue() {
    return this.myValue.get();
  }

  @Override
  public long getTimeStamp() {
    return this.myTimeStamp;
  }

  @Override
  public void addPropertyListener(LiveProperty.LivePropertyListener listener) {
    this.myListeners.add(listener);
  }

  @Override
  public void removePropertyListener(LiveProperty.LivePropertyListener listener) {
    this.myListeners.remove(listener);
  }

  protected T setValue(T value) {
    this.myTimeStamp = System.currentTimeMillis();
    return this.myValue.getAndSet(value);
  }

  protected List<LivePropertyListener> getListeners() {
    return this.myListeners;
  }
}
