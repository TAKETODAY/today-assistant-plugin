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
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.web.mvc.client.WebClientHolder;
import cn.taketoday.assistant.web.mvc.client.WebClientUrl;
import kotlin.collections.CollectionsKt;

public final class TestRestTemplateFramework extends RestTemplateFramework {
  private final SourceTestLibSearcher<WebClientHolder> myHoldersSearcher = new SourceTestLibSearcher<>(
          "SPRING_TEST_REST_TEMPLATE", TestRestTemplateModel.INSTANCE::findHolders);

  private final FrameworkPresentation presentation;

  public TestRestTemplateFramework() {
    String message = InfraAppBundle.message("test.rest.template.endpoints.view.title");
    this.presentation = new FrameworkPresentation("Infra-TestRestTemplate", message, Icons.SpringWeb);
  }

  public FrameworkPresentation getPresentation() {
    return this.presentation;
  }

  public EndpointsProvider.Status getStatus(Project project) {
    return !RestOperationsUtils.isTestRestTemplateAvailable(
            project) ? Status.UNAVAILABLE : TestRestTemplateModel.INSTANCE.hasUsages(project) ? Status.HAS_ENDPOINTS : Status.AVAILABLE;
  }

  public Iterable<WebClientHolder> getEndpointGroups(Project project, EndpointsFilter filter) {
    if ((filter instanceof ModuleEndpointsFilter) && RestOperationsUtils.isTestRestTemplateAvailable(((ModuleEndpointsFilter) filter).getModule())) {
      return this.myHoldersSearcher.iterable(((ModuleEndpointsFilter) filter).getModule(), ((ModuleEndpointsFilter) filter).getFromTests(), ((ModuleEndpointsFilter) filter).getFromLibraries());
    }
    return CollectionsKt.emptyList();
  }

  public Iterable<WebClientUrl> getEndpoints(WebClientHolder group) {
    return TestRestTemplateModel.INSTANCE.getEndpoints(group);
  }
}
