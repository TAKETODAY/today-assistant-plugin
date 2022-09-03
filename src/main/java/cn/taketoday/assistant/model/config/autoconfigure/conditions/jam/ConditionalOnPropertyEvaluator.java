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

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.util.Processor;

import java.util.List;

import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.text.StringsKt;

public final class ConditionalOnPropertyEvaluator {
  private final cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnProperty myConditionalOnProperty;
  private final ConditionalOnEvaluationContext myContext;

  public ConditionalOnPropertyEvaluator(ConditionalOnProperty myConditionalOnProperty, ConditionalOnEvaluationContext myContext) {
    this.myConditionalOnProperty = myConditionalOnProperty;
    this.myContext = myContext;
  }

  public ConditionOutcome matches() {
    String str;
    Module module = this.myContext.getModule();
    Intrinsics.checkNotNullExpressionValue(module, "myContext.module");
    List<MetaConfigKey> keys = this.myConditionalOnProperty.getResolvedMetaConfigKeys(module);
    if (keys.isEmpty()) {
      if (this.myConditionalOnProperty.isMatchIfMissing()) {
        return ConditionOutcome.match("Could not resolve property");
      }
      ConditionOutcome noMatch = ConditionOutcome.noMatch("Could not resolve property");
      Intrinsics.checkNotNullExpressionValue(noMatch, "ConditionOutcome.noMatch…ld not resolve property\")");
      return noMatch;
    }
    int keysCount = keys.size();
    Ref.IntRef foundKeys = new Ref.IntRef();
    foundKeys.element = 0;
    var matched = new com.intellij.openapi.util.Ref(false);
    var conditionMessage = com.intellij.openapi.util.Ref.create();
    Intrinsics.checkNotNullExpressionValue(conditionMessage, "Ref.create<ConditionMessage>()");
    Processor processor = new Processor<List<? extends ConfigurationValueResult>>() {
      public boolean process(List<? extends ConfigurationValueResult> list) {
        boolean valueMatches;
        Intrinsics.checkNotNullExpressionValue(list, "configurationValues");
        if (!list.isEmpty()) {
          foundKeys.element++;
          ConfigurationValueResult configurationValueResult = list.get(0);
          com.intellij.openapi.util.Ref ref = matched;
          valueMatches = ConditionalOnPropertyEvaluator.this.valueMatches(configurationValueResult.getValueText());
          ref.set(Boolean.valueOf(valueMatches));
          conditionMessage.set(ConditionMessage.foundConfigKeyWithValue(configurationValueResult.getParams().getConfigKey().getName(), configurationValueResult.getValueText()));
          return false;
        }
        return true;
      }
    };
    for (MetaConfigKey key : keys) {
      matched.set(false);
      boolean result = this.myContext.processConfigurationValues(processor, this.myConditionalOnProperty.isRelaxedNames(), key);
      if (!result && !(Boolean) matched.get()) {
        return ConditionOutcome.noMatch((ConditionMessage) conditionMessage.get());
      }
    }
    if (keys.size() == 1 && conditionMessage.get() != null) {
      Object obj = matched.get();
      if ((Boolean) obj) {
        return ConditionOutcome.match((ConditionMessage) conditionMessage.get());
      }
      return ConditionOutcome.noMatch((ConditionMessage) conditionMessage.get());
    }
    else if (keysCount == foundKeys.element) {
      return ConditionOutcome.match("All properties matched");
    }
    else {
      if (foundKeys.element == 0) {
        str = "No properties defined";
      }
      else {
        str = foundKeys.element + " properties matched, " + (keysCount - foundKeys.element) + " properties not defined";
      }
      String message = str;
      if (this.myConditionalOnProperty.isMatchIfMissing()) {
        return ConditionOutcome.match(message);
      }
      return ConditionOutcome.noMatch(message);
    }
  }

  public boolean valueMatches(String configurationValue) {
    String havingValue = this.myConditionalOnProperty.getHavingValue();
    if (havingValue == null || havingValue.length() == 0) {
      return !StringsKt.equals("false", configurationValue, true);
    }
    return StringsKt.equals(havingValue, configurationValue, true);
  }
}
