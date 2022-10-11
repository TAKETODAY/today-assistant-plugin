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

abstract class AbstractProperty<T> implements Property<T> {

  private final AtomicReference<T> value = new AtomicReference<>();

  private volatile long timeStamp = -1;

  private final List<PropertyListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();

  protected AbstractProperty(T defaultValue) {
    this.value.set(defaultValue);
  }

  @Override
  @Nullable
  public T getValue() {
    return this.value.get();
  }

  @Override
  public long getTimeStamp() {
    return this.timeStamp;
  }

  @Override
  public void addPropertyListener(PropertyListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removePropertyListener(PropertyListener listener) {
    this.listeners.remove(listener);
  }

  protected T setValue(T value) {
    this.timeStamp = System.currentTimeMillis();
    return this.value.getAndSet(value);
  }

  protected List<PropertyListener> getListeners() {
    return this.listeners;
  }

  public void dispatchComputationFailed(Exception e) {
    for (PropertyListener listener : listeners) {
      listener.computationFailed(e);
    }
  }

  public void dispatchComputationFinished() {
    for (PropertyListener listener : listeners) {
      listener.computationFinished();
    }
  }

  public void dispatchPropertyChanged() {
    for (PropertyListener listener : listeners) {
      listener.propertyChanged();
    }
  }

}
