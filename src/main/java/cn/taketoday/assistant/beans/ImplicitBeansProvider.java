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

package cn.taketoday.assistant.beans;

import com.intellij.javaee.utils.JavaeeType;
import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringImplicitBeansProviderBase;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 15:30
 */
public class ImplicitBeansProvider extends SpringImplicitBeansProviderBase {

  @Override
  public boolean accepts(Module module) {
    return true;
  }

  @Override
  public String getProviderName() {
    return "TODAY Infrastructure";
  }

  @Override
  protected Collection<CommonSpringBean> getImplicitBeans(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      ArrayList<CommonSpringBean> beans = new ArrayList<>(3);
      addImplicitLibraryBean(beans, module, "cn.taketoday.core.env.Environment", "environment");
      addImplicitLibraryBean(beans, module, "cn.taketoday.core.env.PropertyResolver", "propertyResolver");
      addImplicitLibraryBean(beans, module, "cn.taketoday.core.conversion.ConversionService", "conversionService");

      // servlet web
      if (!ApplicationManager.getApplication().isUnitTestMode()
              || !WebFacet.getInstances(module).isEmpty()) {

        JavaeeType javaeeType = JavaeeType.discover(module, WebCommonClassNames.SERVLET_CONTEXT);
        addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_CONTEXT.fqn(javaeeType), "servletContext");
        addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_CONFIG.fqn(javaeeType), "servletConfig");
        addImplicitLibraryBean(beans, module, WebCommonClassNames.SERVLET_REQUEST.fqn(javaeeType), "httpServletRequest");
        addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SERVLET_REQUEST.fqn(javaeeType), "httpServletRequest");
        addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SERVLET_RESPONSE.fqn(javaeeType), "httpServletResponse");
        addImplicitLibraryBean(beans, module, WebCommonClassNames.HTTP_SESSION.fqn(javaeeType), "httpSession");
      }
      // TODO today-framework detection
      addImplicitLibraryBean(beans, module, "cn.taketoday.framework.ApplicationArguments", "applicationArguments");

      return CachedValueProvider.Result.create(beans, getDependencies(module));
    });
  }
}
