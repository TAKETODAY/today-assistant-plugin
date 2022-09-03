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

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;

import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;

public class WModel {

  public final String name;

  public final ModelType type;

  public final ArrayList<WInfraDependency> dependencies;

  private final WScope scope;

  public WModel(String name, ModelType type, WScope scope) {
    this.dependencies = new ArrayList<>();
    this.name = name;
    this.type = type;
    this.scope = scope;
  }

  public WModel(LocalModel model) {
    this.dependencies = new ArrayList<>();
    if (model instanceof LocalXmlModel) {
      this.name = getLocalXmlModelName((LocalXmlModel) model);
      this.type = ModelType.XML;
    }
    else if (model instanceof LocalAnnotationModel) {
      this.name = ((LocalAnnotationModel) model).getConfig().getQualifiedName();
      this.type = ModelType.ANNO;
    }
    else {
      throw new IllegalArgumentException(String.format("Can't get model type for model with class '%s'", model.getClass()));
    }
    this.scope = getScope(model);
  }

  public static String getLocalXmlModelName(LocalXmlModel model) {
    VirtualFile absolutePath = model.getConfig().getVirtualFile();
    VirtualFile projectBasePath = model.getModule().getProject().getBaseDir();
    String relativePath = VfsUtilCore.getRelativePath(absolutePath, projectBasePath);
    return relativePath != null ? relativePath : absolutePath.getPath();
  }

  private static WScope getScope(LocalModel model) {
    ProjectFileIndex index = ProjectRootManager.getInstance(model.getModule().getProject()).getFileIndex();
    VirtualFile file = model.getConfig().getContainingFile().getVirtualFile();
    return index.isInSourceContent(file) ? WScope.SOURCE : index.isInTestSourceContent(file) ? WScope.TEST : index.isInLibrary(file) ? WScope.LIBRARY : WScope.UNKNOWN;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WModel model = (WModel) o;
    return this.name.equals(model.name) && this.type == model.type && this.dependencies.equals(model.dependencies);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    return (31 * ((31 * result) + this.type.hashCode())) + this.dependencies.hashCode();
  }
}
