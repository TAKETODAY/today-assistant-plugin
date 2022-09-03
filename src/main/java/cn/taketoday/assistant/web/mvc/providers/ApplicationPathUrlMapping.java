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

import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.utils.UrlMappingBuilder;
import com.intellij.pom.PomNamedTarget;
import com.intellij.psi.PsiElement;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.lang.Nullable;

public class ApplicationPathUrlMapping implements UrlMappingElement {
  private final String myAppPath;
  private final UrlMappingElement myUrlMappingElement;

  public ApplicationPathUrlMapping(String appPath, UrlMappingElement urlMappingElement) {
    this.myAppPath = appPath;
    this.myUrlMappingElement = urlMappingElement;
  }

  @Override
  public String getURL() {
    return new UrlMappingBuilder(this.myAppPath).appendSegment(this.myUrlMappingElement.getURL()).build();
  }

  @Override
  public UrlPath getUrlPath() {
    return InfraMvcUrlPathSpecification.INSTANCE.parsePath(getURL());
  }

  @Override
  @Nullable
  public PomNamedTarget getPomTarget() {
    return this.myUrlMappingElement.getPomTarget();
  }

  @Override
  @Nullable
  public PsiElement getNavigationTarget() {
    return this.myUrlMappingElement.getNavigationTarget();
  }

  @Override
  public String getPresentation() {
    return new UrlMappingBuilder(this.myAppPath).appendSegment(this.myUrlMappingElement.getPresentation()).build();
  }

  @Override
  public RequestMethod[] getMethod() {
    return this.myUrlMappingElement.getMethod();
  }

  @Override
  @Nullable
  public PsiElement getDocumentationPsiElement() {
    return this.myUrlMappingElement.getDocumentationPsiElement();
  }

  @Override
  public boolean isDefinedInBean(BeanPointer<? extends CommonInfraBean> pointer) {
    return this.myUrlMappingElement.isDefinedInBean(pointer);
  }
}
