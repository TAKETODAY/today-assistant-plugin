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

import com.intellij.microservices.endpoints.EndpointType;
import com.intellij.microservices.endpoints.EndpointTypes;
import com.intellij.microservices.endpoints.EndpointsProvider;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.microservices.utils.EndpointsViewUtils;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.ValueKey;
import com.intellij.psi.PsiElement;
import com.intellij.uast.UastModificationTracker;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.function.Supplier;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.web.mvc.client.WebClientHolder;
import cn.taketoday.assistant.web.mvc.client.WebClientUrl;
import cn.taketoday.lang.Nullable;

public abstract class RestTemplateFramework implements EndpointsProvider<WebClientHolder, WebClientUrl> {

  public EndpointType getEndpointType() {
    return EndpointTypes.HTTP_CLIENT_TYPE;
  }

  public ItemPresentation getEndpointPresentation(WebClientHolder group, WebClientUrl endpoint) {
    return new HttpMethodPresentation(endpoint.getUriPresentation(),
            endpoint.getHttpMethod(), group.getName(), Icons.SpringWeb, null);
  }

  @Nullable
  public Object getEndpointData(WebClientHolder group, WebClientUrl endpoint, String dataId) {
    ValueKey.BeforeIf thenGet = ValueKey.match(dataId).ifEq(EndpointsProvider.DOCUMENTATION_ELEMENT).thenGet(new Supplier() {
      @Override
      public PsiElement get() {
        UElement uElement = UastContextKt.toUElement(endpoint.getPsiElement());
        UElement parent = uElement != null ? UastUtils.getParentOfType(uElement, true, UMethod.class, UClass.class) : null;
        if (parent != null) {
          return parent.getSourcePsi();
        }
        return null;
      }
    });
    return EndpointsViewUtils.orCheckCommonEndpointKeys(thenGet, endpoint.getPsiElement());
  }

  public boolean isValidEndpoint(WebClientHolder group, WebClientUrl endpoint) {
    return endpoint.isValid();
  }

  public ModificationTracker getModificationTracker(Project project) {
    return UastModificationTracker.Companion.getInstance(project);
  }
}
