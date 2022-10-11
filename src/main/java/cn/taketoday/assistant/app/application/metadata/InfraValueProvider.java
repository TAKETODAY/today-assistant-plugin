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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.util.containers.ContainerUtil;

import cn.taketoday.assistant.InfraVersion;
import cn.taketoday.lang.Nullable;

public enum InfraValueProvider {

  ANY("any", "Permit any additional values to be provided"),
  CLASS_REFERENCE("class-reference",
          "Classes available in the project. Usually constrained by a base class specified via 'target' parameter.",
          new Parameter(InfraMetadataConstant.TARGET, false),
          new Parameter(InfraMetadataConstant.CONCRETE, false)),
  HANDLE_AS("handle-as",
          "Handle the property as if it was defined by the type defined via the mandatory 'target' parameter.",
          new Parameter(InfraMetadataConstant.TARGET, true)),
  LOGGER_NAME("logger-name",
          "Valid logger names. Typically, package and class names available in the current project.",
          new Parameter(InfraMetadataConstant.GROUP, false, InfraVersion.V_4_0)),
  BEAN_REFERENCE("infra-bean-reference",
          "Available bean names in the current project. Usually constrained by a base class specified via 'target' parameter.",
          new Parameter(InfraMetadataConstant.TARGET, false)),
  PROFILE_NAME("infra-profile-name", "Available profile names in the project.");

  private final String id;
  private final String description;
  private final Parameter[] parameters;
  private final boolean hasRequiredParameters = calcHasRequiredParameters();

  InfraValueProvider(String id, String description, Parameter... parameters) {
    this.id = id;
    this.description = description;
    this.parameters = parameters;
  }

  public String getId() {
    return this.id;
  }

  public String getDescription() {
    return this.description;
  }

  public Parameter[] getParameters() {
    return this.parameters;
  }

  public boolean hasRequiredParameters() {
    return this.hasRequiredParameters;
  }

  private boolean calcHasRequiredParameters() {
    for (Parameter parameter : parameters) {
      if (parameter.isRequired()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static InfraValueProvider findById(String id) {
    return ContainerUtil.find(values(), provider -> {
      return provider.id.equals(id);
    });
  }

  public static class Parameter {
    private final String myName;
    private final boolean myRequired;
    private final InfraVersion myMinimumVersion;

    Parameter(String name, boolean required) {
      this(name, required, InfraVersion.ANY);
    }

    Parameter(String name, boolean required, InfraVersion minimumVersion) {
      this.myName = name;
      this.myRequired = required;
      this.myMinimumVersion = minimumVersion;
    }

    public String getName() {
      return this.myName;
    }

    public boolean isRequired() {
      return this.myRequired;
    }

    public InfraVersion getMinimumVersion() {
      return this.myMinimumVersion;
    }
  }
}
