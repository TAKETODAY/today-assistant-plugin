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

package cn.taketoday.assistant.app.application.yaml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.jsonSchema.extension.JsonWidgetSuppressor;

import org.jetbrains.yaml.psi.YAMLFile;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileNameContributor;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraApplicationYamlJsonWidgetSuppressor implements JsonWidgetSuppressor {

  public boolean isCandidateForSuppress(VirtualFile file, Project project) {
    Module module;
    if (!InfraUtils.hasFacets(project)) {
      return false;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (!(psiFile instanceof YAMLFile) || (module = ModuleUtilCore.findModuleForPsiElement(psiFile)) == null) {
      return false;
    }
    for (InfraModelConfigFileNameContributor fileNameContributor : InfraModelConfigFileNameContributor.EP_NAME.getExtensions()) {
      String configName = fileNameContributor.getInfraConfigName(module);
      String fileNameWithoutExtension = file.getNameWithoutExtension();
      if (fileNameWithoutExtension.equals(configName) || fileNameWithoutExtension.startsWith(configName + "-")) {
        return true;
      }
      for (String customConfigFileUrl : fileNameContributor.getCustomConfigFileUrls(module)) {
        if (customConfigFileUrl.endsWith(file.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean suppressSwitcherWidget(VirtualFile file, Project project) {
    if (!InfraUtils.hasFacets(project)) {
      return false;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    return (psiFile instanceof YAMLFile) && InfraConfigurationFileService.of().isApplicationConfigurationFile(psiFile);
  }
}
