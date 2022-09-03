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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.ArrayIteratorKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

public final class ModulesSorter implements Sequence<Module> {
  private final ConcurrentHashMap<Module, ModuleHolder> modulesHolders;
  private final AtomicLong maxSortTime;

  private final Module[] modules;

  public static ModulesSorter getInstance(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider<>() {
      public CachedValueProvider.Result<ModulesSorter> compute() {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        return Result.create(new ModulesSorter(modules), moduleManager);
      }
    });
  }

  public Module[] getModules() {
    return this.modules;
  }

  private ModulesSorter(Module[] modules) {
    this.modules = modules;
    var hashMap = new ConcurrentHashMap<Module, ModuleHolder>();
    for (Module module : this.modules) {
      hashMap.put(module, new ModuleHolder(module, Double.MIN_VALUE, 0, 0L, Long.MAX_VALUE));
    }
    this.modulesHolders = hashMap;
    this.maxSortTime = new AtomicLong(0L);
  }

  private boolean isEnabled() {
    return Registry.is("infra.urlpath.completion.smart");
  }

  private <T> T measureComputationTime(Function0<? extends T> function0) {
    long prev;
    long startTime = System.currentTimeMillis();
    T t = function0.invoke();
    long spentTime = System.currentTimeMillis() - startTime;
    do {
      prev = this.maxSortTime.get();
      if (prev >= spentTime) {
        break;
      }
    }
    while (!this.maxSortTime.compareAndSet(prev, spentTime));
    return t;
  }

  public Iterator<Module> iterator() {
    if (isEnabled()) {
      Collection modules1 = measureComputationTime(new Function0<Collection<Module>>() {
        @Override
        public Collection<Module> invoke() {
          Collection values = modulesHolders.values();
          return CollectionsKt.sorted(values);
        }
      });
      return SequencesKt.map(CollectionsKt.asSequence(modules1), ModuleHolder::getModule).iterator();
    }
    return ArrayIteratorKt.iterator(this.modules);
  }

  private static final class ModuleHolder implements Comparable<ModuleHolder> {

    private final Module module;
    private final double rate;
    private final int count;
    private final long maxTime;
    private final long minTime;

    public Module component1() {
      return this.module;
    }

    public double component2() {
      return this.rate;
    }

    public int component3() {
      return this.count;
    }

    public long component4() {
      return this.maxTime;
    }

    public long component5() {
      return this.minTime;
    }

    public ModuleHolder copy(Module module, double rate, int count, long maxTime, long minTime) {
      Intrinsics.checkNotNullParameter(module, "module");
      return new ModuleHolder(module, rate, count, maxTime, minTime);
    }

    public static ModuleHolder copy$default(ModuleHolder moduleHolder, Module module, double d, int i, long j, long j2, int i2, Object obj) {
      if ((i2 & 1) != 0) {
        module = moduleHolder.module;
      }
      if ((i2 & 2) != 0) {
        d = moduleHolder.rate;
      }
      if ((i2 & 4) != 0) {
        i = moduleHolder.count;
      }
      if ((i2 & 8) != 0) {
        j = moduleHolder.maxTime;
      }
      if ((i2 & 16) != 0) {
        j2 = moduleHolder.minTime;
      }
      return moduleHolder.copy(module, d, i, j, j2);
    }

    @Override
    public String toString() {
      return ToStringBuilder.from(this)
              .append("module", module)
              .append("rate", rate)
              .append("count", count)
              .append("maxTime", maxTime)
              .append("minTime", minTime)
              .toString();
    }

    public int hashCode() {
      Module module = this.module;
      return ((((((((module != null ? module.hashCode() : 0) * 31) + Double.hashCode(this.rate)) * 31) + Integer.hashCode(this.count)) * 31) + Long.hashCode(this.maxTime)) * 31) + Long.hashCode(
              this.minTime);
    }

    public boolean equals(@Nullable Object obj) {
      if (this != obj) {
        if (!(obj instanceof ModuleHolder moduleHolder)) {
          return false;
        }
        return Intrinsics.areEqual(this.module, moduleHolder.module) && Double.compare(this.rate,
                moduleHolder.rate) == 0 && this.count == moduleHolder.count && this.maxTime == moduleHolder.maxTime && this.minTime == moduleHolder.minTime;
      }
      return true;
    }

    public Module getModule() {
      return this.module;
    }

    public ModuleHolder(Module module, double rate, int count, long maxTime, long minTime) {
      Intrinsics.checkNotNullParameter(module, "module");
      this.module = module;
      this.rate = rate;
      this.count = count;
      this.maxTime = maxTime;
      this.minTime = minTime;
    }

    public double getRate() {
      return this.rate;
    }

    public int getCount() {
      return this.count;
    }

    public long getMaxTime() {
      return this.maxTime;
    }

    public long getMinTime() {
      return this.minTime;
    }

    @Override
    public int compareTo(ModuleHolder other) {
      Intrinsics.checkNotNullParameter(other, "other");
      return Double.compare(this.rate, other.rate);
    }
  }

  public void dump(Appendable out) {
    try {
      Intrinsics.checkNotNullParameter(out, "out");
      Appendable append = out.append("enabled: " + isEnabled());
      Intrinsics.checkNotNullExpressionValue(append, "append(value)");
      Intrinsics.checkNotNullExpressionValue(append.append('\n'), "append('\\n')");
      Appendable append2 = out.append("max time sorting: " + this.maxSortTime.get() + " ms");
      Intrinsics.checkNotNullExpressionValue(append2, "append(value)");
      Intrinsics.checkNotNullExpressionValue(append2.append('\n'), "append('\\n')");
      Intrinsics.checkNotNullExpressionValue(out.append('\n'), "append('\\n')");
      Appendable append3 = out.append("Modules sorted:");
      Intrinsics.checkNotNullExpressionValue(append3, "append(value)");
      Intrinsics.checkNotNullExpressionValue(append3.append('\n'), "append('\\n')");
      Collection<ModuleHolder> values = this.modulesHolders.values();
      Intrinsics.checkNotNullExpressionValue(values, "modulesHolders.values");
      for (ModuleHolder moduleHolder : CollectionsKt.sorted(values)) {
        String name = moduleHolder.getModule().getName();
        int count = moduleHolder.getCount();
        long minTime = moduleHolder.getMinTime();
        long maxTime = moduleHolder.getMaxTime();
        moduleHolder.getRate();
        Appendable append4 = out.append("  " + name + " " + count + " " + minTime + "-" + name + " " + maxTime);
        Intrinsics.checkNotNullExpressionValue(append4, "append(value)");
        Intrinsics.checkNotNullExpressionValue(append4.append('\n'), "append('\\n')");
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Function2<Long, Integer, Unit> listener(Module module) {
    return new Function2<Long, Integer, Unit>() {
      @Override
      public Unit invoke(Long time, Integer count) {
        ModulesSorter.ModuleHolder holder = modulesHolders.get(module);
        if (holder != null) {
          Intrinsics.checkNotNullExpressionValue(holder, "modulesHolders[module] ?…ant find module $module\")");
          if (count > 0 && count >= holder.getCount()) {
            double newRate = time / count;
            if (count > holder.getCount() || newRate < holder.getRate()) {
              ModulesSorter.ModuleHolder copy$default = ModulesSorter.ModuleHolder.copy$default(holder, null, newRate, count, Long.max(holder.getMaxTime(), time), Long.min(holder.getMinTime(), time),
                      1,
                      null);
              modulesHolders.put(module, copy$default);
            }
          }
          return Unit.INSTANCE;
        }
        throw new IllegalStateException("cant find module " + module);
      }
    };
  }

}
