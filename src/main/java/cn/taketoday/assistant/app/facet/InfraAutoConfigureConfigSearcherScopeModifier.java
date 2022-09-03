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

package cn.taketoday.assistant.app.facet;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.spi.InfraImportsManager;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.facet.searchers.ConfigSearcherScopeModifier;
import cn.taketoday.assistant.service.InfraJamService;

public class InfraAutoConfigureConfigSearcherScopeModifier extends ConfigSearcherScopeModifier {

  public GlobalSearchScope modifyScope(Module module, GlobalSearchScope originalScope) {
    if (!InfraLibraryUtil.hasFrameworkLibrary(module)) {
      return originalScope;
    }
    List<PsiClass> autoConfigs = InfraImportsManager.getInstance(module).getAutoConfigurationClasses(true);
    if (autoConfigs.isEmpty()) {
      return originalScope;
    }
    Set<VirtualFile> allConfigFiles = new HashSet<>(autoConfigs.size());
    for (PsiClass psiClass : autoConfigs) {
      processPsiClass(allConfigFiles, psiClass);
    }
    GlobalSearchScope configFilesScope = GlobalSearchScope.filesScope(module.getProject(), allConfigFiles);
    return originalScope.intersectWith(GlobalSearchScope.notScope(configFilesScope));
  }

  private static void processPsiClass(Set<VirtualFile> allConfigFiles, PsiClass psiClass) {
    Configuration springConfiguration = Configuration.META.getJamElement(psiClass);
    if (springConfiguration == null) {
      return;
    }
    SmartList<PsiClass> smartList = new SmartList<>(psiClass);
    addImports(smartList, psiClass);
    addXmlImports(allConfigFiles, psiClass);
    InfraJamService.of().processCustomAnnotations(psiClass, pair -> {
      PsiClass enableAnno = pair.first;
      addImports(smartList, enableAnno);
      addXmlImports(allConfigFiles, enableAnno);
      return true;
    });
    allConfigFiles.addAll(ContainerUtil.map(smartList, psiClass1 -> {
      return psiClass1.getContainingFile().getVirtualFile();
    }));
    for (PsiClass innerClass : psiClass.getInnerClasses()) {
      if (innerClass.hasModifierProperty("static")) {
        processPsiClass(allConfigFiles, innerClass);
      }
    }
  }

  private static void addXmlImports(Set<VirtualFile> configFiles, PsiClass psiClass) {
    Set<XmlFile> xmlConfigs = InfraJamService.of().getImportedResources(psiClass);
    configFiles.addAll(ContainerUtil.map(xmlConfigs, PsiFile::getVirtualFile));
  }

  private static void addImports(List<PsiClass> allPsiClasses, PsiClass psiClass) {
    Set<PsiClass> importedClasses = InfraJamService.of().getImportedClasses(psiClass, null);
    allPsiClasses.addAll(importedClasses);
  }
}
