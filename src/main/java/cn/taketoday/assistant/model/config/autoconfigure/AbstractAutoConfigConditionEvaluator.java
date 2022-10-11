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

package cn.taketoday.assistant.model.config.autoconfigure;

import java.util.Collection;

import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnJamElement;

abstract class AbstractAutoConfigConditionEvaluator {
  private final boolean nonStrictEvaluation;
  private final ConditionalOnEvaluationContext conditionalOnEvaluationContext;

  protected abstract ConditionalCollector getConditionalCollector();

  AbstractAutoConfigConditionEvaluator(boolean nonStrictEvaluation, ConditionalOnEvaluationContext conditionalOnEvaluationContext) {
    this.nonStrictEvaluation = nonStrictEvaluation;
    this.conditionalOnEvaluationContext = conditionalOnEvaluationContext;
  }

  boolean isActive() {
    Collection<ConditionalOnJamElement> allConditions = getConditionalCollector().getConditionals();
    if (allConditions.isEmpty()) {
      return true;
    }
    for (ConditionalOnJamElement condition : allConditions) {
      if (!this.nonStrictEvaluation || !(condition instanceof ConditionalOnJamElement.NonStrict)) {
        ConditionOutcome outcome = condition.matches(this.conditionalOnEvaluationContext);
        if (!outcome.isMatch()) {
          return false;
        }
      }
    }
    return true;
  }
}
