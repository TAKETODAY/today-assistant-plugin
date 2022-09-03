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
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.lang.Nullable;

public class ConditionalOnRepositoryType implements ConditionalOnJamElement {
  private static final SemKey<ConditionalOnRepositoryType> SEM_KEY = CONDITIONAL_JAM_ELEMENT_KEY.subKey("ConditionalOnRepositoryType");
  private static final JamStringAttributeMeta.Single<String> STORE_ATTRIBUTE_META = JamAttributeMeta.singleString("store");
  private static final JamEnumAttributeMeta.Single<RepositoryType> TYPE_ATTRIBUTE_META = JamAttributeMeta.singleEnum(InfraMetadataConstant.TYPE, RepositoryType.class);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.CONDITIONAL_ON_REPOSITORY_TYPE).addAttribute(STORE_ATTRIBUTE_META)
          .addAttribute(TYPE_ATTRIBUTE_META);
  public static final JamClassMeta<ConditionalOnRepositoryType> CLASS_META = new JamClassMeta<>(null, ConditionalOnRepositoryType.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  public static final JamMethodMeta<ConditionalOnRepositoryType> METHOD_META = new JamMethodMeta<>(null, ConditionalOnRepositoryType.class, SEM_KEY).addAnnotation(
          ANNOTATION_META);
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;

  enum RepositoryType {
    AUTO,
    IMPERATIVE,
    NONE,
    REACTIVE
  }

  public ConditionalOnRepositoryType(PsiModifierListOwner owner) {
    this.myAnnotationRef = ANNOTATION_META.getAnnotationRef(owner);
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    String store = getStore();
    if (StringUtil.isEmpty(store)) {
      return ConditionOutcome.noMatch("Empty 'store' value");
    }
    RepositoryType requiredType = getType();
    if (requiredType == null) {
      return ConditionOutcome.noMatch("Invalid 'type' value");
    }
    String storeConfigKeyName = "spring.data." + store + ".repositories.type";
    Ref<String> configuredValueRef = Ref.create(RepositoryType.AUTO.name());
    Processor<List<ConfigurationValueResult>> findValueProcessor = results -> {
      ConfigurationValueResult item = ContainerUtil.getFirstItem(results);
      if (item != null) {
        configuredValueRef.set(item.getValueText());
        return false;
      }
      return false;
    };
    context.processConfigurationValues(findValueProcessor, true, storeConfigKeyName);
    String configuredValue = configuredValueRef.get();
    if (StringUtil.isEmptyOrSpaces(configuredValue)) {
      return ConditionOutcome.noMatch(ConditionMessage.foundConfigKeyWithValue(storeConfigKeyName, "<no value set>"));
    }
    try {
      RepositoryType configuredRepositoryType = Enum.valueOf(RepositoryType.class, StringUtil.toUpperCase(configuredValue));
      if (configuredRepositoryType == requiredType) {
        ConditionOutcome match = ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(storeConfigKeyName, requiredType.name()));
        return match;
      }
      else if (configuredRepositoryType == RepositoryType.AUTO) {
        return ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(storeConfigKeyName, RepositoryType.AUTO.name()));
      }
      else {
        return ConditionOutcome.noMatch("Configuration value '" + configuredValue + "' does not match " + requiredType.name());
      }
    }
    catch (IllegalArgumentException e) {
      return ConditionOutcome.noMatch("Illegal configuration value '" + configuredValue + "'");
    }
  }

  @Nullable
  public String getStore() {
    return STORE_ATTRIBUTE_META.getJam(this.myAnnotationRef).getStringValue();
  }

  @Nullable
  public RepositoryType getType() {
    return TYPE_ATTRIBUTE_META.getJam(this.myAnnotationRef).getValue();
  }
}
