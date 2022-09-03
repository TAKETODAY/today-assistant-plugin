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

package cn.taketoday.assistant.web.mvc.client.exchange;

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.microservices.endpoints.EndpointsFilter;
import com.intellij.microservices.endpoints.EndpointsProvider;
import com.intellij.microservices.endpoints.FrameworkPresentation;
import com.intellij.microservices.endpoints.ModuleEndpointsFilter;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.microservices.jvm.cache.SourceTestLibSearcher;
import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.microservices.url.references.UrlPksParser;
import com.intellij.microservices.utils.EndpointsViewUtils;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.ValueKey;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.uast.UastModificationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.assistant.Icons;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class InfraExchangeEndpointsProvider
        implements EndpointsProvider<InfraExchangeClient, InfraExchangeMapping<?>> {

  private final SourceTestLibSearcher<InfraExchangeClient> clientsSearcher = new SourceTestLibSearcher<>("Infra_EXCHANGE_CLIENTS",
          InfraExchangeClientModelKt::findExchangeClients);

  private final FrameworkPresentation presentation = new FrameworkPresentation("Infra-HTTP-Exchange", "Infra HTTP Exchange", Icons.RequestMapping);

  public EndpointType getEndpointType() {
    return EndpointTypes.HTTP_CLIENT_TYPE;
  }

  public FrameworkPresentation getPresentation() {
    return this.presentation;
  }

  public EndpointsProvider.Status getStatus(Project project) {
    return InfraExchangeClientModelKt.isExchangeClientAvailable(project) ? Status.AVAILABLE : Status.UNAVAILABLE;
  }

  public Iterable<InfraExchangeClient> getEndpointGroups(Project project, EndpointsFilter filter) {
    if ((filter instanceof ModuleEndpointsFilter) && InfraExchangeClientModelKt.isExchangeClientAvailable(
            ((ModuleEndpointsFilter) filter).getModule())) {
      return this.clientsSearcher.iterable(((ModuleEndpointsFilter) filter).getModule(), ((ModuleEndpointsFilter) filter).getFromTests(), ((ModuleEndpointsFilter) filter).getFromLibraries());
    }
    return CollectionsKt.emptyList();
  }

  public ModificationTracker getModificationTracker(Project project) {
    Intrinsics.checkNotNullParameter(project, "project");
    return UastModificationTracker.Companion.getInstance(project);
  }

  @Nullable
  public Object getEndpointData(InfraExchangeClient group,
          InfraExchangeMapping<?> infraExchangeMapping, String dataId) {
    ValueKey.BeforeIf thenGet = ValueKey.match(dataId).ifEq(EndpointsProvider.URL_TARGET_INFO).thenGet(new Supplier() {
      @Override
      public Iterable<UrlTargetInfo> get() {
        Iterable<UrlTargetInfo> urlTargetInfo;
        urlTargetInfo = InfraExchangeEndpointsProvider.this.getUrlTargetInfo(group, infraExchangeMapping);
        return urlTargetInfo;
      }
    });
    return EndpointsViewUtils.orCheckCommonEndpointKeys(thenGet, infraExchangeMapping.getPsiElement());
  }

  public ItemPresentation getEndpointPresentation(InfraExchangeClient group,
          InfraExchangeMapping<?> infraExchangeMapping) {
    String fullUrlPath = InfraExchangeClientModelKt.getFullUrlPath(group, infraExchangeMapping);
    String httpMethod = infraExchangeMapping.getHttpMethod();
    PsiClass psiElement = group.getPsiElement();
    Intrinsics.checkNotNullExpressionValue(psiElement, "group.psiElement");
    return new HttpMethodPresentation(fullUrlPath, httpMethod, psiElement.getName(), Icons.RequestMapping, null);
  }

  public boolean isValidEndpoint(InfraExchangeClient group, InfraExchangeMapping<?> infraExchangeMapping) {
    return infraExchangeMapping.isValid();
  }

  public Iterable<InfraExchangeMapping<?>> getEndpoints(InfraExchangeClient group) {
    return InfraExchangeClientModelKt.getExchangeClientEndpoints(group);
  }

  public Iterable<UrlTargetInfo> getUrlTargetInfo(InfraExchangeClient group, InfraExchangeMapping<?> endpoint) {
    String fullUrlString = InfraExchangeClientModelKt.getFullUrlPath(group, endpoint);
    if (fullUrlString == null) {
      return CollectionsKt.emptyList();
    }
    else {
      UrlPksParser.ParsedPksUrl fullUrl = InfraExchangeUrlPathSpecification.INSTANCE.getParser().parseFullUrl(new PartiallyKnownString(fullUrlString));
      UrlPath urlPath = fullUrl.getUrlPath();
      PartiallyKnownString var22 = fullUrl.getAuthority();
      List<String> $this$map$iv = CollectionsKt.listOfNotNull(var22 != null ? var22.getValueIfKnown() : null);
      ArrayList<Authority> authorities = new ArrayList<>(Math.max($this$map$iv.size(), 10));

      for (String it : $this$map$iv) {
        Authority.Exact var17 = new Authority.Exact(it);
        authorities.add(var17);
      }

      label40:
      {
        var22 = fullUrl.getScheme();
        if (var22 != null) {
          PartiallyKnownString var19 = var22;
          var22 = !var19.getSegments().isEmpty() ? var19 : null;
          if (var22 != null) {
            fullUrlString = var22.getValueIfKnown();
            break label40;
          }
        }

        fullUrlString = null;
      }

      List var20 = CollectionsKt.listOfNotNull(fullUrlString);
      List var23 = !var20.isEmpty() ? var20 : null;
      if (var23 == null) {
        var23 = UrlConstants.HTTP_SCHEMES;
      }

      List schemes = var23;
      return CollectionsKt.listOf(new ExchangeHttpUrlTargetInfo(schemes, urlPath, group, endpoint, authorities));
    }
  }
}
