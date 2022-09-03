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
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;

public class ConditionalOnNotWebApplication implements ConditionalOnJamElement {
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_NOT_WEB_APPLICATION);
  private static final SemKey<ConditionalOnNotWebApplication> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnNotWebApplication");
  public static final JamClassMeta<ConditionalOnNotWebApplication> CLASS_META = new JamClassMeta<>(null, ConditionalOnNotWebApplication.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnNotWebApplication> METHOD_META = new JamMethodMeta<>(null, ConditionalOnNotWebApplication.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);

  @Override

  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    ConditionOutcome servletOutcome = cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnWebApplication.getServletOutcome(context);
    if (!servletOutcome.isMatch()) {

      ConditionOutcome reactiveOutcome = ConditionalOnWebApplication.getReactiveOutcome(context);
      if (reactiveOutcome.isMatch()) {
        return ConditionOutcome.noMatch("Found web application");
      }

      return ConditionOutcome.match("Not a web application");
    }
    return ConditionOutcome.noMatch("Found web application");
  }
}
