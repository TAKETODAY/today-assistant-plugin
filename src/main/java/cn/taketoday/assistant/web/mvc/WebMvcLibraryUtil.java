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

package cn.taketoday.assistant.web.mvc;

import com.intellij.jam.JavaLibraryUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.util.InfraUtils;

@Deprecated
public final class WebMvcLibraryUtil {

  public static boolean hasWebfluxLibrary(Project project) {
    return JavaLibraryUtils.hasLibraryClass(project, WebMvcFunctionalRoutingConstant.REACTIVE_DISPATCHER_HANDLER);
  }

  public static boolean hasWebfluxLibrary(Module module) {
    return InfraUtils.findLibraryClass(module, WebMvcFunctionalRoutingConstant.REACTIVE_DISPATCHER_HANDLER) != null;
  }

  public static boolean isWebfluxEnabled(Project project) {
    return InfraUtils.hasFacets(project) && hasWebfluxLibrary(project);
  }

}
