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

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKey.AccessType;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

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

public class InfraModelApplicationPropertiesConfigFileContributor extends InfraModelConfigFileContributor {

  public InfraModelApplicationPropertiesConfigFileContributor() {
    super(PropertiesFileType.INSTANCE);
  }

  @Override
  public List<ConfigurationValueResult> findConfigurationValues(PsiFile configFile, ConfigurationValueSearchParams params) {
    VirtualFile virtualFile = configFile.getVirtualFile();
    params.getProcessedFiles().add(virtualFile);
    PropertiesFile propertiesFile = ObjectUtils.tryCast(configFile, PropertiesFile.class);
    if (propertiesFile == null) {
      return Collections.emptyList();
    }
    String fileName = propertiesFile.getVirtualFile().getNameWithoutExtension();
    String profileSuffix = StringUtil.substringAfter(fileName, "-");
    if (!isProfileRelevant(params, profileSuffix)) {
      return Collections.emptyList();
    }
    var valueResults = new SmartList<ConfigurationValueResult>();
    List<List<IProperty>> documents = getDocuments(propertiesFile, params.getModule());
    if (!documents.isEmpty()) {
      boolean processAllDocuments = params.isProcessAllProfiles();
      for (int i = documents.size() - 1; i >= 0; i--) {
        List<IProperty> document = documents.get(i);
        if (processAllDocuments || isProfileRelevantDocument(document, params, profileSuffix)) {
          processImports(params, virtualFile, valueResults, i);
          if (!processDocument(document, i, params, valueResults) && !processAllDocuments) {
            break;
          }
        }
      }
    }
    return valueResults;
  }

  private static List<List<IProperty>> getDocuments(PropertiesFile propertiesFile, Module module) {
    if (InfraApplicationPropertiesUtil.isSupportMultiDocuments(module)) {
      return InfraApplicationPropertiesUtil.getDocuments(propertiesFile);
    }
    return new SmartList(propertiesFile.getProperties());
  }

  private static boolean processDocument(List<IProperty> document,
          int documentId, ConfigurationValueSearchParams params, List<ConfigurationValueResult> results) {
    MetaConfigKey configKey = params.getConfigKey();
    String keyName = configKey.getName();
    List<IProperty> reversed = ContainerUtil.reverse(document);
    if (configKey.isAccessType(AccessType.NORMAL, AccessType.INDEXED)) {
      IProperty byExactName = null;

      for (IProperty property : reversed) {
        if (keyName.equals(property.getKey())) {
          byExactName = property;
          break;
        }
      }

      if (byExactName != null) {
        results.add(createResult(params, byExactName, null, documentId));
        return false;
      }
      else if (!params.getCheckRelaxedNames() && configKey.isAccessType(AccessType.NORMAL)) {
        return true;
      }
    }
    var binder = InfraApplicationMetaConfigKeyManager.of().getConfigKeyNameBinder(params.getModule());
    boolean multipleOccurrencesPossible = configKey.isAccessType(AccessType.ENUM_MAP, AccessType.MAP, AccessType.INDEXED);
    boolean processParts = multipleOccurrencesPossible && !(params.getKeyIndex() == null && params.getKeyProperty() == null);
    boolean found = false;
    for (IProperty property : reversed) {
      ProgressManager.checkCanceled();
      String propertyName = property.getName();
      if (propertyName != null && (params.getCheckRelaxedNames() || propertyName.startsWith(keyName))) {
        if (processParts) {
          String keyIndexText = binder.bindsToKeyProperty(configKey, params.getKeyProperty(), propertyName);
          if (keyIndexText != null && (params.getKeyIndex() == null || keyIndexText.equals(params.getKeyIndex()))) {
            results.add(createResult(params, property, keyIndexText, documentId));
            found = true;
          }
        }
        else if (binder.bindsTo(configKey, propertyName)) {
          results.add(createResult(params, property, null, documentId));
          if (!multipleOccurrencesPossible) {
            return false;
          }
          found = true;
        }
      }
    }
    return !found;
  }

  private static ConfigurationValueResult createResult(ConfigurationValueSearchParams params,
          IProperty property, @Nullable String keyIndexText, int documentId) {
    PropertyImpl propertyImpl = (PropertyImpl) property.getPsiElement();
    PropertyKeyImpl key = InfraApplicationPropertiesUtil.getPropertyKey(propertyImpl);
    return new ConfigurationValueResult(key, keyIndexText, InfraApplicationPropertiesUtil.getPropertyValue(propertyImpl), propertyImpl.getValue(), documentId, params);
  }

  private static boolean isProfileRelevantDocument(List<IProperty> document,
          ConfigurationValueSearchParams params, String fileProfile) {
    Set<String> activeProfiles = params.getActiveProfiles();
    if (ContainerUtil.isEmpty(activeProfiles)) {
      return true;
    }
    MetaConfigKeyManager.ConfigKeyNameBinder binder = InfraApplicationMetaConfigKeyManager.of().getConfigKeyNameBinder(params.getModule());
    String profileText = getProfileByKey(InfraMetadataConstant.INFRA_PROFILES_KEY, document, null, binder);
    if (profileText == null) {
      profileText = getProfileByKey(InfraMetadataConstant.INFRA_CONFIG_ACTIVE_ON_PROFILE_KEY, document, fileProfile, binder);
    }
    if (profileText == null) {
      return true;
    }
    try {
      Predicate<Set<String>> profiles = InfraProfilesFactory.of()
              .parseProfileExpressions(StringUtil.split(profileText, ","));
      return profiles.test(activeProfiles);
    }
    catch (InfraProfilesFactory.MalformedProfileExpressionException e) {
      return false;
    }
  }

  private static String getProfileByKey(String key, List<IProperty> document,
          String fileProfile, MetaConfigKeyManager.ConfigKeyNameBinder binder) {
    for (IProperty property : ContainerUtil.reverse(document)) {
      String propertyKey = property.getKey();
      if (propertyKey != null && binder.matchesPart(key, propertyKey)) {
        return property.getValue();
      }
    }
    return fileProfile;
  }
}
