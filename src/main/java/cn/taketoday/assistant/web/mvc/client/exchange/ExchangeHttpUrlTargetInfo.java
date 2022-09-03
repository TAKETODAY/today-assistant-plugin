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

import com.intellij.microservices.jvm.url.UastUrlAttributeUtils;
import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlQueryParameter;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlResolverKt;
import cn.taketoday.lang.Nullable;
import kotlin.collections.SetsKt;

final class ExchangeHttpUrlTargetInfo implements UrlTargetInfo {

  private final List<String> schemes;

  private final UrlPath path;
  private final InfraExchangeClient group;
  private final InfraExchangeMapping<?> endpoint;

  private final List<Authority> authorities;

  public List<String> getSchemes() {
    return this.schemes;
  }

  public ExchangeHttpUrlTargetInfo(List<String> list, UrlPath path, InfraExchangeClient group,
          InfraExchangeMapping<?> infraExchangeMapping, List<Authority> list2) {
    this.schemes = list;
    this.path = path;
    this.group = group;
    this.endpoint = infraExchangeMapping;
    this.authorities = list2;
  }

  public UrlPath getPath() {
    return this.path;
  }

  public List<Authority> getAuthorities() {
    return this.authorities;
  }

  public Icon getIcon() {
    return Icons.RequestMapping;
  }

  public boolean isDeprecated() {
    return UastUrlAttributeUtils.isUastDeclarationDeprecated(resolveToPsiElement());
  }

  @Nullable
  public PsiElement resolveToPsiElement() {
    if (!this.endpoint.isValid()) {
      return null;
    }
    return this.endpoint.getPsiElement();
  }

  public Set<String> getMethods() {
    return SetsKt.setOfNotNull(this.endpoint.getHttpMethod());
  }

  public String getSource() {
    PsiClass psiElement = this.group.getPsiElement();
    String name = psiElement.getName();
    return name != null ? name : "";
  }

  public Iterable<UrlQueryParameter> getQueryParameters() {
    return WebMvcUrlResolverKt.queryParameterSupport.getParametersFromDeclarationContext(resolveToPsiElement());
  }
}
