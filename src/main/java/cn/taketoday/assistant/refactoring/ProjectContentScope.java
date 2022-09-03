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

package cn.taketoday.assistant.refactoring;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;
import java.util.Objects;

public class ProjectContentScope extends GlobalSearchScope {
  private final ProjectFileIndex myFileIndex;

  public ProjectContentScope(Project project) {
    super(project);
    this.myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
  }

  public boolean contains(VirtualFile file) {
    return this.myFileIndex.isInContent(file);
  }

  public boolean isSearchInModuleContent(Module aModule) {
    return true;
  }

  public boolean isSearchInLibraries() {
    return false;
  }

  public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
    return ModuleManager.getInstance(Objects.requireNonNull(getProject())).getUnloadedModuleDescriptions();
  }
}
