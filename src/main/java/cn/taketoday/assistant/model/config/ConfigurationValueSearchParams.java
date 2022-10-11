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

import java.util.Objects;
import java.util.Set;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import kotlin.collections.SetsKt;
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

  public ConfigurationValueSearchParams(Module module, boolean checkRelaxedNames, @Nullable Set<String> set, MetaConfigKey configKey) {
    this(module, checkRelaxedNames, set, configKey, null, null, false, null);
  }

  public ConfigurationValueSearchParams(Module module, MetaConfigKey configKey) {
    this(module, true, PROCESS_ALL_VALUES, configKey);
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

  public Module getModule() {
    return this.module;
  }

  public boolean getCheckRelaxedNames() {
    return this.checkRelaxedNames;
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

  public boolean isProcessAllProfiles() {
    return Objects.equals(activeProfiles, PROCESS_ALL_VALUES);
  }

  @Override
  public int hashCode() {
    return Objects.hash(module, checkRelaxedNames, activeProfiles,
            configKey, keyIndex, keyProperty, processImports, processedFiles);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ConfigurationValueSearchParams that))
      return false;
    return checkRelaxedNames == that.checkRelaxedNames
            && processImports == that.processImports
            && Objects.equals(module, that.module)
            && Objects.equals(keyIndex, that.keyIndex)
            && Objects.equals(configKey, that.configKey)
            && Objects.equals(keyProperty, that.keyProperty)
            && Objects.equals(activeProfiles, that.activeProfiles)
            && Objects.equals(processedFiles, that.processedFiles);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("module", module)
            .append("checkRelaxedNames", checkRelaxedNames)
            .append("activeProfiles", activeProfiles)
            .append("configKey", configKey)
            .append("keyIndex", keyIndex)
            .append("keyProperty", keyProperty)
            .append("processImports", processImports)
            .append("processedFiles", processedFiles)
            .toString();
  }
}
