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

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiClass;
import com.intellij.util.Processor;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnJamElement;
import cn.taketoday.lang.Nullable;

/**
 * Simulates run-time context for evaluating conditions.
 *
 * @see ConditionalContributor
 * @see ConditionalOnJamElement
 */
public interface ConditionalOnEvaluationContext extends UserDataHolderEx {
  Key<CommonInfraModel> MODEL_KEY = Key.create("INFRA_MODEL_KEY");

  PsiClass getAutoConfigClass();

  Module getModule();

  @Nullable
  Set<String> getActiveProfiles();

  /**
   * @param processor Processor.
   * @param checkRelaxedNames Whether to allow relaxed names.
   * @param configKey Configuration key in canonical notation.
   * @return Processor result.
   */
  default boolean processConfigurationValues(Processor<? super List<ConfigurationValueResult>> processor,
          boolean checkRelaxedNames,
          String configKey) {
    final MetaConfigKey key =
            InfraApplicationMetaConfigKeyManager.getInstance().findCanonicalApplicationMetaConfigKey(getModule(), configKey);
    if (key == null)
      return true;

    return processConfigurationValues(processor, checkRelaxedNames, key);
  }

  boolean processConfigurationValues(Processor<? super List<ConfigurationValueResult>> processor,
          boolean checkRelaxedNames,
          MetaConfigKey configKey);
}
