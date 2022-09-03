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

package cn.taketoday.assistant.app.run.lifecycle.beans.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;
import cn.taketoday.lang.Nullable;

class LiveContextImpl implements LiveContext {

  private final String myName;
  private LiveContext myParent;
  private final List<LiveResource> myResources;

  LiveContextImpl(String name) {
    this.myResources = new ArrayList();
    this.myName = name;
  }

  @Override

  public String getName() {
    String str = this.myName;
    return str;
  }

  @Override
  @Nullable
  public LiveContext getParent() {
    return this.myParent;
  }

  @Override

  public List<LiveResource> getResources() {
    return new ArrayList(this.myResources);
  }

  @Override

  public List<LiveBean> getBeans() {
    return this.myResources.stream().flatMap(resource -> {
      return resource.getBeans().stream();
    }).collect(Collectors.toList());
  }

  void setParent(@Nullable LiveContext parent) {
    this.myParent = parent;
  }

  void addResources(Collection<? extends LiveResource> resources) {
    this.myResources.addAll(resources);
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LiveContextImpl liveContext)) {
      return false;
    }
    if (!this.myName.equals(liveContext.myName)) {
      return false;
    }
    if (this.myParent != null && liveContext.myParent != null) {
      return this.myParent.getName().equals(liveContext.myParent.getName());
    }
    return this.myParent == liveContext.myParent;
  }

  public int hashCode() {
    int result = (31 * 17) + this.myName.hashCode();
    return (31 * result) + (this.myParent != null ? this.myParent.getName().hashCode() : 0);
  }

  public String toString() {
    return this.myName;
  }
}
