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

package cn.taketoday.assistant.model.config;

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.lang.Nullable;

public final class InfraConfigValueSearcher {

  private final Module module;
  private final boolean includeTests;
  private final String configKey;
  private final boolean checkRelaxedNames;
  private final Set<String> activeProfiles;
  private final String keyIndex;
  private final String keyProperty;

  public static InfraConfigValueSearcher productionForAllProfiles(
          Module module, String configKey) {
    return new InfraConfigValueSearcher(module, false, configKey,
            false, ConfigurationValueSearchParams.PROCESS_ALL_VALUES, null, null);
  }

  public static InfraConfigValueSearcher productionForProfiles(
          Module module, String configKey, @Nullable Set<String> set) {
    return new InfraConfigValueSearcher(module, false, configKey, false, set, null, null);
  }

  public static InfraConfigValueSearcher productionForProfiles(
          Module module, String configKey, @Nullable Set<String> set,
          @Nullable String keyIndex, @Nullable String keyProperty) {
    return new InfraConfigValueSearcher(module, false, configKey, false, set, keyIndex, keyProperty);
  }

  public InfraConfigValueSearcher(Module module, boolean includeTests, String configKey,
          boolean checkRelaxedNames, @Nullable Set<String> set, @Nullable String keyIndex,
          @Nullable String keyProperty) {
    this.module = module;
    this.includeTests = includeTests;
    this.configKey = configKey;
    this.checkRelaxedNames = checkRelaxedNames;
    this.activeProfiles = set;
    this.keyIndex = keyIndex;
    this.keyProperty = keyProperty;
  }

  @Nullable
  public static Set<String> clearDefaultTestProfile(@Nullable Set<String> set) {
    if (set == null) {
      return null;
    }
    if (set.contains(InfraProfile.DEFAULT_TEST_PROFILE_NAME)) {
      LinkedHashSet<String> result = new LinkedHashSet<>(set);
      result.remove(InfraProfile.DEFAULT_TEST_PROFILE_NAME);
      return result;
    }
    return set;
  }

  @Nullable
  public String findValueText() {
    Ref<String> valueText = Ref.create();
    Processor<ConfigurationValueResult> findValueTextProcessor = result -> {
      String text = result.getValueText();
      if (text != null) {
        valueText.set(text);
        return false;
      }
      return true;
    };
    process(findValueTextProcessor);
    return valueText.get();
  }

  public boolean process(Processor<ConfigurationValueResult> processor) {
    MetaConfigKey metaConfigKey = InfraApplicationMetaConfigKeyManager.of().findApplicationMetaConfigKey(module, configKey);
    if (metaConfigKey == null) {
      return true;
    }
    MetaConfigKey.Deprecation deprecation = metaConfigKey.getDeprecation();
    if (deprecation.getLevel() == MetaConfigKey.Deprecation.DeprecationLevel.ERROR) {
      return true;
    }
    PsiManager psiManager = PsiManager.getInstance(module.getProject());
    Set<String> activeProfiles = clearDefaultTestProfile(this.activeProfiles);
    var params = new ConfigurationValueSearchParams(module, checkRelaxedNames,
            activeProfiles, metaConfigKey, keyIndex, keyProperty, false, null);

    for (VirtualFile configFile : InfraConfigurationFileService.of().findConfigFiles(module, includeTests)) {
      var contributor = InfraModelConfigFileContributor.getContributor(configFile);
      if (contributor != null) {
        PsiFile configPsiFile = psiManager.findFile(configFile);
        if (configPsiFile != null) {
          var result = contributor.findConfigurationValues(configPsiFile, params);
          if (!ContainerUtil.process(result, processor)) {
            return false;
          }
        }
      }
    }
    return true;
  }

}
