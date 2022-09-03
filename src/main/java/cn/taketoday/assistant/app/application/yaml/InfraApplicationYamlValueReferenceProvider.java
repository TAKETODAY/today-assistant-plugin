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

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils;
import com.intellij.microservices.jvm.config.hints.BooleanHintReference;
import com.intellij.microservices.jvm.config.hints.NumberHintReferenceBase;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.PlaceholderTextRanges;

import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.config.InfraPlaceholderReference;
import cn.taketoday.assistant.app.application.config.InfraReplacementTokenResolver;

class InfraApplicationYamlValueReferenceProvider extends PsiReferenceProvider {
  static final String INSPECTION_ID = new InfraApplicationYamlInspection().getID();
  private static final Key<InfraApplicationYamlInspection> INSPECTION_KEY = Key.create(INSPECTION_ID);

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    List<TextRange> valueTextRanges;
    YAMLKeyValue psiElement = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
    if (psiElement == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    YAMLScalar yamlScalar = (YAMLScalar) element;
    if (yamlScalar.isMultiline()) {
      return PsiReference.EMPTY_ARRAY;
    }
    PsiReference[] placeholderReferences = ConfigPlaceholderReference.createPlaceholderReferences(element, InfraPlaceholderReference::new);
    MetaConfigKeyReference<?> keyReference = ContainerUtil.findInstance(psiElement.getReferences(), MetaConfigKeyReference.class);
    MetaConfigKey key = keyReference != null ? keyReference.getResolvedKey() : null;
    if (key == null) {
      return placeholderReferences;
    }
    if (canHaveMultipleValues(psiElement.getValue(), key)) {
      valueTextRanges = MicroservicesConfigUtils.getListValueRanges(element);
    }
    else {
      valueTextRanges = Collections.singletonList(ElementManipulators.getValueTextRange(element));
    }
    List<Couple<String>> replacementTokens = getReplacementTokens(element);
    String elementText = element.getText();
    List<TextRange> rangesWithoutReplacementTokens = new SmartList<>();
    for (TextRange range : valueTextRanges) {
      String rangeText = range.substring(elementText);
      boolean isRangeWithoutReplacementTokens = true;
      for (Couple<String> tokens : replacementTokens) {
        String prefix = tokens.first;
        String suffix = tokens.second;
        if (StringUtil.contains(rangeText, prefix) && StringUtil.contains(rangeText, suffix)) {
          Set<TextRange> ranges = PlaceholderTextRanges.getPlaceholderRanges(rangeText, prefix, suffix);
          if (!ranges.isEmpty()) {
            isRangeWithoutReplacementTokens = false;
          }
          for (TextRange textRange : ranges) {
            PsiReference tokenReference = InfraReplacementTokenResolver.createReference(element, textRange.shiftRight(range.getStartOffset()));
            placeholderReferences = ArrayUtil.append(placeholderReferences, tokenReference);
          }
        }
      }
      if (isRangeWithoutReplacementTokens) {
        rangesWithoutReplacementTokens.add(range);
      }
    }
    if (key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP) && !ConfigYamlUtils.isConfigKeyPath(keyReference)) {
      return placeholderReferences;
    }
    if (!key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      context.put(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY_TEXT, ConfigYamlUtils.getQualifiedConfigKeyName(psiElement));
    }
    context.put(NumberHintReferenceBase.NUMBER_VALUE_SANITIZER_KEY, ConfigYamlUtils.getYamlNumberValueSanitizer());
    PsiReference[] providerReferences = InfraHintReferencesProvider.getInstance().getValueReferences(module, key, psiElement, element, rangesWithoutReplacementTokens, context);
    for (int i = 0; i < providerReferences.length; i++) {
      PsiReference reference = providerReferences[i];
      if (reference instanceof BooleanHintReference) {
        providerReferences[i] = new BooleanHintReference(reference.getElement(), reference.getRangeInElement(), "on", "off");
      }
    }
    return ArrayUtil.mergeArrays(providerReferences, placeholderReferences);
  }

  private static boolean canHaveMultipleValues(YAMLValue valueElement, MetaConfigKey key) {
    if ((valueElement instanceof YAMLSequence) || key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return false;
    }
    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      return true;
    }
    PsiType mapValueType = key.getEffectiveValueType();
    if (mapValueType == null) {
      return false;
    }
    MetaConfigKey.AccessType mapValueAccessType = MetaConfigKey.AccessType.forPsiType(mapValueType);
    return mapValueAccessType == MetaConfigKey.AccessType.INDEXED;
  }

  static List<Couple<String>> getReplacementTokens(PsiElement element) {
    InspectionProfileImpl profile = InspectionProfileManager.getInstance(element.getProject()).getCurrentProfile();
    InfraApplicationYamlInspection tool = profile.getUnwrappedTool(INSPECTION_KEY, element);
    return tool == null ? Collections.emptyList() : tool.getReplacementTokens();
  }
}
