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

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.lang.Nullable;

public class CombinedInfraModelImpl extends InfraModel implements CombinedInfraModel {
  private final Set<CommonInfraModel> myModels;

  public CombinedInfraModelImpl(Set<? extends CommonInfraModel> models, @Nullable Module module) {
    super(module);
    this.myModels = new LinkedHashSet<>(models.size());
    this.myModels.addAll(models);
  }

  public CombinedInfraModelImpl(Set<? extends InfraModel> models, Module module, @Nullable InfraFileSet fileSet) {
    super(module, fileSet);
    this.myModels = new LinkedHashSet<>(models.size());
    this.myModels.addAll(models);
  }

  @Override
  public Set<CommonInfraModel> getUnderlyingModels() {
    return getRelatedModels(true);
  }

  @Override
  public Set<CommonInfraModel> getRelatedModels(boolean checkActiveProfiles) {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    models.addAll(this.myModels);
    models.addAll(getDependencies());
    return models;
  }

  @Override
  @Nullable
  public Set<String> getActiveProfiles() {
    Set<String> profiles = new LinkedHashSet<>();
    InfraModelVisitors.visitRecursionAwareRelatedModels(this, InfraModelVisitorContext.context(p -> true, (m, p2) -> {
      Set<String> activeProfiles = m.getActiveProfiles();
      if (activeProfiles != null) {
        profiles.addAll(activeProfiles);
        return true;
      }
      return true;
    }));
    return profiles;
  }

  @Override
  public String toString() {
    return "CombinedSpringModel[" + getModule().getName() + ", #" + this.myModels.size() + "]";
  }
}
