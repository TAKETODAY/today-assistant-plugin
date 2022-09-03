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
package cn.taketoday.assistant.model.config.autoconfigure.conditions;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.List;

/**
 * Pluggable mechanism to evaluate actual Conditional behavior at design time.
 *
 * @see ConditionalContributorEP
 */
public abstract class ConditionalContributor {

  static final ExtensionPointName<ConditionalContributor> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.configConditionalContributor");

  public abstract ConditionOutcome matches(ConditionalOnEvaluationContext context);

  /**
   * Checks whether given config key is set.
   *
   * @param context Context.
   * @param configKey Configuration key to find.
   * @return {@code true} if configuration key was found.
   */
  protected boolean hasConfigKey(ConditionalOnEvaluationContext context, String configKey) {
    return !context.processConfigurationValues(List::isEmpty, true, configKey);
  }
}
