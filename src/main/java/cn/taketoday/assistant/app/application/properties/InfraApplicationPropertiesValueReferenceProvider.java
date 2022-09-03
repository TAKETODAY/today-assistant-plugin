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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils;
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
import com.intellij.util.text.PlaceholderTextRanges;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.config.InfraPlaceholderReference;
import cn.taketoday.assistant.app.application.config.InfraReplacementTokenResolver;

class InfraApplicationPropertiesValueReferenceProvider extends PsiReferenceProvider {
  static final String INSPECTION_ID = new InfraApplicationPropertiesInspection().getID();
  private static final Key<InfraApplicationPropertiesInspection> INSPECTION_KEY = Key.create(INSPECTION_ID);

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    List<TextRange> valueTextRanges;
    PropertyImpl property = PsiTreeUtil.getParentOfType(element, PropertyImpl.class);
    if (property == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    PropertyKeyImpl propertyKey = InfraApplicationPropertiesUtil.getPropertyKey(property);
    if (propertyKey == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    PsiReference[] placeholderReferences = ConfigPlaceholderReference.createPlaceholderReferences(element, InfraPlaceholderReference::new);
    MetaConfigKey key = MetaConfigKeyReference.getResolvedMetaConfigKey(propertyKey);
    if (key == null) {
      return placeholderReferences;
    }
    if (canHaveMultipleValues(propertyKey, key)) {
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
    if (!key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      context.put(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY_TEXT, property.getKey());
    }
    PsiReference[] providerReferences = InfraHintReferencesProvider.getInstance().getValueReferences(module, key, propertyKey, element, rangesWithoutReplacementTokens, context);
    return ArrayUtil.mergeArrays(providerReferences, placeholderReferences);
  }

  private static boolean canHaveMultipleValues(PropertyKeyImpl propertyKey, MetaConfigKey key) {
    PsiType mapValueType;
    if (key.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return false;
    }
    if (key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
      return !isUsingIndexedAccess(propertyKey);
    }
    else if (isUsingIndexedAccess(propertyKey) || (mapValueType = key.getEffectiveValueType()) == null) {
      return false;
    }
    else {
      MetaConfigKey.AccessType mapValueAccessType = MetaConfigKey.AccessType.forPsiType(mapValueType);
      return mapValueAccessType == MetaConfigKey.AccessType.INDEXED;
    }
  }

  private static boolean isUsingIndexedAccess(PropertyKeyImpl propertyKey) {
    return propertyKey.textContains('[');
  }

  static List<Couple<String>> getReplacementTokens(PsiElement element) {
    InspectionProfileImpl profile = InspectionProfileManager.getInstance(element.getProject()).getCurrentProfile();
    InfraApplicationPropertiesInspection tool = profile.getUnwrappedTool(INSPECTION_KEY, element);
    return tool == null ? Collections.emptyList() : tool.getReplacementTokens();
  }
}
