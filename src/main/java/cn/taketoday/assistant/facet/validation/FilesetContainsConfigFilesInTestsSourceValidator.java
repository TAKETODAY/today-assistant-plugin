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

package cn.taketoday.assistant.facet.validation;

import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.SlowFacetEditorValidator;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFileSet;

public class FilesetContainsConfigFilesInTestsSourceValidator extends FacetEditorValidator implements SlowFacetEditorValidator {
  private static final int MAX_FILES_TO_SHOW = 5;
  private final Module myModule;
  private final Set<? extends InfraFileSet> myFileSets;

  public FilesetContainsConfigFilesInTestsSourceValidator(Module module, Set<? extends InfraFileSet> fileSets) {
    this.myModule = module;
    this.myFileSets = fileSets;
  }

  public ValidationResult check() {
    Project project = this.myModule.getProject();
    if (DumbService.isDumb(project)) {
      return ValidationResult.OK;
    }
    ModuleFileIndex index = ModuleRootManager.getInstance(this.myModule).getFileIndex();
    Set<VirtualFile> inTestSourceRoots = new LinkedHashSet<>();
    for (InfraFileSet set : this.myFileSets) {
      Iterable<VirtualFilePointer> configFiles = ContainerUtil.concat(set.getXmlFiles(), set.getCodeConfigurationFiles());
      for (VirtualFilePointer pointer : configFiles) {
        VirtualFile file = pointer.isValid() ? pointer.getFile() : null;
        if (file != null && index.isInTestSourceContent(file)) {
          inTestSourceRoots.add(file);
        }
      }
    }
    if (inTestSourceRoots.isEmpty()) {
      return ValidationResult.OK;
    }
    else if (inTestSourceRoots.size() > MAX_FILES_TO_SHOW) {
      return new ValidationResult(InfraBundle.message("fileset.contains.testing.configs.short", inTestSourceRoots.size()));
    }
    else {
      String filesText = StringUtil.join(inTestSourceRoots, VirtualFile::getPresentableName, "<br/>");
      return new ValidationResult(InfraBundle.message("fileset.contains.testing.configs.full", inTestSourceRoots.size(), filesText));
    }
  }
}
