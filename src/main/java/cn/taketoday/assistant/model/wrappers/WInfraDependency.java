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

package cn.taketoday.assistant.model.wrappers;

import com.intellij.openapi.util.Pair;

import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;

public class WInfraDependency {

  public final String modelName;

  final LocalModelDependencyType dependencyType;

  public WInfraDependency(String name, LocalModelDependencyType type) {
    this.modelName = name;
    this.dependencyType = type;
  }

  public WInfraDependency(Pair<LocalModel, LocalModelDependency> model) {
    this.dependencyType = model.second.getType();
    if (model.first instanceof LocalXmlModel) {
      this.modelName = WModel.getLocalXmlModelName((LocalXmlModel) model.first);
    }
    else if (model.first instanceof LocalAnnotationModel) {
      this.modelName = ((LocalAnnotationModel) model.first).getConfig().getQualifiedName();
    }
    else {
      throw new IllegalArgumentException(String.format("Model with class:'%s' is not xml nor annotation model", model.getClass()));
    }
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WInfraDependency that = (WInfraDependency) o;
    return this.modelName.equals(that.modelName) && this.dependencyType == that.dependencyType;
  }

  public int hashCode() {
    int result = this.modelName.hashCode();
    return (31 * result) + this.dependencyType.hashCode();
  }
}
