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

package cn.taketoday.assistant.model.wrappers;

import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.facet.InfraFileSet;

public class WFileset {

  public final String name;

  public final ArrayList<WModel> models;

  private final Set<String> activeProfiles;

  private final List<String> dependencyFileSets;

  private final List<String> propertiesFiles;
  private final boolean autodetected;
  private final boolean removed;

  public WFileset(String name, Set<String> activeProfiles, List<String> dependencyFileSets, List<String> propertiesFiles, boolean autodetected, boolean removed) {
    this.models = new ArrayList<>();
    this.name = name;
    this.activeProfiles = activeProfiles;
    this.dependencyFileSets = dependencyFileSets;
    this.propertiesFiles = propertiesFiles;
    this.autodetected = autodetected;
    this.removed = removed;
  }

  public WFileset(InfraFileSet fileSet) {
    this.models = new ArrayList<>();
    this.name = fileSet.getName();
    this.activeProfiles = fileSet.getActiveProfiles();
    this.dependencyFileSets = ContainerUtil.map(fileSet.getDependencyFileSets(), InfraFileSet::getName);
    this.propertiesFiles = ContainerUtil.map(fileSet.getPropertiesFiles(), VirtualFilePointer::getFileName);
    this.autodetected = fileSet.isAutodetected();
    this.removed = fileSet.isRemoved();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WFileset set = (WFileset) o;
    return this.name.equals(set.name) && this.models.equals(set.models);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    return (31 * result) + this.models.hashCode();
  }
}
