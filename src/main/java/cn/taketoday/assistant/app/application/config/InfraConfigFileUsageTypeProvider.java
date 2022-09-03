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

package cn.taketoday.assistant.app.application.config;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;

import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.messagePointer;

class InfraConfigFileUsageTypeProvider implements UsageTypeProvider {
  private static final UsageType CONFIGURATION_FILE_USAGE_TYPE = new UsageType(messagePointer("application.config.usage.type"));
  private static final UsageType ADDITIONAL_METADATA_USAGE_TYPE = new UsageType(messagePointer("additional.config.usage.type"));

  @Nullable
  public UsageType getUsageType(PsiElement element) {
    Module module;
    VirtualFile virtualFile;
    Project project = element.getProject();
    if (!InfraUtils.hasFacets(project)
            || !InfraLibraryUtil.hasFrameworkLibrary(project)
            || (module = ModuleUtilCore.findModuleForPsiElement(
            element)) == null || (virtualFile = element.getContainingFile().getVirtualFile()) == null) {
      return null;
    }
    if (virtualFile.getFileType() == JsonFileType.INSTANCE) {
      if (virtualFile.getName().equals(InfraConfigFileConstants.ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON)) {
        return ADDITIONAL_METADATA_USAGE_TYPE;
      }
      return null;
    }
    List<VirtualFile> configFiles = InfraConfigurationFileService.of().findConfigFilesWithImports(module, true);
    if (configFiles.contains(virtualFile)) {
      return CONFIGURATION_FILE_USAGE_TYPE;
    }
    return null;
  }
}
