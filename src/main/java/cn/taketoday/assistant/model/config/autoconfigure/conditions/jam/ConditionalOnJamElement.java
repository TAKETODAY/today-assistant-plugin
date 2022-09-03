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

import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.model.InfraConditional;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;

/**
 * All condition JAM elements must implement this interface to be evaluated.
 * <p>
 * Registration via {@link #CONDITIONAL_JAM_ELEMENT_KEY} subKey is required.
 */
public interface ConditionalOnJamElement extends InfraConditional {

  SemKey<ConditionalOnJamElement> CONDITIONAL_JAM_ELEMENT_KEY =
          InfraConditional.CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnJamElement");

  ConditionOutcome matches(ConditionalOnEvaluationContext context);

  interface NonStrict extends ConditionalOnJamElement {

    @Override
    default ConditionOutcome matches(ConditionalOnEvaluationContext context) {
      return ConditionOutcome.noMatch("Strict Mode (cannot evaluate)");
    }
  }
}
