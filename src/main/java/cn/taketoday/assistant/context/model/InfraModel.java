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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.module.Module;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.custom.CustomModuleComponentsDiscoverer;
import cn.taketoday.lang.Nullable;

public abstract class InfraModel extends AbstractProcessableModel {

  @Nullable
  private final Module myModule;
  private Set<InfraModel> myDependencies;
  @Nullable
  private final InfraFileSet fileSet;
  public static final CommonInfraModel UNKNOWN = new CommonInfraModel() {

    @Override
    public boolean processByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
      return true;
    }

    @Override
    public boolean processByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
      return true;
    }

    @Override
    public Collection<BeanPointer<?>> getAllCommonBeans() {
      return Collections.emptySet();
    }

    @Override
    public Module getModule() {
      return null;
    }

    @Override
    public Set<String> getActiveProfiles() {
      return Collections.emptySet();
    }
  };

  public abstract Set<CommonInfraModel> getRelatedModels(boolean z);

  public InfraModel(@Nullable Module module) {
    this(module, null);
  }

  public InfraModel(@Nullable Module module, @Nullable InfraFileSet fileSet) {
    this.myDependencies = Collections.emptySet();
    this.fileSet = fileSet;
    this.myModule = module;
  }

  @Nullable
  public InfraFileSet getFileSet() {
    return this.fileSet;
  }

  public Set<InfraModel> getDependencies() {
    return this.myDependencies;
  }

  public void setDependencies(Set<InfraModel> dependencies) {
    this.myDependencies = dependencies;
  }

  @Override
  public final Set<CommonInfraModel> getRelatedModels() {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    ContainerUtil.addAllNotNull(models, getRelatedModels(true));
    if (this.myModule != null && !this.myModule.isDisposed()) {
      models.add(CustomModuleComponentsDiscoverer.getCustomBeansModel(this.myModule));
    }
    return models;
  }

  @Nullable
  public Module getModule() {
    return this.myModule;
  }

  public String toString() {
    InfraFileSet fileSet = this.fileSet;
    return getClass().getName() + (fileSet != null ? " fileset=" + fileSet.getId() : "");
  }

  @Nullable
  public Set<String> getActiveProfiles() {
    if (this.fileSet == null) {
      return null;
    }
    return this.fileSet.getActiveProfiles();
  }
}
