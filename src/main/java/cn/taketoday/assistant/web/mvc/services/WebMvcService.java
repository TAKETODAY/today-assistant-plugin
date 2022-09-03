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

package cn.taketoday.assistant.web.mvc.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;

import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;

public abstract class WebMvcService {
  public abstract Set<BeanPointer<?>> getControllers(Module module);

  public abstract Set<BeanPointer<?>> getBeanControllers(Module module);

  public abstract Set<BeanPointer<?>> getFunctionalRoutes(Module module);

  public abstract Set<ViewResolver> getViewResolvers(Module module);

  public static WebMvcService getInstance() {
    return ApplicationManager.getApplication().getService(WebMvcService.class);
  }

  public static Set<InfraModel> getServletModels(Module module) {
    return InfraManager.from(module.getProject()).getAllModels(module).stream()
            .filter(model -> model.getFileSet() instanceof ServletFileSet)
            .collect(Collectors.toSet());
  }
}
