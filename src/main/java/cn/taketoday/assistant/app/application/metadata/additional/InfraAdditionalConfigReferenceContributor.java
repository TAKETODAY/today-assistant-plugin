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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonPsiUtil;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.microservices.jvm.config.MetaConfigKey.Deprecation.DeprecationLevel;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.references.StaticStringValuesReferenceProvider;

public class InfraAdditionalConfigReferenceContributor extends PsiReferenceContributor {
  private static final Key<InfraAdditionalConfigPropertyNameReference.GroupContext> GROUP_CONTEXT_KEY = Key.create("groupContext");

  private static class Holder {
    private static final PatternCondition<JsonProperty> GROUP_CONTEXT = new PatternCondition<>("groupContext") {

      public boolean accepts(JsonProperty property, ProcessingContext context) {
        String propertyName = property.getName();
        InfraAdditionalConfigPropertyNameReference.GroupContext groupContext = InfraAdditionalConfigPropertyNameReference.GroupContext.forProperty(propertyName);
        if (groupContext == null) {
          return false;
        }
        if (InfraLibraryUtil.isAtLeastVersion(ModuleUtilCore.findModuleForPsiElement(property), groupContext.getMinimumVersion())) {
          context.put(GROUP_CONTEXT_KEY, groupContext);
          return true;
        }
        return true;
      }
    };
    private static final PsiElementPattern.Capture<JsonProperty> GROUP_CONTEXT_WITH_OBJECT = PlatformPatterns.psiElement(JsonProperty.class).with(GROUP_CONTEXT)
            .with(new PatternCondition<>("jsonObjectValue") {

              public boolean accepts(JsonProperty property, ProcessingContext context) {
                return property.getValue() instanceof JsonObject;
              }
            });
    private static final PsiElementPattern.Capture<JsonProperty> GROUP_CONTEXT_WITH_ARRAY = PlatformPatterns.psiElement(JsonProperty.class).with(GROUP_CONTEXT)
            .with(new PatternCondition<JsonProperty>("jsonArrayValue") {

              public boolean accepts(JsonProperty property, ProcessingContext context) {
                return property.getValue() instanceof JsonArray;
              }
            });
    private static final PsiFilePattern.Capture<JsonFile> ADDITIONAL_CONFIG_JSON = PlatformPatterns.psiFile(JsonFile.class).with(new PatternCondition<JsonFile>("isAdditionalJson") {

      public boolean accepts(JsonFile jsonFile, ProcessingContext context) {
        return InfraAdditionalConfigUtils.isAdditionalMetadataFile(jsonFile);
      }
    });
    private static final PsiFilePattern.Capture<JsonFile> SPRING_CONFIGURATION_METADATA_JSON = PlatformPatterns.psiFile(JsonFile.class)
            .withName(InfraConfigFileConstants.CONFIGURATION_METADATA_JSON);
    private static final PatternCondition<JsonStringLiteral> VALUE_PATTERN = new PatternCondition<JsonStringLiteral>("inPropertyValue") {

      public boolean accepts(JsonStringLiteral property, ProcessingContext context) {
        return JsonPsiUtil.isPropertyValue(property);
      }
    };
    private static final PatternCondition<JsonStringLiteral> PROPERTY_PATTERN = new PatternCondition<JsonStringLiteral>("inPropertyKey") {

      public boolean accepts(JsonStringLiteral property, ProcessingContext context) {
        return JsonPsiUtil.isPropertyKey(property);
      }
    };
    private static final PatternCondition<JsonProperty> NAME_PROPERTY = new PatternCondition<JsonProperty>("nameProperty") {

      public boolean accepts(JsonProperty property, ProcessingContext context) {
        return InfraMetadataConstant.NAME.equals(property.getName());
      }
    };

  }

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    JavaClassReferenceProvider originalClassReferenceProvider = new JavaClassReferenceProvider();
    originalClassReferenceProvider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
    PsiReferenceProvider javaClassReferenceProvider = new PsiReferenceProvider() {

      public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        JavaClassReferenceSet set = new JavaClassReferenceSet(ElementManipulators.getValueText(element), element, ElementManipulators.getOffsetInElement(element), false,
                originalClassReferenceProvider) {
          public boolean isAllowDollarInNames() {
            return true;
          }
        };
        return set.getAllReferences();
      }
    };
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class)
                    .inFile(StandardPatterns.or(Holder.ADDITIONAL_CONFIG_JSON, Holder.SPRING_CONFIGURATION_METADATA_JSON))
                    .with(Holder.VALUE_PATTERN).withParent(PlatformPatterns.psiElement(JsonProperty.class).with(new PatternCondition<JsonProperty>("classPropertyNames") {

                      public boolean accepts(JsonProperty property, ProcessingContext context) {
                        String name = property.getName();
                        return InfraMetadataConstant.TYPE.equals(name) || InfraMetadataConstant.SOURCE_TYPE.equals(name);
                      }
                    })).withSuperParent(4, Holder.GROUP_CONTEXT_WITH_ARRAY), javaClassReferenceProvider);
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class)
                    .inFile(StandardPatterns.or(Holder.ADDITIONAL_CONFIG_JSON, Holder.SPRING_CONFIGURATION_METADATA_JSON))
                    .with(Holder.VALUE_PATTERN)
                    .withParent(PlatformPatterns.psiElement(JsonProperty.class)
                            .with(new PatternCondition<>("targetProperty") {
                              public boolean accepts(JsonProperty property, ProcessingContext context) {
                                return InfraMetadataConstant.TARGET.equals(property.getName());
                              }
                            })).withSuperParent(3, Holder.GROUP_CONTEXT_WITH_OBJECT), javaClassReferenceProvider);
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.PROPERTY_PATTERN).withSuperParent(3, PlatformPatterns.psiFile(JsonFile.class)),
            new PsiReferenceProvider() {

              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                return new PsiReference[] { new InfraAdditionalConfigPropertyNameReference(element, InfraAdditionalConfigPropertyNameReference.GroupContext.FAKE_TOP_LEVEL) };
              }
            });
    PsiReferenceProvider propertyNamesProvider = new PsiReferenceProvider() {

      public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        InfraAdditionalConfigPropertyNameReference.GroupContext groupContext = context.get(
                GROUP_CONTEXT_KEY);
        if (groupContext == null) {
          return PsiReference.EMPTY_ARRAY;
        }
        return new PsiReference[] { new InfraAdditionalConfigPropertyNameReference(element, groupContext) };
      }
    };
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.PROPERTY_PATTERN).withSuperParent(4, Holder.GROUP_CONTEXT_WITH_ARRAY),
            propertyNamesProvider);
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.PROPERTY_PATTERN).withSuperParent(3, Holder.GROUP_CONTEXT_WITH_OBJECT),
            propertyNamesProvider);
    PatternCondition<JsonStringLiteral> deprecationGroupPattern = new PatternCondition<>("deprecationGroup") {

      public boolean accepts(JsonStringLiteral literal, ProcessingContext context) {
        return context.get(
                GROUP_CONTEXT_KEY) == InfraAdditionalConfigPropertyNameReference.GroupContext.DEPRECATION;
      }
    };
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.VALUE_PATTERN)
                    .withParent(PlatformPatterns.psiElement(JsonProperty.class).with(new PatternCondition<JsonProperty>("replacementProperty") {

                      public boolean accepts(JsonProperty jsonProperty, ProcessingContext context) {
                        return jsonProperty.getName().equals(InfraMetadataConstant.REPLACEMENT);
                      }
                    })).withSuperParent(3, Holder.GROUP_CONTEXT_WITH_OBJECT).with(deprecationGroupPattern), new PsiReferenceProvider() {

              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                return new PsiReference[] { new InfraAdditionalConfigMetaConfigKeyReference(element, InfraAdditionalConfigMetaConfigKeyReference.Mode.REPLACEMENT) };
              }
            });
    String[] deprecationLevels = ContainerUtil.map2Array(DeprecationLevel.values(), String.class, DeprecationLevel::getValue);
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.VALUE_PATTERN)
                    .withParent(PlatformPatterns.psiElement(JsonProperty.class).with(new PatternCondition<>("levelProperty") {

                      public boolean accepts(JsonProperty jsonProperty, ProcessingContext context) {
                        return jsonProperty.getName().equals(InfraMetadataConstant.LEVEL);
                      }
                    })).withSuperParent(3, Holder.GROUP_CONTEXT_WITH_OBJECT).with(deprecationGroupPattern), new StaticStringValuesReferenceProvider(false, deprecationLevels));
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class)
                    .inFile(Holder.ADDITIONAL_CONFIG_JSON)
                    .with(Holder.VALUE_PATTERN)
                    .withParent(PlatformPatterns.psiElement(JsonProperty.class).with(Holder.NAME_PROPERTY))
                    .withSuperParent(4, Holder.GROUP_CONTEXT_WITH_ARRAY)
                    .with(new PatternCondition<>("hintsProvidersGroup") {

                      public boolean accepts(JsonStringLiteral literal, ProcessingContext context) {
                        return context.get(
                                GROUP_CONTEXT_KEY) == InfraAdditionalConfigPropertyNameReference.GroupContext.HINTS_PROVIDERS;
                      }
                    }), new PsiReferenceProvider() {

              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                return new PsiReference[] { new InfraAdditionalConfigValueProviderReference(element) };
              }
            });
    registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(JsonStringLiteral.class).inFile(Holder.ADDITIONAL_CONFIG_JSON).with(Holder.VALUE_PATTERN)
                    .withParent(PlatformPatterns.psiElement(JsonProperty.class).with(Holder.NAME_PROPERTY)).withSuperParent(4, Holder.GROUP_CONTEXT_WITH_ARRAY)
                    .with(new PatternCondition<>("hintsGroup") {

                      public boolean accepts(JsonStringLiteral literal, ProcessingContext context) {
                        return context.get(
                                GROUP_CONTEXT_KEY) == InfraAdditionalConfigPropertyNameReference.GroupContext.HINTS;
                      }
                    }), new PsiReferenceProvider() {

              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                return new PsiReference[] { new InfraAdditionalConfigMetaConfigKeyReference(element, InfraAdditionalConfigMetaConfigKeyReference.Mode.HINTS) };
              }
            });
  }
}
