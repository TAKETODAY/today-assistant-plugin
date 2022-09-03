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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiVariable;

import cn.taketoday.assistant.web.mvc.MvcJakartaSessionAttributesProvider;
import cn.taketoday.assistant.web.mvc.MvcJavaxSessionAttributesProvider;
import cn.taketoday.assistant.web.mvc.MvcServletJakartaContextAttributesProvider;
import cn.taketoday.assistant.web.mvc.MvcServletJavaxContextAttributesProvider;
import cn.taketoday.assistant.web.mvc.WebMvcJakartaRequestAttributesProvider;
import cn.taketoday.assistant.web.mvc.WebMvcJavaxRequestAttributesProvider;
import cn.taketoday.assistant.web.mvc.WebMvcRequestAttributeAnnotationProvider;
import cn.taketoday.assistant.web.mvc.WebMvcSessionAttributeAnnotationProvider;
import cn.taketoday.assistant.web.mvc.WebMvcSessionAttributesAnnotationProvider;

public final class WebMvcVariablesServiceImpl implements WebMvcVariablesService {

  private final CachingVariableProvider applicationVariablesProvider = new CachingVariableProvider(
          MvcServletJavaxContextAttributesProvider.INSTANCE,
          MvcServletJakartaContextAttributesProvider.INSTANCE
  );
  private final CachingVariableProvider sessionVariablesProvider = new CachingVariableProvider(
          MvcJavaxSessionAttributesProvider.INSTANCE,
          MvcJakartaSessionAttributesProvider.INSTANCE,
          WebMvcSessionAttributesAnnotationProvider.INSTANCE,
          WebMvcSessionAttributeAnnotationProvider.INSTANCE
  );
  private final CachingVariableProvider requestVariablesProvider = new CachingVariableProvider(
          WebMvcJavaxRequestAttributesProvider.INSTANCE,
          WebMvcJakartaRequestAttributesProvider.INSTANCE,
          WebMvcRequestAttributeAnnotationProvider.INSTANCE
  );

  @Override
  public Iterable<PsiVariable> getApplicationVariables(Project project) {
    return this.applicationVariablesProvider.getVariables(project);
  }

  @Override
  public Iterable<PsiVariable> getSessionVariables(Project project) {
    return this.sessionVariablesProvider.getVariables(project);
  }

  @Override
  public Iterable<PsiVariable> getRequestVariables(Project project) {
    return this.requestVariablesProvider.getVariables(project);
  }
}
