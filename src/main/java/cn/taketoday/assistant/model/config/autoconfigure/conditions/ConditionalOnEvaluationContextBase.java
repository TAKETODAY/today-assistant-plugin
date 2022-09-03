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
package cn.taketoday.assistant.model.config.autoconfigure.conditions;

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Processor;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.InfraModelConfigFileContributor;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.lang.Nullable;

public class ConditionalOnEvaluationContextBase extends UserDataHolderBase implements ConditionalOnEvaluationContext {

  private final PsiClass myAutoConfigClass;

  private final Module myModule;

  @Nullable
  private final Set<String> myActiveProfiles;

  private final NotNullLazyValue<? extends List<VirtualFile>> myConfigFilesCache;

  public ConditionalOnEvaluationContextBase(PsiClass autoConfigClass,
          Module module, @Nullable Set<String> activeProfiles,
          NotNullLazyValue<? extends List<VirtualFile>> configFilesCache,
          @Nullable ConditionalOnEvaluationContextBase sharedContext) {
    myModule = module;
    myAutoConfigClass = autoConfigClass;
    myActiveProfiles = activeProfiles;
    myConfigFilesCache = configFilesCache;

    if (sharedContext != null) {
      sharedContext.copyUserDataTo(this);
    }
  }

  @Override
  public PsiClass getAutoConfigClass() {
    return myAutoConfigClass;
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Nullable
  @Override
  public Set<String> getActiveProfiles() {
    return myActiveProfiles;
  }

  @Override
  public boolean processConfigurationValues(
          Processor<? super List<ConfigurationValueResult>> processor, boolean checkRelaxedNames, MetaConfigKey configKey) {
    PsiManager psiManager = PsiManager.getInstance(myModule.getProject());
    Set<String> activeProfiles = InfraConfigValueSearcher.clearDefaultTestProfile(myActiveProfiles);
    var searchParams = new ConfigurationValueSearchParams(myModule, checkRelaxedNames, activeProfiles, configKey);
    for (VirtualFile file : myConfigFilesCache.getValue()) {
      InfraModelConfigFileContributor contributor = InfraModelConfigFileContributor.getContributor(file);
      if (contributor == null) {
        continue;
      }

      PsiFile psiFile = psiManager.findFile(file);
      if (psiFile == null) {
        continue;
      }

      List<ConfigurationValueResult> values = contributor.findConfigurationValues(psiFile, searchParams);
      if (!processor.process(values)) {
        return false;
      }
    }
    return true;
  }
}
