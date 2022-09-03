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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.psi.util.CachedValue;

import java.util.function.BiConsumer;

import cn.taketoday.lang.Nullable;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public final class PerformanceTracker {
  private int remainToTrack;
  private long aggregatedTime;
  private int aggregatedCount;
  private final BiConsumer<? super Long, ? super Integer> report;

  public static <T> T weakTrack(@Nullable PerformanceTracker tracker, Function1<? super T, Integer> function1, Function0<? extends T> function0) {
    if (tracker != null) {
      return tracker.track(function1, function0);
    }
    return function0.invoke();
  }

  public static <T> T weakTrack(@Nullable PerformanceTracker tracker, Function1<? super T, Integer> function1, CachedValue<T> cachedValue) {
    if (tracker != null && !cachedValue.hasUpToDateValue()) {
      return tracker.track(function1, cachedValue::getValue);
    }
    return cachedValue.getValue();
  }

  public PerformanceTracker(BiConsumer<? super Long, ? super Integer> biConsumer, int trackCount) {
    this.report = biConsumer;
    this.remainToTrack = trackCount;
  }

  public <T> T track(Function1<? super T, Integer> function1, Function0<? extends T> function0) {
    Intrinsics.checkNotNullParameter(function1, "weight");
    Intrinsics.checkNotNullParameter(function0, "f");
    long startTime = System.currentTimeMillis();
    T t = function0.invoke();
    long evalTime = System.currentTimeMillis() - startTime;
    this.aggregatedTime += evalTime;
    this.aggregatedCount += ((Number) function1.invoke(t)).intValue();
    this.remainToTrack--;
    if (this.remainToTrack == 0) {
      this.report.accept(this.aggregatedTime, this.aggregatedCount);
    }
    return t;
  }

}
