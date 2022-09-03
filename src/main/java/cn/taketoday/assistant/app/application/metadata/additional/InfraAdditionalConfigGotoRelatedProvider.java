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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.json.psi.JsonFile;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraAdditionalConfigGotoRelatedProvider extends GotoRelatedProvider {

  public List<? extends GotoRelatedItem> getItems(PsiElement psiElement) {
    Project project = psiElement.getProject();
    if (!InfraUtils.hasFacets(project) || !InfraLibraryUtil.hasFrameworkLibrary(project)) {
      return Collections.emptyList();
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) {
      return Collections.emptyList();
    }
    InfraAdditionalConfigUtils additionalConfigUtils = new InfraAdditionalConfigUtils(module);
    if (!additionalConfigUtils.hasResourceRoots()) {
      return Collections.emptyList();
    }
    PsiFile file = psiElement.getContainingFile();
    if (file == null) {
      return ContainerUtil.emptyList();
    }
    VirtualFile virtualFile = file.getVirtualFile();
    if (InfraConfigurationFileService.of().findConfigFilesWithImports(module, true).contains(virtualFile)) {
      CommonProcessors.CollectProcessor<JsonFile> collectProcessor = new CommonProcessors.CollectProcessor<>();
      additionalConfigUtils.processAdditionalMetadataFiles(collectProcessor);
      return GotoRelatedItem.createItems(collectProcessor.getResults(),
              InfraAppBundle.message("infra.metadata.goto.related.item.group.name"));
    }
    return Collections.emptyList();
  }
}
