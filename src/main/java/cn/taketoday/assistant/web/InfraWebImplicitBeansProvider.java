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

package cn.taketoday.assistant.web;

import com.intellij.javaee.utils.JavaeeType;
import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraImplicitBeansProviderBase;

final class InfraWebImplicitBeansProvider extends InfraImplicitBeansProviderBase {

  @Override
  public String getProviderName() {
    return "Infra Web";
  }

  @Override
  public boolean accepts(Module module) {
    return !ApplicationManager.getApplication().isUnitTestMode() || !WebFacet.getInstances(module).isEmpty();
  }

  @Override
  protected Collection<CommonInfraBean> getImplicitBeans(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Set<CommonInfraBean> beans = new HashSet<>(5);
      JavaeeType javaeeType = JavaeeType.discover(module, WebCommonClassNames.SERVLET_CONTEXT);
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_CONTEXT.fqn(javaeeType), "servletContext");
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_CONFIG.fqn(javaeeType), "servletConfig");
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_REQUEST.fqn(javaeeType), "httpServletRequest");
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SERVLET_REQUEST.fqn(javaeeType), "httpServletRequest");
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SERVLET_RESPONSE.fqn(javaeeType), "httpServletResponse");
      this.addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SESSION.fqn(javaeeType), "httpSession");
      this.addImplicitLibraryBean(beans, module, "cn.taketoday.web.context.WebApplicationContext", "webApplicationContext");
      return CachedValueProvider.Result.create(beans, this.getDependencies(module));
    });
  }
}
