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

package cn.taketoday.assistant.web.mvc.config.anno;

import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.CommonServletMapping;
import com.intellij.javaee.web.WebModelContributor;
import com.intellij.openapi.module.Module;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;
import cn.taketoday.assistant.web.mvc.services.WebMvcServiceImpl;

public class WebMvcPsiBasedServletWebModelContributor extends WebModelContributor {

  private static final Function<InfraModel, CommonServlet> SERVLET_CONTEXT_SERVLET_MAPPING = model -> {
    InfraFileSet fileSet = model.getFileSet();
    if (!(fileSet instanceof ServletFileSet servletFileSet)) {
      return null;
    }
    CommonServlet servlet = servletFileSet.getServlet();
    if (servlet instanceof PsiBasedServlet) {
      return servlet;
    }
    return null;
  };
  private static final Function<CommonServlet, CommonServletMapping<CommonServlet>> SERVLET_MAPPING_FUNCTION = servlet -> {
    if (!(servlet instanceof CommonServletMapping)) {
      return null;
    }
    return (CommonServletMapping) servlet;
  };

  public List<CommonServlet> getServlets(Module module) {
    if (!InfraUtils.hasFacets(module.getProject())) {
      return Collections.emptyList();
    }
    InfraFacet springFacet = InfraFacet.from(module);
    if (springFacet == null) {
      return Collections.emptyList();
    }
    if (!InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return Collections.emptyList();
    }
    return ContainerUtil.mapNotNull(WebMvcServiceImpl.getServletModels(module), SERVLET_CONTEXT_SERVLET_MAPPING);
  }

  public List<CommonServletMapping<CommonServlet>> getServletMappings(Module module) {
    return ContainerUtil.mapNotNull(getServlets(module), SERVLET_MAPPING_FUNCTION);
  }
}
