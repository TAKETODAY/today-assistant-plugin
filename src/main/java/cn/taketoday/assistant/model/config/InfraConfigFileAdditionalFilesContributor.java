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

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.assistant.InfraInspectionsRegistry;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/31 02:02
 */
public class InfraConfigFileAdditionalFilesContributor
        extends InfraInspectionsRegistry.AdditionalFilesContributor {

  public Collection<VirtualFile> getAdditionalFilesToProcess(Project project, CompileContext context) {
    if (!InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return Collections.emptyList();
    }
    else {
      Collection<VirtualFile> additionalFiles = new LinkedHashSet<>();
      Module[] affectedModules = context.getCompileScope().getAffectedModules();

      for (Module module : affectedModules) {
        if (InfraUtils.hasFacet(module)) {
          addConfigurationFiles(additionalFiles, module);
        }
      }

      return additionalFiles;
    }
  }

  private static void addConfigurationFiles(Collection<VirtualFile> additionalFiles, Module module) {
    additionalFiles.addAll(InfraConfigurationFileService.of().findConfigFilesWithImports(module, true));
  }
}