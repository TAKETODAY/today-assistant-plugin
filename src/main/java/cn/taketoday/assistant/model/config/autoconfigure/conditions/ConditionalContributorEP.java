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

import com.intellij.openapi.extensions.CustomLoadingExtensionPointBean;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;

import cn.taketoday.lang.Nullable;

/**
 * Registers {@link ConditionalContributor} implementation via FQN of actual Condition.
 */
final class ConditionalContributorEP extends CustomLoadingExtensionPointBean<ConditionalContributor> implements KeyedLazyInstance<ConditionalContributor> {
  /**
   * FQN of library condition class.
   */
  @Attribute("condition")
  public String condition;

  /**
   * IDE implementation of condition class.
   */
  @Attribute("implementation")
  public String implementation;

  @Nullable
  @Override
  protected String getImplementationClassName() {
    return implementation;
  }

  @Override
  public String getKey() {
    return condition;
  }
}
