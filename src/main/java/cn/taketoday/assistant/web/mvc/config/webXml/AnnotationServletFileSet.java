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

package cn.taketoday.assistant.web.mvc.config.webXml;

import com.intellij.javaee.web.CommonServlet;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NonNls;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.context.model.ComponentScannedApplicationContext;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;
import cn.taketoday.lang.Nullable;

public class AnnotationServletFileSet extends ServletFileSet implements ComponentScannedApplicationContext {
  private final Set<String> myComponentScanPackages;

  AnnotationServletFileSet(@NonNls String id, String name, @Nullable CommonServlet servlet, InfraFacet parent) {
    super(id, name, servlet, parent);
    this.myComponentScanPackages = new LinkedHashSet();
  }

  void addComponentScanPackage(String pkg) {
    if (StringUtil.isNotEmpty(pkg)) {
      this.myComponentScanPackages.add(pkg);
    }
  }

  public Set<String> getComponentScanPackages() {
    return this.myComponentScanPackages;
  }
}
