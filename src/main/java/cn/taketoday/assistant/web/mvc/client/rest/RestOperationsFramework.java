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

package cn.taketoday.assistant.web.mvc.client.rest;

import com.intellij.microservices.endpoints.EndpointsFilter;
import com.intellij.microservices.endpoints.EndpointsProvider;
import com.intellij.microservices.endpoints.FrameworkPresentation;
import com.intellij.microservices.endpoints.ModuleEndpointsFilter;
import com.intellij.microservices.jvm.cache.SourceTestLibSearcher;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.web.mvc.client.WebClientHolder;
import cn.taketoday.assistant.web.mvc.client.WebClientUrl;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;

public final class RestOperationsFramework extends RestTemplateFramework {
  private final SourceTestLibSearcher<WebClientHolder> myHoldersSearcher = new SourceTestLibSearcher<>("SPRING_REST_OPERATIONS",
          new Function2<Module, GlobalSearchScope, Collection<? extends WebClientHolder>>() {
            @Override
            public List<WebClientHolder> invoke(Module p1, GlobalSearchScope p2) {
              Intrinsics.checkNotNullParameter(p1, "p1");
              Intrinsics.checkNotNullParameter(p2, "p2");
              return RestOperationsModel.INSTANCE.findHolders(p1, p2);
            }
          });

  private final FrameworkPresentation presentation;

  public RestOperationsFramework() {
    String message = InfraAppBundle.message("rest.operations.endpoints.view.title");
    this.presentation = new FrameworkPresentation("Infra-RestOperations", message, Icons.SpringWeb);
  }

  public FrameworkPresentation getPresentation() {
    return this.presentation;
  }

  public EndpointsProvider.Status getStatus(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    return !RestOperationsUtils.isRestOperationsAvailable(
            project) ? Status.UNAVAILABLE : RestOperationsModel.INSTANCE.hasUsages(project) ? Status.HAS_ENDPOINTS : Status.AVAILABLE;
  }

  public Iterable<WebClientHolder> getEndpointGroups(Project project, EndpointsFilter filter) {
    if ((filter instanceof ModuleEndpointsFilter) && RestOperationsUtils.isRestOperationsAvailable(((ModuleEndpointsFilter) filter).getModule())) {
      return this.myHoldersSearcher.iterable(((ModuleEndpointsFilter) filter).getModule(), ((ModuleEndpointsFilter) filter).getFromTests(), ((ModuleEndpointsFilter) filter).getFromLibraries());
    }
    return CollectionsKt.emptyList();
  }

  public Iterable<WebClientUrl> getEndpoints(WebClientHolder group) {
    Intrinsics.checkNotNullParameter(group, "group");
    return RestOperationsModel.INSTANCE.getEndpoints(group);
  }
}
