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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.assistant.InfraInspectionsRegistry;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraAdditionalConfigInspectionsRegistryContributor implements InfraInspectionsRegistry.Contributor {
  public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
    return new Class[] { InfraAdditionalConfigInspection.class };
  }

  public static class Contributor extends InfraInspectionsRegistry.AdditionalFilesContributor {
    public Collection<VirtualFile> getAdditionalFilesToProcess(Project project, CompileContext context) {
      PsiPackage metaInfPackage;
      if (InfraLibraryUtil.hasFrameworkLibrary(project) && (metaInfPackage = JavaPsiFacade.getInstance(project).findPackage("META-INF")) != null) {
        GlobalSearchScope metaInfPackageScope = PackageScope.packageScopeWithoutLibraries(metaInfPackage, false);
        Collection<VirtualFile> additionalFiles = new LinkedHashSet<>();
        for (Module module : context.getCompileScope().getAffectedModules()) {
          if (InfraUtils.hasFacet(module)) {
            GlobalSearchScope moduleScope = module.getModuleScope(false).intersectWith(metaInfPackageScope);
            additionalFiles.addAll(FilenameIndex.getVirtualFilesByName(InfraConfigFileConstants.ADDITIONAL_SPRING_CONFIGURATION_METADATA_JSON, moduleScope));
          }
        }
        return additionalFiles;
      }
      return Collections.emptyList();
    }
  }
}
