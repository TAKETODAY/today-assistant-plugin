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

package cn.taketoday.assistant.web.mvc.providers;

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.microservices.endpoints.EndpointsFilter;
import com.intellij.microservices.endpoints.EndpointsProvider;
import com.intellij.microservices.endpoints.ModuleEndpointsFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.WebMvcLibraryUtil;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;
import cn.taketoday.lang.Nullable;

public final class InfraMvcControllersEndpointsProvider extends WebMvcServerProvider {

  public InfraMvcControllersEndpointsProvider() {
    super("Web-MVC-Controllers", "Web MVC Controllers");
  }

  public EndpointType getEndpointType() {
    return EndpointTypes.HTTP_SERVER_TYPE;
  }

  public EndpointsProvider.Status getStatus(Project project) {
    if (!InfraLibraryUtil.hasWebMvcLibrary(project) && !WebMvcLibraryUtil.hasWebfluxLibrary(project)) {
      return Status.UNAVAILABLE;
    }
    else if (InfraLibraryUtil.isWebMVCEnabled(project)) {
      return Status.HAS_ENDPOINTS;
    }
    else {
      return Status.AVAILABLE;
    }
  }

  public Iterable<BeanPointer<?>> getEndpointGroups(Project project, EndpointsFilter filter) {
    if (!(filter instanceof ModuleEndpointsFilter moduleFilter)) {
      return ContainerUtil.emptyList();
    }
    Module module = moduleFilter.getModule();
    if (!InfraLibraryUtil.hasWebMvcLibrary(module) && !WebMvcLibraryUtil.hasWebfluxLibrary(module)) {
      return ContainerUtil.emptyList();
    }
    Collection<BeanPointer<?>> allControllers = WebMvcService.getInstance().getBeanControllers(module);
    return moduleFilter.filterByScope(allControllers, BeanPointer::getContainingFile);
  }

  public ModificationTracker getModificationTracker(Project project) {
    return InfraModificationTrackersManager.from(project).getEndpointsModificationTracker();
  }

  @Override
  @Nullable
  public Object getEndpointData(BeanPointer<?> group, UrlMappingElement endpoint, String dataId) {
    if (EndpointsProvider.OPENAPI_PATH.is(dataId)) {
      return WebMvcOasConverters.getMvcHandlerOasModel(endpoint, getUrlTargetInfo(group, endpoint));
    }
    return super.getEndpointData(group, endpoint, dataId);
  }
}
