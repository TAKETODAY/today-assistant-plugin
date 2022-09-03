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

import com.intellij.microservices.endpoints.EndpointsProvider;
import com.intellij.microservices.endpoints.FrameworkPresentation;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.microservices.utils.EndpointsViewUtils;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.ValueKey;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.InfraMvcUrlResolver;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlTargetInfo;
import cn.taketoday.assistant.web.mvc.model.mappings.UrlMappingPsiBasedElement;
import cn.taketoday.assistant.web.mvc.toolwindow.WebMvcViewUtils;
import cn.taketoday.assistant.web.mvc.utils.WebMvcUrlUtils;
import cn.taketoday.lang.Nullable;

public abstract class WebMvcServerProvider implements EndpointsProvider<BeanPointer<?>, UrlMappingElement> {
  private final FrameworkPresentation myPresentation;

  public WebMvcServerProvider(String queryTag, String title) {
    this.myPresentation = new FrameworkPresentation(queryTag, title, Icons.SpringWeb);
  }

  public FrameworkPresentation getPresentation() {
    FrameworkPresentation frameworkPresentation = this.myPresentation;
    return frameworkPresentation;
  }

  public boolean isValidEndpoint(BeanPointer<?> group, UrlMappingElement endpoint) {
    PsiElement element = endpoint.getNavigationTarget();
    return element != null && element.isValid();
  }

  @Nullable
  protected String getLocationString(BeanPointer<?> beanPointer, UrlMappingElement endpoint) {
    PsiClass beanClass = beanPointer.getBeanClass();
    return beanClass != null ? beanClass.getName() : beanPointer.getContainingFile().getName();
  }

  public ItemPresentation getEndpointPresentation(BeanPointer<?> group, UrlMappingElement endpoint) {
    String pathPresentation = UrlMappingElement.getPathPresentation(endpoint);
    UrlTargetInfo urlTargetInfo = getUrlTargetInfo(group, endpoint);
    boolean deprecated = urlTargetInfo != null && urlTargetInfo.isDeprecated();
    return new HttpMethodPresentation(
            pathPresentation, ContainerUtil.map(endpoint.getMethod(), Enum::name), getLocationString(group, endpoint), Icons.RequestMapping,
            deprecated ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null);
  }

  @Override
  @Nullable
  public Object getEndpointData(BeanPointer<?> group, UrlMappingElement endpoint, String dataId) {
    Object data = ValueKey.match(dataId)
            .ifEq(EndpointsProvider.URL_TARGET_INFO)
            .thenGet(() -> {
              UrlTargetInfo urlTargetInfo = this.getUrlTargetInfo(group, endpoint);
              return urlTargetInfo == null ? null : Collections.singletonList(urlTargetInfo);
            })
            .ifEq(EndpointsProvider.DOCUMENTATION_ELEMENT)
            .thenGet(endpoint::getDocumentationPsiElement).orNull();
    return data != null ? data : EndpointsViewUtils.getCommonEndpointValue(endpoint.getNavigationTarget(), dataId);
  }

  @Nullable
  protected UrlTargetInfo getUrlTargetInfo(BeanPointer<?> group, UrlMappingElement endpoint) {
    String path = UrlMappingElement.getPathPresentation(endpoint);
    PsiElement endpointPsiElement = endpoint.getNavigationTarget();
    PomTarget pomTarget = null;
    if (endpointPsiElement instanceof PomTargetPsiElement) {
      pomTarget = ((PomTargetPsiElement) endpointPsiElement).getTarget();
      endpointPsiElement = endpointPsiElement.getNavigationElement();
    }

    if (endpointPsiElement == null) {
      return null;
    }
    else {
      UrlMappingPsiBasedElement mapping = new UrlMappingPsiBasedElement(path, endpointPsiElement, pomTarget, endpoint.getPresentation(), endpoint.getMethod());
      Module module = ObjectUtils.doIfNotNull(group.getPsiElement(), ModuleUtilCore::findModuleForPsiElement);
      return new WebMvcUrlTargetInfo(UrlConstants.HTTP_SCHEMES, mapping, WebMvcUrlUtils.getAuthoritiesByModule(module));
    }
  }

  public Iterable<UrlMappingElement> getEndpoints(BeanPointer<?> group) {
    Module module = ModuleUtilCore.findModuleForPsiElement(group.getContainingFile());
    if (module == null) {
      return ContainerUtil.emptyList();
    }
    List<UrlMappingElement> items = new ArrayList<>();
    WebMvcViewUtils.processUrls(module, group, EnumSet.allOf(RequestMethod.class), new CommonProcessors.CollectProcessor(items));
    return InfraMvcUrlResolver.getAppPathUrlMappingElements(module, items);
  }
}
