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

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.impl.LightFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

public abstract class InfraAutodetectedFileSet extends InfraFileSet {
  private final Icon myIcon;

  protected InfraAutodetectedFileSet(String id, String name, InfraFacet parent, Icon icon) {
    super(id, name, parent);
    this.myIcon = icon;
  }

  @Override
  public final Icon getIcon() {
    return this.myIcon;
  }

  @Override
  public final boolean isAutodetected() {
    return true;
  }

  @Override
  public final void setAutodetected(boolean autodetected) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected final VirtualFilePointer createVirtualFilePointer(String url) {
    return new LightFilePointer(url);
  }

  @Override
  public final void setActiveProfiles(Set<String> activeProfiles) {
    super.setActiveProfiles(activeProfiles);
    getFacet().getConfiguration().setActiveProfilesForAutodetected(getId(), activeProfiles);
  }

  public static void refreshAutodetectedFileSets() {
    InfraFileSetService fileSetService = InfraFileSetService.of();
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        InfraFacet facet = InfraFacet.from(module);
        if (facet != null) {
          Set<InfraFileSet> fileSets = new HashSet<>(facet.getFileSets());
          List<String> providerSets = ContainerUtil.map(fileSetService.getModelProviderSets(facet), InfraFileSet::getId);
          boolean modified = ContainerUtil.retainAll(fileSets, fileSet -> !fileSet.isAutodetected() || providerSets.contains(fileSet.getId()));
          if (modified) {
            facet.removeFileSets();
            for (InfraFileSet fileSet2 : fileSets) {
              facet.addFileSet(fileSet2);
            }
          }
          else {
            facet.getConfiguration().setModified();
          }
          FacetManager.getInstance(module).facetConfigurationChanged(facet);
        }
      }
    }
  }
}
