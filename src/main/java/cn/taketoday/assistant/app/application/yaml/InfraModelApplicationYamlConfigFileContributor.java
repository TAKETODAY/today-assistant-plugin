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

import com.intellij.microservices.jvm.config.ConfigKeyPathReferenceBase;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.assistant.profiles.InfraProfilesFactory;
import cn.taketoday.lang.Nullable;

public class InfraModelApplicationYamlConfigFileContributor extends InfraModelConfigFileContributor {

  public InfraModelApplicationYamlConfigFileContributor() {
    super(YAMLFileType.YML);
  }

  @Override

  public List<ConfigurationValueResult> findConfigurationValues(PsiFile configFile, ConfigurationValueSearchParams params) {
    VirtualFile virtualFile = configFile.getVirtualFile();
    params.getProcessedFiles().add(virtualFile);
    YAMLFile yamlFile = ObjectUtils.tryCast(configFile, YAMLFile.class);
    if (yamlFile == null) {
      return Collections.emptyList();
    }
    String fileName = yamlFile.getVirtualFile().getNameWithoutExtension();
    String profileSuffix = StringUtil.substringAfter(fileName, "-");
    if (!isProfileRelevant(params, profileSuffix)) {
      return Collections.emptyList();
    }
    MetaConfigKey configKey = params.getConfigKey();
    String keyName = configKey.getName();
    MetaConfigKeyManager.ConfigKeyNameBinder binder = InfraApplicationMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(params.getModule());
    boolean multipleOccurrencesPossible = configKey.isAccessType(MetaConfigKey.AccessType.ENUM_MAP, MetaConfigKey.AccessType.MAP, MetaConfigKey.AccessType.INDEXED);
    boolean processParts = multipleOccurrencesPossible && !(params.getKeyIndex() == null && params.getKeyProperty() == null);
    SmartList<ConfigurationValueResult> smartList = new SmartList<>();
    List<YAMLDocument> documents = yamlFile.getDocuments();
    if (!documents.isEmpty()) {
      boolean processAllDocuments = params.isProcessAllProfiles();
      for (int i = documents.size() - 1; i >= 0; i--) {
        YAMLDocument document = documents.get(i);
        if (processAllDocuments || isProfileRelevantDocument(document, params, profileSuffix)) {
          processImports(params, virtualFile, smartList, i);
          int documentId = i;
          Processor<YAMLKeyValue> processor = keyValue -> {
            ProgressManager.checkCanceled();
            String qualifiedKeyName = ConfigYamlUtils.getQualifiedConfigKeyName(keyValue);
            if (qualifiedKeyName.equals(keyName)) {
              if (processParts) {
                if (configKey.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
                  return processIndexedKeyValue(params, smartList, keyValue, documentId);
                }
                return true;
              }
              processYamlKeyValue(params, smartList, keyValue, null, documentId);
              return false;
            }
            else if (!params.getCheckRelaxedNames() && !qualifiedKeyName.startsWith(keyName)) {
              return true;
            }
            else {
              if (processParts) {
                String keyIndexText = binder.bindsToKeyProperty(configKey, params.getKeyProperty(), qualifiedKeyName);
                if (keyIndexText == null) {
                  return true;
                }
                if (params.getKeyIndex() == null || keyIndexText.equals(params.getKeyIndex())) {
                  processYamlKeyValue(params, smartList, keyValue, keyIndexText, documentId);
                  return true;
                }
                return true;
              }
              else if (binder.bindsTo(configKey, qualifiedKeyName)) {
                processYamlKeyValue(params, smartList, keyValue, null, documentId);
                return false;
              }
              else {
                return true;
              }
            }
          };
          if (!ConfigYamlAccessor.processAllKeys(document, processor, true) && !processAllDocuments) {
            break;
          }
        }
      }
    }
    return smartList;
  }

  private static void processYamlKeyValue(ConfigurationValueSearchParams params, List<ConfigurationValueResult> results, YAMLKeyValue yamlKeyValue, String keyIndexText, int documentId) {
    PsiElement keyElement = yamlKeyValue.getKey();
    if (keyElement == null) {
      return;
    }
    YAMLValue value = yamlKeyValue.getValue();
    if (value instanceof YAMLSequence sequence) {
      for (YAMLSequenceItem item : ContainerUtil.reverse(sequence.getItems())) {
        YAMLValue value2 = item.getValue();
        String valueText = value2 instanceof YAMLScalar yamlScalar ? yamlScalar.getTextValue() : "";
        results.add(new YamlConfigurationValueResult(yamlKeyValue, keyElement, keyIndexText, value2, valueText, documentId, params));
      }
    }
    else if (value instanceof YAMLMapping mapping) {
      for (YAMLKeyValue mapKeyValue : ContainerUtil.reverse(new ArrayList<>(mapping.getKeyValues()))) {
        if (mapKeyValue.getKey() != null) {
          results.add(new YamlConfigurationValueResult(mapKeyValue, mapKeyValue.getKey(),
                  keyIndexText, mapKeyValue.getValue(), mapKeyValue.getValueText(), documentId, params));
        }
      }
    }
    else {
      results.add(new YamlConfigurationValueResult(yamlKeyValue, keyElement, keyIndexText, value, yamlKeyValue.getValueText(), documentId, params));
    }
  }

  private static boolean processIndexedKeyValue(ConfigurationValueSearchParams params,
          List<ConfigurationValueResult> results, YAMLKeyValue yamlKeyValue, int documentId) {
    PsiElement keyElement = yamlKeyValue.getKey();
    if (keyElement == null || params.getKeyIndex() == null || params.getKeyProperty() != null) {
      return true;
    }
    YAMLValue value = yamlKeyValue.getValue();
    if (!(value instanceof YAMLSequence sequence)) {
      return true;
    }
    try {
      int index = Integer.parseInt(params.getKeyIndex());
      List<YAMLSequenceItem> items = sequence.getItems();
      if (index < 0 || index >= items.size()) {
        return true;
      }
      YAMLSequenceItem item = items.get(index);
      YAMLValue itemValue = item.getValue();
      if (!(itemValue instanceof YAMLScalar)) {
        return true;
      }
      results.add(new YamlConfigurationValueResult(yamlKeyValue, keyElement, params.getKeyIndex(), itemValue, itemValue.getText(), documentId, params));
      return false;
    }
    catch (NumberFormatException e) {
      return true;
    }
  }

  private static class YamlConfigurationValueResult extends ConfigurationValueResult {

    private final YAMLKeyValue myValue;

    YamlConfigurationValueResult(YAMLKeyValue yamlKeyValue, PsiElement keyElement,
            @Nullable String keyIndexText, @Nullable PsiElement valueElement, @Nullable String valueText,
            int documentId, ConfigurationValueSearchParams params) {
      super(keyElement, keyIndexText, valueElement, convertValue(yamlKeyValue, valueText, params), documentId, params);
      this.myValue = yamlKeyValue;
    }

    @Override

    public MetaConfigKeyReference<?> getMetaConfigKeyReference() {
      MetaConfigKeyReference<?> metaConfigKeyReference = ContainerUtil.findInstance(this.myValue.getReferences(), MetaConfigKeyReference.class);
      if (metaConfigKeyReference == null) {
        String referencesText = StringUtil.join(ContainerUtil.map(this.myValue.getReferences(), ref -> ref.getClass().getName()), "|");
        throw new IllegalStateException(this.myValue.getClass() + " - " + this.myValue.getText() + " - " + referencesText);
      }
      return metaConfigKeyReference;
    }

    private static String convertValue(YAMLKeyValue yamlKeyValue, String valueText, ConfigurationValueSearchParams params) {
      String sanitizedValue = ConfigYamlUtils.sanitizeNumberValueIfNeeded(valueText, () -> getEffectiveValueType(yamlKeyValue, params));
      if (sanitizedValue != null) {
        return sanitizedValue;
      }
      if (StringUtil.isEmpty(valueText)) {
        return valueText;
      }
      char first = valueText.charAt(0);
      if (first != 'o' && first != 'O') {
        return valueText;
      }
      String aliasOf = null;
      if (StringUtil.equalsIgnoreCase(valueText, "on")) {
        aliasOf = "true";
      }
      else if (StringUtil.equalsIgnoreCase(valueText, "off")) {
        aliasOf = "false";
      }
      if (aliasOf == null) {
        return valueText;
      }
      PsiClass typeClass = PsiTypesUtil.getPsiClass(getEffectiveValueType(yamlKeyValue, params));
      String typeName = typeClass == null ? null : typeClass.getQualifiedName();
      return "java.lang.Boolean".equals(typeName) ? aliasOf : valueText;
    }

    public static PsiType getEffectiveValueType(YAMLKeyValue yamlKeyValue, ConfigurationValueSearchParams params) {
      PsiType valueType;
      MetaConfigKey configKey = params.getConfigKey();
      return (configKey.isAccessType(MetaConfigKey.AccessType.NORMAL) || (valueType = getBeanPropertyValueType(
              yamlKeyValue)) == null) ? configKey.getEffectiveValueElementType() : valueType;
    }
  }

  private static PsiType getBeanPropertyValueType(YAMLKeyValue yamlKeyValue) {
    ConfigKeyPathReferenceBase configKeyPathReference;
    PsiReference[] references = yamlKeyValue.getReferences();
    MetaConfigKeyReference<?> metaConfigKeyReference = ContainerUtil.findInstance(references, MetaConfigKeyReference.class);
    if (ConfigYamlUtils.isConfigKeyPath(metaConfigKeyReference)
            && (configKeyPathReference = ContainerUtil.findInstance(references, ConfigKeyPathReferenceBase.class)) != null) {
      return configKeyPathReference.getValueElementType();
    }
    return null;
  }

  private static boolean isProfileRelevantDocument(YAMLDocument document, ConfigurationValueSearchParams params, String fileProfile) {
    Set<String> activeProfiles = params.getActiveProfiles();
    if (ContainerUtil.isEmpty(activeProfiles)) {
      return true;
    }
    ConfigYamlAccessor accessor = new ConfigYamlAccessor(document, params.getModule(), InfraApplicationMetaConfigKeyManager.getInstance());
    String profileText = getProfileByKey(InfraMetadataConstant.INFRA_PROFILES_KEY, accessor, null);
    if (profileText == null) {
      profileText = getProfileByKey(InfraMetadataConstant.INFRA_CONFIG_ACTIVE_ON_PROFILE_KEY, accessor, fileProfile);
    }
    if (profileText == null) {
      return true;
    }
    try {
      Predicate<Set<String>> profiles = InfraProfilesFactory.getInstance().parseProfileExpressions(StringUtil.split(profileText, ","));
      return profiles.test(activeProfiles);
    }
    catch (InfraProfilesFactory.MalformedProfileExpressionException e) {
      return false;
    }
  }

  private static String getProfileByKey(String key, ConfigYamlAccessor accessor, String fileProfile) {
    YAMLKeyValue profilesKey = accessor.findExistingKey(key);
    return profilesKey != null ? profilesKey.getValueText() : fileProfile;
  }
}
