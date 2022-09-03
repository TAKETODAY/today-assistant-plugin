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

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.properties.ConfigKeyPathReferenceProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;

import java.util.Collections;

import cn.taketoday.assistant.ReferencePatternConditions;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.config.hints.LoggerNameReferenceProvider;

public class InfraApplicationPropertiesReferenceContributor extends PsiReferenceContributor {
  private static final String LOGGING_LEVEL_KEY_PREFIX = "logging.level.";

  static final class Holder {
    static final PatternCondition<PsiElement> APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER = new PatternCondition<>("isApplicationPropertiesAndSB_1_2") {

      public boolean accepts(PsiElement element, ProcessingContext context) {
        if (!ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION.accepts(element, context)) {
          return false;
        }
        PsiFile containingFile = element.getContainingFile().getOriginalFile();
        if (!(containingFile instanceof PropertiesFile)) {
          return false;
        }
        return InfraConfigurationFileService.of().isApplicationConfigurationFile(containingFile);
      }
    };

  }

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registerKeyProviders(registrar);
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyValueImpl.class)
                    .with(Holder.APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER),
            new InfraApplicationPropertiesValueReferenceProvider(), 100.0d);
  }

  private static void registerKeyProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class)
                    .with(Holder.APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER),
            new InfraApplicationPropertiesKeyReferenceProvider());
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class)
            .with(Holder.APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER)
            .with(new PatternCondition<>("logging.level") {

              public boolean accepts(PropertyKeyImpl key, ProcessingContext context) {
                return StringUtil.startsWith(key.getText(), LOGGING_LEVEL_KEY_PREFIX);
              }
            }), new PsiReferenceProvider() {

      public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        TextRange textRange = ElementManipulators.getValueTextRange(element);
        int startInElement = LOGGING_LEVEL_KEY_PREFIX.length();
        if (startInElement <= textRange.getLength()) {
          TextRange shiftedTextRange = textRange.shiftRight(startInElement).grown(-startInElement);
          LoggerNameReferenceProvider provider = new LoggerNameReferenceProvider(false);
          return provider.getReferences(element, Collections.singletonList(shiftedTextRange), context);
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });
    Condition<MetaConfigKey> mapTypeKeyCondition = configKey -> {
      return configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP);
    };
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl.class).with(Holder.APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER)
            .with(new PatternCondition<>("mapTypeKey") {

              public boolean accepts(PropertyKeyImpl key, ProcessingContext context) {
                return MetaConfigKeyReference.findAndStoreMetaConfigKeyIfMatches(key, context, mapTypeKeyCondition);
              }
            }), new PsiReferenceProvider() {

      public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        MetaConfigKey configKey2 = context.get(MetaConfigKeyReference.META_CONFIG_KEY);
        TextRange textRange = ElementManipulators.getValueTextRange(element);
        int startInElement = configKey2.getName().length() + 1;
        if (startInElement <= textRange.getLength()) {
          TextRange shiftedTextRange = textRange.shiftRight(startInElement).grown(-startInElement);
          InfraHintReferencesProvider hintReferencesProvider = InfraHintReferencesProvider.getInstance();
          return hintReferencesProvider.getKeyReferences(configKey2, element, shiftedTextRange, context);
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl.class).with(Holder.APPLICATION_PROPERTIES_SB_1_2_OR_HIGHER).with(new PatternCondition<PropertyKeyImpl>("isMapOrIndexedKey") {

              public boolean accepts(PropertyKeyImpl key, ProcessingContext context) {
                return MetaConfigKeyReference.findAndStoreMetaConfigKeyIfMatches(key, context, MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION);
              }
            }), new ConfigKeyPathReferenceProvider());
  }
}
