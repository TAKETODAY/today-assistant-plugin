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

package cn.taketoday.assistant.app.mvc.importer;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.javaee.web.framework.WebFrameworkType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Nullable;

@Order(1000)
public class InfraMvcExternalSystemDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {
  private static final Key<Set<WebFacet>> NEW_FACETS = Key.create("NEW_FACETS");

  public com.intellij.openapi.externalSystem.model.Key<LibraryDependencyData> getTargetDataKey() {
    return ProjectKeys.LIBRARY_DEPENDENCY;
  }

  public void importData(Collection<? extends DataNode<LibraryDependencyData>> toImport, @Nullable ProjectData projectData, Project project, IdeModifiableModelsProvider modelsProvider) {
    Module module;
    if (DetectionExcludesConfiguration.getInstance(project).isExcludedFromDetection(WebFrameworkType.getInstance())) {
      return;
    }
    for (DataNode<LibraryDependencyData> node : toImport) {
      String externalName = node.getData().getExternalName();
      if (externalName.startsWith("cn.taketoday:today-web:")) {
        Set<String> paths = node.getData().getTarget().getPaths(LibraryPathType.BINARY);
        String path = ContainerUtil.getFirstItem(paths);
        if (path != null && (module = modelsProvider.findIdeModule(node.getData().getOwnerModule())) != null) {
          setupWebFacet(module, modelsProvider, node.getData().getOwner());
        }
      }
    }
  }

  public void onSuccessImport(Collection<DataNode<LibraryDependencyData>> imported, @Nullable ProjectData projectData, Project project, IdeModelsProvider modelsProvider) {
    Set<WebFacet> newFacets = project.getUserData(NEW_FACETS);
    if (!ContainerUtil.isEmpty(newFacets)) {
      for (WebFacet newFacet : newFacets) {
        Module facetModule = newFacet.getModule();
        Set<String> sourceRoots = ContainerUtil.newHashSet(ModuleRootManager.getInstance(facetModule).getSourceRootUrls(false));
        List<String> roots = newFacet.getWebSourceRootUrls();
        ApplicationManager.getApplication().invokeAndWait(() -> {
          for (String sourceRoot : sourceRoots) {
            if (!roots.contains(sourceRoot)) {
              newFacet.addWebSourceRoot(sourceRoot);
            }
          }
        });
      }
      project.putUserData(NEW_FACETS, null);
    }
  }

  public void onFailureImport(Project project) {
    project.putUserData(NEW_FACETS, null);
  }

  private static void setupWebFacet(Module module, IdeModifiableModelsProvider modelsProvider, ProjectSystemId externalSystemId) {
    WebFacetType facetType = WebFacetType.getInstance();
    ModifiableFacetModel facetModel = modelsProvider.getModifiableFacetModel(module);
    WebFacet existing = facetModel.getFacetByType(WebFacet.ID);
    if (existing != null) {
      return;
    }
    WebFacet webFacet = facetType.createFacet(module, "Web", facetType.createDefaultConfiguration(), null);
    facetModel.addFacet(webFacet, ExternalSystemApiUtil.toExternalSource(externalSystemId));
    Set<WebFacet> newFacets = module.getProject().getUserData(NEW_FACETS);
    if (newFacets == null) {
      newFacets = new LinkedHashSet<>();
      module.getProject().putUserData(NEW_FACETS, newFacets);
    }
    newFacets.add(webFacet);
  }
}
