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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.hash.LinkedHashMap;

import cn.taketoday.lang.Nullable;

public abstract class InfraSizeLimitedCache<K, V> {
  protected final LinkedHashMap<K, V> myQueue;

  private final Condition<? super K> myKeyValidityCheck;
  private final int myMaxQueueSize;
  private final Object myLock;

  protected abstract V createValue(K k);

  public InfraSizeLimitedCache(int maxQueueSize, Condition<? super K> keyValidityCheck) {
    this.myLock = new Object();
    this.myQueue = new LinkedHashMap<>(10, 0.6f);
    this.myKeyValidityCheck = keyValidityCheck;
    this.myMaxQueueSize = maxQueueSize;
  }

  @Nullable
  public V getCachedValue(K key) {
    V value;
    synchronized(this.myLock) {
      value = this.myQueue.get(key);
    }
    return value;
  }

  public V get(K key) {
    V value = getCachedValue(key);
    if (value != null) {
      return value;
    }
    V newValue = createValue(key);
    synchronized(this.myLock) {
      if (this.myQueue.size() >= this.myMaxQueueSize) {
        if (containsInvalidKeys()) {
          this.myQueue.clear();
        }
        if (this.myQueue.remove(this.myQueue.getFirstKey()) == null) {
          this.myQueue.clear();
        }
      }
      this.myQueue.put(key, newValue);
    }
    return newValue;
  }

  private boolean containsInvalidKeys() {
    for (K obj : this.myQueue.keySet()) {
      if (!this.myKeyValidityCheck.value(obj)) {
        return true;
      }
    }
    return false;
  }
}
