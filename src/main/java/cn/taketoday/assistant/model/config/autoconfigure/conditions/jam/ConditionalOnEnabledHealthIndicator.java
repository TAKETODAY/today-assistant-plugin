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

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.lang.Nullable;

public class ConditionalOnEnabledHealthIndicator extends ConditionalOnEndpointBase {
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;
  private static final SemKey<ConditionalOnEnabledHealthIndicator> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnEnabledHealthIndicator");
  private static final String CONFIG_KEY_PREFIX = "management.health.";
  private static final PrefixSuffixApplicationMetaConfigKeyConverter VALUE_CONVERTER = new PrefixSuffixApplicationMetaConfigKeyConverter(CONFIG_KEY_PREFIX, ".enabled");
  private static final JamStringAttributeMeta.Single<MetaConfigKey> VALUE_ATTRIBUTE_META = JamAttributeMeta.singleString("value", VALUE_CONVERTER);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR).addAttribute(VALUE_ATTRIBUTE_META);
  private static final JamAnnotationMeta ANNOTATION_META_SB2 = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_ENABLED_HEALTH_INDICATOR_SB2).addAttribute(
          VALUE_ATTRIBUTE_META);
  public static final JamClassMeta<ConditionalOnEnabledHealthIndicator> CLASS_META = new JamClassMeta<>(null, ConditionalOnEnabledHealthIndicator.class, SEM_KEY).addAnnotation(
          ANNOTATION_META).addAnnotation(ANNOTATION_META_SB2);
  public static final JamMethodMeta<ConditionalOnEnabledHealthIndicator> METHOD_META = new JamMethodMeta<>(null, ConditionalOnEnabledHealthIndicator.class, SEM_KEY).addAnnotation(
          ANNOTATION_META).addAnnotation(ANNOTATION_META_SB2);

  @Override
  @Nullable
  public MetaConfigKey getResolvedConfigurationKey(Module module) {
    return super.getResolvedConfigurationKey(module);
  }

  @Override
  @Nullable
  public String getValue() {
    return super.getValue();
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext conditionalOnEvaluationContext) {
    return super.matches(conditionalOnEvaluationContext);
  }

  public ConditionalOnEnabledHealthIndicator(PsiModifierListOwner owner) {
    PsiElementRef<PsiAnnotation> annotationRef;
    if (ANNOTATION_META_SB2.getAnnotation(owner) != null) {
      annotationRef = ANNOTATION_META_SB2.getAnnotationRef(owner);
    }
    else {
      annotationRef = ANNOTATION_META.getAnnotationRef(owner);
    }
    this.myAnnotationRef = annotationRef;
  }

  @Override

  protected JamStringAttributeElement<MetaConfigKey> getValueJam() {
    return VALUE_ATTRIBUTE_META.getJam(this.myAnnotationRef);
  }

  @Override
  protected String getConfigurationKey(String value) {
    return VALUE_CONVERTER.getConfigurationKey(value);
  }

  @Override
  protected String getPrefix() {
    return CONFIG_KEY_PREFIX;
  }
}
