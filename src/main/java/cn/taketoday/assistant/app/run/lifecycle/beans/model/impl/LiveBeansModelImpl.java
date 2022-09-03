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

import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveResource;

class LiveBeansModelImpl implements LiveBeansModel {

  private final List<LiveContext> myContexts = new SmartList<>();
  private final Map<String, List<LiveBean>> myBeansByName;

  LiveBeansModelImpl(Collection<? extends LiveContext> contexts) {
    this.myContexts.addAll(contexts);
    this.myBeansByName = contexts.stream()
            .flatMap(context -> context.getBeans().stream())
            .filter(bean -> !bean.isInnerBean())
            .collect(Collectors.groupingBy(LiveBean::getName, Collectors.toCollection(SmartList::new)));
  }

  @Override
  public List<LiveContext> getContexts() {
    SmartList<LiveContext> smartList = new SmartList<>();
    smartList.addAll(this.myContexts);
    return smartList;
  }

  @Override
  public List<LiveResource> getResources() {
    return this.myContexts.stream().flatMap(context -> context.getResources().stream()).collect(Collectors.toList());
  }

  @Override
  public List<LiveBean> getBeans() {
    return this.myContexts.stream()
            .flatMap(context -> context.getBeans().stream())
            .collect(Collectors.toList());
  }

  @Override

  public List<LiveBean> getBeansByName(String name) {
    List<LiveBean> beans = this.myBeansByName.get(name);
    return beans != null ? Collections.unmodifiableList(beans) : Collections.emptyList();
  }
}
