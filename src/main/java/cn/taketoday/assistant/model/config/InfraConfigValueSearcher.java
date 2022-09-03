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
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.internal.DefaultConstructorMarker;

public final class InfraConfigValueSearcher {
  private final Module myModule;
  private final boolean myIncludeTests;
  private final String myConfigKey;
  private final boolean myCheckRelaxedNames;
  private final Set<String> myActiveProfiles;
  private final String myKeyIndex;
  private final String myKeyProperty;

  public static InfraConfigValueSearcher productionForAllProfiles(Module module, String configKey) {
    return new InfraConfigValueSearcher(module, false, configKey, false, ConfigurationValueSearchParams.PROCESS_ALL_VALUES, null, null, 106, null);
  }

  public static InfraConfigValueSearcher productionForProfiles(Module module, String configKey, @Nullable Set<String> set) {
    return new InfraConfigValueSearcher(module, false, configKey, false, set, null, null, 106, null);
  }

  public static InfraConfigValueSearcher productionForProfiles(Module module, String configKey, @Nullable Set<String> set, @Nullable String keyIndex, @Nullable String keyProperty) {
    return new InfraConfigValueSearcher(module, false, configKey, false, set, keyIndex, keyProperty, 10, null);
  }

  public InfraConfigValueSearcher(Module myModule, boolean myIncludeTests, String myConfigKey,
          boolean myCheckRelaxedNames, @Nullable Set<String> set, @Nullable String myKeyIndex,
          @Nullable String myKeyProperty) {
    this.myModule = myModule;
    this.myIncludeTests = myIncludeTests;
    this.myConfigKey = myConfigKey;
    this.myCheckRelaxedNames = myCheckRelaxedNames;
    this.myActiveProfiles = set;
    this.myKeyIndex = myKeyIndex;
    this.myKeyProperty = myKeyProperty;
  }

  public InfraConfigValueSearcher(Module module, boolean z, String str, boolean z2, Set set, String str2, String str3, int i, DefaultConstructorMarker defaultConstructorMarker) {
    this(module, (i & 2) == 0 && z, str, (i & 8) != 0 || z2, (i & 16) != 0 ? null : set, (i & 32) != 0 ? null : str2, (i & 64) != 0 ? null : str3);
  }

  @Nullable
  public static Set<String> clearDefaultTestProfile(@Nullable Set<String> set) {
    if (set == null) {
      return null;
    }
    if (set.contains("_DEFAULT_TEST_PROFILE_NAME_")) {
      LinkedHashSet result = new LinkedHashSet(set);
      result.remove("_DEFAULT_TEST_PROFILE_NAME_");
      return result;
    }
    return set;
  }

  @Nullable
  public String findValueText() {
    Ref valueText = Ref.create();
    Processor<ConfigurationValueResult> findValueTextProcessor = new Processor<>() {
      public boolean process(ConfigurationValueResult result) {
        String text = result.getValueText();
        if (text != null) {
          valueText.set(text);
          return false;
        }
        return true;
      }
    };
    process(findValueTextProcessor);
    return (String) valueText.get();
  }

  public boolean process(Processor<ConfigurationValueResult> processor) {
    MetaConfigKey metaConfigKey = InfraApplicationMetaConfigKeyManager.getInstance().findApplicationMetaConfigKey(this.myModule, this.myConfigKey);
    if (metaConfigKey == null) {
      return true;
    }
    MetaConfigKey.Deprecation deprecation = metaConfigKey.getDeprecation();
    if (deprecation.getLevel() == MetaConfigKey.Deprecation.DeprecationLevel.ERROR) {
      return true;
    }
    PsiManager psiManager = PsiManager.getInstance(this.myModule.getProject());
    Set activeProfiles = clearDefaultTestProfile(this.myActiveProfiles);
    ConfigurationValueSearchParams params = new ConfigurationValueSearchParams(this.myModule, this.myCheckRelaxedNames, activeProfiles,
            metaConfigKey, this.myKeyIndex, this.myKeyProperty, false, null, 192, null);

    for (VirtualFile configFile : InfraConfigurationFileService.of().findConfigFiles(this.myModule, this.myIncludeTests)) {
      InfraModelConfigFileContributor contributor = InfraModelConfigFileContributor.getContributor(configFile);
      if (contributor != null) {
        PsiFile configPsiFile = psiManager.findFile(configFile);
        if (configPsiFile != null) {
          List result = contributor.findConfigurationValues(configPsiFile, params);
          if (!ContainerUtil.process(result, processor)) {
            return false;
          }
        }
        else {
          continue;
        }
      }
    }
    return true;
  }

}
