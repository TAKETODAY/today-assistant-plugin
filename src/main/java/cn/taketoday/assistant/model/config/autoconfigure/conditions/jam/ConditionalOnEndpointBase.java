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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionMessage;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.lang.Nullable;

abstract class ConditionalOnEndpointBase implements ConditionalOnJamElement {

  protected static final String CONFIG_KEY_SUFFIX = ".enabled";

  protected abstract JamStringAttributeElement<MetaConfigKey> getValueJam();

  protected abstract String getPrefix();

  protected abstract String getConfigurationKey(String str);

  ConditionalOnEndpointBase() { }

  protected String getSuffix() {
    return CONFIG_KEY_SUFFIX;
  }

  @Override
  public ConditionOutcome matches(ConditionalOnEvaluationContext context) {
    String value = getValue();
    if (StringUtil.isEmpty(value)) {
      return ConditionOutcome.noMatch("Empty 'value' attribute");
    }
    String configKey = getConfigurationKey(value);
    Ref<String> wrongValue = Ref.create();
    Processor<List<ConfigurationValueResult>> isKeySetToTrue = results -> {
      if (ContainerUtil.exists(results, result -> {
        return Comparing.strEqual("true", result.getValueText(), false);
      })) {
        return false;
      }
      ConfigurationValueResult firstItem = ContainerUtil.getFirstItem(results);
      if (firstItem != null) {
        wrongValue.set(firstItem.getValueText());
        return true;
      }
      return true;
    };
    if (!context.processConfigurationValues(isKeySetToTrue, true, configKey)) {
      return ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(configKey, "true"));
    }
    if (!wrongValue.isNull()) {
      return ConditionOutcome.noMatch(ConditionMessage.foundConfigKeyWithValue(configKey, wrongValue.get()));
    }
    String defaultsKey = getConfigurationKey("defaults");
    if (!context.processConfigurationValues(isKeySetToTrue, true, defaultsKey)) {
      return ConditionOutcome.match(ConditionMessage.foundConfigKeyWithValue(defaultsKey, "true"));
    }
    if (!wrongValue.isNull()) {
      return ConditionOutcome.noMatch(ConditionMessage.foundConfigKeyWithValue(defaultsKey, wrongValue.get()));
    }
    return ConditionOutcome.match(ConditionMessage.generic("Unset", "property", configKey, defaultsKey));
  }

  @Nullable
  public String getValue() {
    return getValueJam().getStringValue();
  }

  @Nullable
  public MetaConfigKey getResolvedConfigurationKey(Module module) {
    PrefixSuffixApplicationMetaConfigKeyConverter converter = new PrefixSuffixApplicationMetaConfigKeyConverter(module, getPrefix(), CONFIG_KEY_SUFFIX);
    return converter.fromString(getValue(), getValueJam());
  }

  protected static class PrefixSuffixApplicationMetaConfigKeyConverter extends JamSimpleReferenceConverter<MetaConfigKey> {
    private final Module myModule;
    private final String myConfigKeyPrefix;
    private final String myConfigKeySuffix;
    static final boolean $assertionsDisabled;

    @Nullable
    public Object m109fromString(@Nullable String str, JamStringAttributeElement jamStringAttributeElement) {
      return fromString(str, (JamStringAttributeElement<MetaConfigKey>) jamStringAttributeElement);
    }

    static {
      $assertionsDisabled = !ConditionalOnEndpointBase.class.desiredAssertionStatus();
    }

    protected PrefixSuffixApplicationMetaConfigKeyConverter(String configKeyPrefix, String configKeySuffix) {
      this.myConfigKeyPrefix = configKeyPrefix;
      this.myConfigKeySuffix = configKeySuffix;
      this.myModule = null;
    }

    private PrefixSuffixApplicationMetaConfigKeyConverter(Module module, String configKeyPrefix, String configKeySuffix) {
      this.myModule = module;
      this.myConfigKeyPrefix = configKeyPrefix;
      this.myConfigKeySuffix = configKeySuffix;
    }

    public String getConfigurationKey(String value) {
      return this.myConfigKeyPrefix + value + this.myConfigKeySuffix;
    }

    @Nullable
    public MetaConfigKey fromString(@Nullable String s, JamStringAttributeElement<MetaConfigKey> context) {
      PsiAnnotationMemberValue psiElement = context.getPsiElement();
      if ($assertionsDisabled || psiElement != null) {
        Module module = ObjectUtils.chooseNotNull(this.myModule, ModuleUtilCore.findModuleForPsiElement(psiElement));
        return InfraApplicationMetaConfigKeyManager.of().findCanonicalApplicationMetaConfigKey(module, getConfigurationKey(s));
      }
      throw new AssertionError();
    }

    @Nullable
    public PsiElement getPsiElementFor(MetaConfigKey target) {
      return target.getDeclaration();
    }

    public LookupElement createLookupElementFor(MetaConfigKey target) {
      String afterPrefix = StringUtil.substringAfter(target.getName(), this.myConfigKeyPrefix);
      if ($assertionsDisabled || afterPrefix != null) {
        String id = StringUtil.substringBefore(afterPrefix, this.myConfigKeySuffix);
        return target.getPresentation().tuneLookupElement(target.getPresentation().getLookupElement(id));
      }
      throw new AssertionError();
    }

    public Collection<MetaConfigKey> getVariants(JamStringAttributeElement<MetaConfigKey> context) {
      List<? extends MetaConfigKey> allKeys = InfraApplicationMetaConfigKeyManager.of().getAllMetaConfigKeys(context.getPsiElement());
      SmartList smartList = new SmartList();
      for (MetaConfigKey key : allKeys) {
        if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
          String name = key.getName();
          if (StringUtil.startsWith(name, this.myConfigKeyPrefix) && StringUtil.endsWith(name, this.myConfigKeySuffix)) {
            smartList.add(key);
          }
        }
      }
      return smartList;
    }
  }
}
