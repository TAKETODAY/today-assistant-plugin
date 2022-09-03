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

package cn.taketoday.assistant.facet;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class InfraFileSetData {
  private String name;
  private final String id;
  private final List<String> files;
  private final Set<String> dependencies;

  private Set<String> activeProfiles;

  private boolean removed;

  public static InfraFileSetData create(String id, String name) {
    return new InfraFileSetData(id, name);
  }

  private InfraFileSetData(String id, String name) {
    this.id = id;
    this.name = name;
    this.files = new SmartList<>();
    this.dependencies = new LinkedHashSet<>();
    this.activeProfiles = new LinkedHashSet<>();
  }

  public boolean isRemoved() {
    return this.removed;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getFiles() {
    return this.files;
  }

  public void setActiveProfiles(Set<String> activeProfiles) {
    this.activeProfiles = activeProfiles;
  }

  public Set<String> getActiveProfiles() {
    return this.activeProfiles;
  }

  public Set<String> getDependencies() {
    return this.dependencies;
  }

  public void addDependency(String dep) {
    this.dependencies.add(dep);
  }

  public void addFile(String url) {
    if (!StringUtil.isEmptyOrSpaces(url)) {
      this.files.add(url);
    }
  }

  public void removeFile(String url) {
    this.files.remove(url);
  }

  public void setRemoved(boolean removed) {
    this.removed = removed;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraFileSetData data = (InfraFileSetData) o;
    if (!this.id.equals(data.id)) {
      return false;
    }
    return Objects.equals(this.name, data.name);
  }

  public int hashCode() {
    int result = this.id.hashCode();
    return (31 * result) + (this.name != null ? this.name.hashCode() : 0);
  }

  public static InfraFileSetData create(InfraFileSetData original) {
    InfraFileSetData infraFileSetData = new InfraFileSetData(original.id, original.name);
    infraFileSetData.files.addAll(original.files);
    infraFileSetData.setRemoved(original.removed);
    infraFileSetData.dependencies.addAll(original.dependencies);
    infraFileSetData.activeProfiles.addAll(original.activeProfiles);
    return infraFileSetData;
  }

}
