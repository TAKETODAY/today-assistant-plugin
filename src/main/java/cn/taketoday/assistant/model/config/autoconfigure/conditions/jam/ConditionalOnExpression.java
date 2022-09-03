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
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;

public class ConditionalOnExpression implements ConditionalOnJamElement.NonStrict {
  private static final JamStringAttributeMeta.Single<String> VALUE_ATTRIBUTE = JamAttributeMeta.singleString("value");
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_EXPRESSION).addAttribute(VALUE_ATTRIBUTE);
  private static final SemKey<ConditionalOnExpression> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnExpression");
  public static final JamClassMeta<ConditionalOnExpression> CLASS_META = new JamClassMeta<>(null, ConditionalOnExpression.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnExpression> METHOD_META = new JamMethodMeta<>(null, ConditionalOnExpression.class, SEM_KEY).addAnnotation(ANNOTATION_META);
}
