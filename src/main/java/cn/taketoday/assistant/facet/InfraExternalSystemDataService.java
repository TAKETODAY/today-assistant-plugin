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

package cn.taketoday.assistant.facet;

import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Set;

import cn.taketoday.lang.Nullable;

@Order(1000)
public class InfraExternalSystemDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {

  public Key<LibraryDependencyData> getTargetDataKey() {
    return ProjectKeys.LIBRARY_DEPENDENCY;
  }

  @Override
  public void importData(Collection<? extends DataNode<LibraryDependencyData>> toImport,
          @Nullable ProjectData projectData, Project project, IdeModifiableModelsProvider modelsProvider) {
    if (DetectionExcludesConfiguration.getInstance(project)
            .isExcludedFromDetection(InfraFrameworkDetector.getSpringFrameworkType())) {
      return;
    }

    Module module;
    for (DataNode<LibraryDependencyData> node : toImport) {
      String externalName = node.getData().getExternalName();
      if (externalName.startsWith("cn.taketoday:today-context:")) {
        Set<String> paths = node.getData().getTarget().getPaths(LibraryPathType.BINARY);
        String path = ContainerUtil.getFirstItem(paths);
        if (path != null && (module = modelsProvider.findIdeModule(node.getData().getOwnerModule())) != null) {
          setupFacet(module, modelsProvider, node.getData().getOwner());
        }
      }
    }

  }

  private static void setupFacet(Module module, IdeModifiableModelsProvider modelsProvider, ProjectSystemId externalSystemId) {
    FacetType<InfraFacet, InfraFacetConfiguration> facetType = InfraFacet.getFacetType();
    ModifiableFacetModel facetModel = modelsProvider.getModifiableFacetModel(module);
    InfraFacet existing = facetModel.getFacetByType(facetType.getId());
    if (existing != null) {
      return;
    }
    InfraFacet facet = facetType.createFacet(module, facetType.getDefaultFacetName(), facetType.createDefaultConfiguration(), null);
    facetModel.addFacet(facet, ExternalSystemApiUtil.toExternalSource(externalSystemId));
  }
}
