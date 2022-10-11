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

package cn.taketoday.assistant.app.application.yaml;

import com.intellij.microservices.jvm.config.ConfigKeyPathArbitraryEntryKeyReference;
import com.intellij.microservices.jvm.config.ConfigKeyPathContext;
import com.intellij.microservices.jvm.config.ConfigKeyPathEnumReference;
import com.intellij.microservices.jvm.config.ConfigKeyPathReferenceBase;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.yaml.ConfigKeyPathYamlContext;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.lang.Nullable;

class InfraApplicationYamlKeyReferenceProvider extends PsiReferenceProvider {

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    YAMLKeyValue yamlKeyValue = (YAMLKeyValue) element;
    if (yamlKeyValue.getKey() == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    String qualifiedConfigKeyName = ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue);
    YamlKeyMetaConfigKeyReference metaConfigKeyReference = new YamlKeyMetaConfigKeyReference(yamlKeyValue, qualifiedConfigKeyName);
    SmartList<PsiReference> smartList = new SmartList<>(metaConfigKeyReference);
    YAMLKeyValue parent = PsiTreeUtil.getParentOfType(yamlKeyValue, YAMLKeyValue.class);
    if (parent == null) {
      return smartList.toArray(PsiReference.EMPTY_ARRAY);
    }
    smartList.addAll(getReferences(yamlKeyValue, parent, null, context));
    if (smartList.size() > 1) {
      metaConfigKeyReference.setRangeInElement(TextRange.EMPTY_RANGE);
    }
    return smartList.toArray(PsiReference.EMPTY_ARRAY);
  }

  static List<PsiReference> getReferences(YAMLKeyValue yamlKeyValue, YAMLKeyValue parent, @Nullable PsiElement hintElement, ProcessingContext processingContext) {
    PsiReference configPathReference = ConfigKeyPathYamlContext.getConfigPathReference(yamlKeyValue, parent);
    if (configPathReference == null) {
      return Collections.emptyList();
    }
    if ((configPathReference instanceof ConfigKeyPathArbitraryEntryKeyReference) || (configPathReference instanceof ConfigKeyPathEnumReference)) {
      ConfigKeyPathReferenceBase keyReference = (ConfigKeyPathReferenceBase) configPathReference;
      ConfigKeyPathContext context = keyReference.getContext();
      if (context.isFirst()) {
        String prefix = ((ConfigKeyPathYamlContext) context).getPrefix();
        if (StringUtil.isNotEmpty(prefix)) {
          processingContext.put(InfraHintReferencesProvider.HINT_REFERENCES_MAP_KEY_PREFIX, prefix);
        }
        PsiReference[] hintReferences = getHintReferences(context.getKey(), ObjectUtils.chooseNotNull(hintElement, yamlKeyValue), processingContext);
        if (hintReferences.length > 0) {
          if (keyReference instanceof ConfigKeyPathEnumReference) {
            keyReference = new ConfigKeyPathArbitraryEntryKeyReference(keyReference.getElement(), keyReference.getContext());
          }
          SmartList<PsiReference> smartList = new SmartList<>(keyReference);
          keyReference.setRangeInElement(TextRange.EMPTY_RANGE);
          ContainerUtil.addAll(smartList, hintReferences);
          return smartList;
        }
      }
    }
    return new SmartList(configPathReference);
  }

  private static PsiReference[] getHintReferences(MetaConfigKey configKey, PsiElement element, ProcessingContext context) {
    TextRange textRange;
    InfraHintReferencesProvider hintReferencesProvider = InfraHintReferencesProvider.getInstance();
    if (element instanceof YAMLKeyValue yamlKeyValue) {
      TextRange textRange2 = ConfigYamlUtils.getKeyReferenceRange(yamlKeyValue);
      textRange = ConfigYamlUtils.trimIndexedAccess(textRange2, yamlKeyValue);
    }
    else {
      textRange = ElementManipulators.getValueTextRange(element);
    }
    return hintReferencesProvider.getKeyReferences(configKey, element, textRange, context);
  }

  private static final class YamlKeyMetaConfigKeyReference extends MetaConfigKeyReference<YAMLKeyValue> {

    private YamlKeyMetaConfigKeyReference(YAMLKeyValue yamlKeyValue, String keyText) {
      super(InfraApplicationMetaConfigKeyManager.of(), yamlKeyValue, keyText);
      setRangeInElement(ConfigYamlUtils.getKeyReferenceRange(yamlKeyValue));
    }

    public String getReferenceDisplayText() {
      return ConfigYamlUtils.getQualifiedConfigKeyName(getElement()) + ": " + ConfigYamlUtils.getValuePresentationText(getElement());
    }
  }
}
