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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.microservices.jvm.url.UastUrlAttributeUtils;
import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlQueryParameter;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.psi.PsiElement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.lang.Nullable;

public final class WebMvcUrlTargetInfo implements UrlTargetInfo {

  private final UrlPath path;

  private final Set<String> methods;

  private final List<String> schemes;

  private final UrlMappingElement urlMapping;

  private final List<Authority> authorities;

  public WebMvcUrlTargetInfo(List<String> list, UrlMappingElement urlMapping, List<Authority> list2) {
    this.schemes = list;
    this.urlMapping = urlMapping;
    this.authorities = list2;
    UrlPath urlPath = this.urlMapping.getUrlPath();
    this.path = urlPath;
    RequestMethod[] method = this.urlMapping.getMethod();
    var hashSet = new LinkedHashSet<String>();
    for (RequestMethod requestMethod : method) {
      String upperCase = requestMethod.name().toUpperCase(Locale.ROOT);
      hashSet.add(upperCase);
    }
    this.methods = hashSet;
  }

  public List<String> getSchemes() {
    return this.schemes;
  }

  public UrlMappingElement getUrlMapping() {
    return this.urlMapping;
  }

  public List<Authority> getAuthorities() {
    return this.authorities;
  }

  public UrlPath getPath() {
    return this.path;
  }

  public Icon getIcon() {
    return Icons.RequestMapping;
  }

  public boolean isDeprecated() {
    return UastUrlAttributeUtils.isUastDeclarationDeprecated(resolveToPsiElement());
  }

  @Nullable
  public PsiElement resolveToPsiElement() {
    return this.urlMapping.getNavigationTarget();
  }

  public Set<String> getMethods() {
    return this.methods;
  }

  public String getSource() {
    return UastUrlAttributeUtils.getUastDeclaringLocation(resolveToPsiElement());
  }

  @Nullable
  public PsiElement getDocumentationPsiElement() {
    return this.urlMapping.getDocumentationPsiElement();
  }

  public Iterable<UrlQueryParameter> getQueryParameters() {
    return WebMvcUrlResolverKt.getQueryParameterSupport().getParametersFromDeclarationContext(resolveToPsiElement());
  }
}
