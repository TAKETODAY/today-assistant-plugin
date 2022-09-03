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

package cn.taketoday.assistant.model.config;

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.lang.Nullable;
import kotlin.collections.SetsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

public final class ConfigurationValueSearchParams {

  private final Module module;
  private final boolean checkRelaxedNames;
  @Nullable
  private final Set<String> activeProfiles;

  private final MetaConfigKey configKey;
  @Nullable
  private final String keyIndex;
  @Nullable
  private final String keyProperty;
  private final boolean processImports;

  private final Set<VirtualFile> processedFiles;

  public static final Set<String> PROCESS_ALL_VALUES = SetsKt.setOf("PROCESS_ALL_VALUES");

  public Module component1() {
    return this.module;
  }

  public boolean component2() {
    return this.checkRelaxedNames;
  }

  @Nullable
  public Set<String> component3() {
    return this.activeProfiles;
  }

  public MetaConfigKey component4() {
    return this.configKey;
  }

  @Nullable
  public String component5() {
    return this.keyIndex;
  }

  @Nullable
  public String component6() {
    return this.keyProperty;
  }

  public boolean component7() {
    return this.processImports;
  }

  public Set<VirtualFile> component8() {
    return this.processedFiles;
  }

  public ConfigurationValueSearchParams copy(Module module, boolean checkRelaxedNames,
          @Nullable Set<String> set, MetaConfigKey configKey, @Nullable String keyIndex,
          @Nullable String keyProperty, boolean processImports, Set<VirtualFile> set2) {
    Intrinsics.checkNotNullParameter(module, "module");
    Intrinsics.checkNotNullParameter(configKey, "configKey");
    Intrinsics.checkNotNullParameter(set2, "processedFiles");
    return new ConfigurationValueSearchParams(module, checkRelaxedNames, set, configKey, keyIndex, keyProperty, processImports, set2);
  }

  public static ConfigurationValueSearchParams copy$default(ConfigurationValueSearchParams configurationValueSearchParams, Module module, boolean z, Set set, MetaConfigKey metaConfigKey, String str,
          String str2, boolean z2, Set set2, int i, Object obj) {
    if ((i & 1) != 0) {
      module = configurationValueSearchParams.module;
    }
    if ((i & 2) != 0) {
      z = configurationValueSearchParams.checkRelaxedNames;
    }
    if ((i & 4) != 0) {
      set = configurationValueSearchParams.activeProfiles;
    }
    if ((i & 8) != 0) {
      metaConfigKey = configurationValueSearchParams.configKey;
    }
    if ((i & 16) != 0) {
      str = configurationValueSearchParams.keyIndex;
    }
    if ((i & 32) != 0) {
      str2 = configurationValueSearchParams.keyProperty;
    }
    if ((i & 64) != 0) {
      z2 = configurationValueSearchParams.processImports;
    }
    if ((i & 128) != 0) {
      set2 = configurationValueSearchParams.processedFiles;
    }
    return configurationValueSearchParams.copy(module, z, set, metaConfigKey, str, str2, z2, set2);
  }

  public String toString() {
    return "ConfigurationValueSearchParams(module=" + this.module + ", checkRelaxedNames=" + this.checkRelaxedNames + ", activeProfiles=" + this.activeProfiles + ", configKey=" + this.configKey + ", keyIndex=" + this.keyIndex + ", keyProperty=" + this.keyProperty + ", processImports=" + this.processImports + ", processedFiles=" + this.processedFiles + ")";
  }

  public int hashCode() {
    Module module = this.module;
    int hashCode = (module != null ? module.hashCode() : 0) * 31;
    boolean z = this.checkRelaxedNames;
    if (z) {
      z = true;
    }
    int i = z ? 1 : 0;
    int i2 = z ? 1 : 0;
    int i3 = (hashCode + i) * 31;
    Set<String> set = this.activeProfiles;
    int hashCode2 = (i3 + (set != null ? set.hashCode() : 0)) * 31;
    MetaConfigKey metaConfigKey = this.configKey;
    int hashCode3 = (hashCode2 + (metaConfigKey != null ? metaConfigKey.hashCode() : 0)) * 31;
    String str = this.keyIndex;
    int hashCode4 = (hashCode3 + (str != null ? str.hashCode() : 0)) * 31;
    String str2 = this.keyProperty;
    int hashCode5 = (hashCode4 + (str2 != null ? str2.hashCode() : 0)) * 31;
    boolean z2 = this.processImports;
    if (z2) {
      z2 = true;
    }
    int i4 = z2 ? 1 : 0;
    int i5 = z2 ? 1 : 0;
    int i6 = (hashCode5 + i4) * 31;
    Set<VirtualFile> set2 = this.processedFiles;
    return i6 + (set2 != null ? set2.hashCode() : 0);
  }

  public boolean equals(@Nullable Object obj) {
    if (this != obj) {
      if (!(obj instanceof ConfigurationValueSearchParams configurationValueSearchParams)) {
        return false;
      }
      return Intrinsics.areEqual(this.module, configurationValueSearchParams.module) && this.checkRelaxedNames == configurationValueSearchParams.checkRelaxedNames && Intrinsics.areEqual(
              this.activeProfiles, configurationValueSearchParams.activeProfiles) && Intrinsics.areEqual(this.configKey, configurationValueSearchParams.configKey) && Intrinsics.areEqual(this.keyIndex,
              configurationValueSearchParams.keyIndex) && Intrinsics.areEqual(this.keyProperty,
              configurationValueSearchParams.keyProperty) && this.processImports == configurationValueSearchParams.processImports && Intrinsics.areEqual(this.processedFiles,
              configurationValueSearchParams.processedFiles);
    }
    return true;
  }

  public Module getModule() {
    return this.module;
  }

  public ConfigurationValueSearchParams(Module module, boolean checkRelaxedNames,
          @Nullable Set<String> set, MetaConfigKey configKey,
          @Nullable String keyIndex, @Nullable String keyProperty,
          boolean processImports, Set<VirtualFile> set2) {
    this.module = module;
    this.checkRelaxedNames = checkRelaxedNames;
    this.activeProfiles = set;
    this.configKey = configKey;
    this.keyIndex = keyIndex;
    this.keyProperty = keyProperty;
    this.processImports = processImports;
    this.processedFiles = set2;
  }

  public boolean getCheckRelaxedNames() {
    return this.checkRelaxedNames;
  }

  public ConfigurationValueSearchParams(Module module, boolean z, Set set, MetaConfigKey metaConfigKey,
          String str, String str2, boolean z2, Set set2, int i,
          DefaultConstructorMarker defaultConstructorMarker) {
    this(module, (i & 2) != 0 || z, set, metaConfigKey, str, str2, (i & 64) != 0 || z2, (i & 128) != 0 ? new HashSet() : set2);
  }

  @Nullable
  public Set<String> getActiveProfiles() {
    return this.activeProfiles;
  }

  public MetaConfigKey getConfigKey() {
    return this.configKey;
  }

  @Nullable
  public String getKeyIndex() {
    return this.keyIndex;
  }

  @Nullable
  public String getKeyProperty() {
    return this.keyProperty;
  }

  public boolean getProcessImports() {
    return this.processImports;
  }

  public Set<VirtualFile> getProcessedFiles() {
    return this.processedFiles;
  }

  public ConfigurationValueSearchParams(Module module, boolean checkRelaxedNames, @Nullable Set<String> set, MetaConfigKey configKey) {
    this(module, checkRelaxedNames, set, configKey, null, null, false, null, 192, null);
  }

  public ConfigurationValueSearchParams(Module module, boolean z, Set set, MetaConfigKey metaConfigKey, int i, DefaultConstructorMarker defaultConstructorMarker) {
    this(module, (i & 2) != 0 || z, set, metaConfigKey);
  }

  public ConfigurationValueSearchParams(Module module, MetaConfigKey configKey) {
    this(module, true, PROCESS_ALL_VALUES, configKey);
  }

  public boolean isProcessAllProfiles() {
    return Intrinsics.areEqual(this.activeProfiles, PROCESS_ALL_VALUES);
  }
}
