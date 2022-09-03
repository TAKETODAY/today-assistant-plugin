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

package cn.taketoday.assistant.model.config.autoconfigure.conditions.jam;

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.util.InfraUtils;

public class ConditionalOnEnabledResourceChain implements ConditionalOnJamElement {

  private static final String WEB_JAR_ASSET_LOCATOR = "org.webjars.WebJarAssetLocator";
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_ENABLED_RESOURCE_CHAIN);
  private static final SemKey<ConditionalOnEnabledResourceChain> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnEnabledResourceChain");
  public static final JamClassMeta<ConditionalOnEnabledResourceChain> CLASS_META = new JamClassMeta<>(null, ConditionalOnEnabledResourceChain.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnEnabledResourceChain> METHOD_META = new JamMethodMeta<>(null, ConditionalOnEnabledResourceChain.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);

  private static final String[] CONFIGURATION_KEYS = {
          "web.resources.chain.enabled",
          "web.resources.chain.strategy.fixed.enabled",
          "web.resources.chain.strategy.content.enabled"
  };
  private static final Key<ConditionOutcome> OUTCOME_KEY = Key.create("ConditionalOnEnabledResourceChain");
  private static final Function<ConditionalOnEvaluationContext, ConditionOutcome> OUTCOME_FUNCTION = context -> {
    Processor<List<ConfigurationValueResult>> isSetToTrueProcessor = results -> {
      return !ContainerUtil.exists(results, result -> {
        return Comparing.strEqual("true", result.getValueText(), false);
      });
    };
    for (String key : CONFIGURATION_KEYS) {
      if (!context.processConfigurationValues(isSetToTrueProcessor, true, key)) {
        return ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(key, "true"));
      }
    }
    boolean hasWebJar = InfraUtils.findLibraryClass(context.getModule(), WEB_JAR_ASSET_LOCATOR) != null;
    return hasWebJar ? ConditionOutcome.match(ConditionMessage.foundClass(WEB_JAR_ASSET_LOCATOR)) : ConditionOutcome.noMatch(ConditionMessage.didNotFindClass(WEB_JAR_ASSET_LOCATOR));
  };

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    ConditionOutcome data = context.getUserData(OUTCOME_KEY);
    if (data == null) {
      data = context.putUserDataIfAbsent(OUTCOME_KEY, OUTCOME_FUNCTION.fun(context));
    }
    return data;
  }
}
