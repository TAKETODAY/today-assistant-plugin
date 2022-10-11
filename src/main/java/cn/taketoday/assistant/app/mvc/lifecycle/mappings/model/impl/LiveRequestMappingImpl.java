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

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveDispatcherServlet;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveHandlerMethod;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.lang.Nullable;

class LiveRequestMappingImpl implements LiveRequestMapping {
  private final String myMapping;
  private final LiveRequestMappingPredicate predicate;
  private final String myBean;
  private final LiveHandlerMethod myMethod;
  private final LiveDispatcherServlet myDispatcherServlet;

  LiveRequestMappingImpl(String mapping, LiveRequestMappingPredicate predicate, @Nullable String bean, @Nullable String method, LiveDispatcherServlet dispatcherServlet) {
    this.myMapping = mapping;
    this.predicate = predicate;
    this.myBean = bean;
    this.myMethod = method != null ? new cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl.LiveHandlerMethodImpl(method) : null;
    this.myDispatcherServlet = dispatcherServlet;
  }

  @Override

  public String getMapping() {
    String str = this.myMapping;
    return str;
  }

  @Override

  public String getPath() {
    String path = this.predicate.path();
    return path;
  }

  @Override

  public List<String> getRequestMethods() {
    List<String> unmodifiableList = Collections.unmodifiableList(this.predicate.requestMethods());
    return unmodifiableList;
  }

  @Override

  public List<Pair<String, String>> getHeaders() {
    return Collections.unmodifiableList(this.predicate.headers());
  }

  @Override

  public List<String> getProduces() {
    return Collections.unmodifiableList(this.predicate.produces());
  }

  @Override

  public List<String> getConsumes() {
    return Collections.unmodifiableList(this.predicate.consumes());
  }

  @Override

  public List<Pair<String, String>> getParams() {
    return Collections.unmodifiableList(this.predicate.params());
  }

  @Override
  @Nullable
  public String getBean() {
    return this.myBean;
  }

  @Override
  @Nullable
  public LiveHandlerMethod getMethod() {
    return this.myMethod;
  }

  @Override
  public boolean canNavigate() {
    return this.myMethod != null && (this.predicate.requestMethods().isEmpty() || this.predicate.requestMethods().contains("GET"));
  }

  @Override

  public LiveDispatcherServlet getDispatcherServlet() {
    return this.myDispatcherServlet;
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LiveRequestMappingImpl liveMapping) {
      return this.myMapping.equals(liveMapping.myMapping) && this.predicate.path().equals(liveMapping.predicate.path()) && Objects.equals(this.myBean, liveMapping.myBean) && Comparing.equal(
              this.myMethod, liveMapping.myMethod) && this.myDispatcherServlet.equals(liveMapping.myDispatcherServlet);
    }
    return false;
  }

  public int hashCode() {
    int result = (31 * 17) + this.myMapping.hashCode();
    return (31 * ((31 * ((31 * ((31 * result) + this.predicate.path()
            .hashCode())) + (this.myBean != null ? this.myBean.hashCode() : 0))) + (this.myMethod != null ? this.myMethod.hashCode() : 0))) + this.myDispatcherServlet.hashCode();
  }

  public String toString() {
    return this.myMapping;
  }
}
