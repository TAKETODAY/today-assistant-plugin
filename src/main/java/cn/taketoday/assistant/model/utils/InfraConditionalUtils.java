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

package cn.taketoday.assistant.model.utils;

import com.intellij.openapi.extensions.ExtensionPointName;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ConditionalEvaluationContext;
import cn.taketoday.assistant.model.ConditionalEvaluator;
import cn.taketoday.assistant.model.ConditionalEvaluatorProvider;

public class InfraConditionalUtils {
  private static final ExtensionPointName<ConditionalEvaluatorProvider> EP_NAME = ExtensionPointName.create("cn.taketoday.assistant.conditionalEvaluatorProvider");

  public static boolean isActive(BeanPointer<?> beanPointer, ConditionalEvaluationContext context) {
    if (beanPointer instanceof ConditionalEvaluator) {
      return ((ConditionalEvaluator) beanPointer).isActive(context);
    }
    for (ConditionalEvaluatorProvider provider : EP_NAME.getExtensionList()) {
      ConditionalEvaluator evaluator = provider.getConditionalEvaluator(beanPointer);
      if (evaluator != null && !evaluator.isActive(context)) {
        return false;
      }
    }
    return true;
  }
}
