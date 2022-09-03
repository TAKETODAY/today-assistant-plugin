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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveDispatcherServlet;
import cn.taketoday.lang.Nullable;

class LiveDispatcherServletImpl implements LiveDispatcherServlet {
  static final LiveDispatcherServlet DEFAULT = new LiveDispatcherServletImpl("dispatcherServlet", Collections.emptyList());
  private final String myName;
  private final List<String> myServletMappings;

  LiveDispatcherServletImpl(String name, List<String> servletMappings) {
    this.myName = name;
    this.myServletMappings = servletMappings;
  }

  @Override

  public String getName() {
    String str = this.myName;
    return str;
  }

  @Override

  public List<String> getServletMappings() {
    List<String> unmodifiableList = Collections.unmodifiableList(this.myServletMappings);
    return unmodifiableList;
  }

  @Override
  @Nullable
  public String getServletMappingPath() {
    String servletPath = ContainerUtil.getFirstItem(this.myServletMappings);
    if (servletPath != null) {
      return StringUtil.trimEnd(servletPath, '*');
    }
    return null;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LiveDispatcherServletImpl liveDispatcherServlet) {
      return this.myName.equals(liveDispatcherServlet.myName) && this.myServletMappings.equals(liveDispatcherServlet.myServletMappings);
    }
    return false;
  }

  public int hashCode() {
    int result = (31 * 17) + this.myName.hashCode();
    return (31 * result) + this.myServletMappings.hashCode();
  }
}
