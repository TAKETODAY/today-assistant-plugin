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
package cn.taketoday.assistant.app.run.update;

import com.intellij.execution.Executor;
import com.intellij.openapi.extensions.ExtensionPointName;

import org.jetbrains.annotations.Nls;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.lang.Nullable;

/**
 * InfraApplicationUpdatePolicy may be set up for Infra run configuration to perform an update of running application.
 * Update may be performed on frame deactivation or by invoking update action.
 *
 * @author Konstantin Aleev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class InfraApplicationUpdatePolicy {
  public static final ExtensionPointName<InfraApplicationUpdatePolicy> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.app.run.applicationUpdatePolicy");

  private final String id;
  private final @Nls(capitalization = Nls.Capitalization.Sentence) String name;
  @Nullable
  private final @Nls(capitalization = Nls.Capitalization.Sentence) String description;

  protected InfraApplicationUpdatePolicy(String id,
          @Nls(capitalization = Nls.Capitalization.Sentence) String name,
          @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  public String getName() {
    return name;
  }

  /**
   * @return policy details description or <code>null</code> if it is not specified. Return value may contain HTML.
   */
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Nullable
  public String getDescription() {
    return description;
  }

  public boolean isAvailableOnFrameDeactivation() {
    return true;
  }

  public boolean isAvailableForExecutor(Executor executor) {
    return true;
  }

  public boolean isAvailableForConfiguration(InfraApplicationRunConfigurationBase configuration) {
    return true;
  }

  public abstract void runUpdate(InfraApplicationUpdateContext context);

  public static List<InfraApplicationUpdatePolicy> getAvailablePolicies(boolean onFrameDeactivation) {
    if (onFrameDeactivation) {
      return Arrays.stream(EP_NAME.getExtensions())
              .filter(InfraApplicationUpdatePolicy::isAvailableOnFrameDeactivation)
              .collect(Collectors.toList());
    }
    return Arrays.asList(EP_NAME.getExtensions());
  }

  @Nullable
  public static InfraApplicationUpdatePolicy findPolicy(@Nullable String id) {
    if (id == null)
      return null;

    for (InfraApplicationUpdatePolicy updatePolicy : EP_NAME.getExtensions()) {
      if (id.equals(updatePolicy.id))
        return updatePolicy;
    }

    return null;
  }
}
