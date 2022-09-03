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

package cn.taketoday.assistant.web;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JspPsiUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.util.InfraUtils;

final class InfraWebFileReferenceHelper extends FileReferenceHelper {

  public Collection<PsiFileSystemItem> getRoots(Module module) {
    return getWebRoots(module);
  }

  public Collection<PsiFileSystemItem> getContexts(Project project, VirtualFile file) {
    if (!isMine(project, file)) {
      return Collections.emptyList();
    }
    Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null) {
      return Collections.emptyList();
    }
    return getWebRoots(module);
  }

  public boolean isMine(Project project, VirtualFile file) {
    if (FileBasedIndex.getInstance().getFileBeingCurrentlyIndexed() != null) {
      return false;
    }
    if (!DumbService.isDumb(project) && !InfraLibraryUtil.hasLibrary(project)) {
      return false;
    }
    PsiFile findFile = PsiManager.getInstance(project).findFile(file);
    if (JamCommonUtil.isPlainXmlFile(findFile)) {
      return InfraDomUtils.isInfraXml((XmlFile) findFile);
    }
    if (!(findFile instanceof PsiClassOwner psiClassOwner) || JspPsiUtil.isInJspFile(findFile)) {
      return false;
    }
    for (PsiClass psiClass : psiClassOwner.getClasses()) {
      if (InfraUtils.isConfigurationOrMeta(psiClass)) {
        return true;
      }
    }
    return false;
  }

  private static Collection<PsiFileSystemItem> getWebRoots(Module module) {
    Collection<WebFacet> webFacets = WebFacet.getInstances(module);
    if (webFacets.isEmpty()) {
      return Collections.emptyList();
    }
    LinkedHashSet<PsiFileSystemItem> webRoots = new LinkedHashSet<>();
    for (WebFacet facet : webFacets) {
      for (WebRoot root : facet.getWebRoots()) {
        VirtualFile rootFile = root.getFile();
        if (rootFile != null) {
          WebDirectoryElement webDirectory = WebUtil.getWebUtil().findWebDirectoryByFile(rootFile, facet);
          ContainerUtil.addIfNotNull(webRoots, webDirectory);
        }
      }
    }
    return webRoots;
  }
}
