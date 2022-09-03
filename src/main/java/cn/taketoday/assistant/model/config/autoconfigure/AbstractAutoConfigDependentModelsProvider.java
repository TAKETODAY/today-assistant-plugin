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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.util.PairProcessor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.facet.AppInfraFileSetEditorCustomization;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModelDependentModelsProvider;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContextBase;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class AbstractAutoConfigDependentModelsProvider extends LocalAnnotationModelDependentModelsProvider {

  protected abstract List<PsiClass> getAutoConfigClasses(
          LocalAnnotationModel localAnnotationModel, ConditionalOnEvaluationContext conditionalOnEvaluationContext);

  protected abstract LocalModelDependencyType getModelDependencyType();

  public boolean processCustomDependentLocalModels(LocalAnnotationModel localAnnotationModel, PairProcessor<? super LocalModel, ? super LocalModelDependency> processor) {
    if (!acceptModel(localAnnotationModel)) {
      return true;
    }
    for (Pair<LocalModel<?>, LocalModelDependency> pair : getAutoConfigModels(localAnnotationModel)) {
      if (!processor.process(pair.getFirst(), pair.getSecond())) {
        return false;
      }
    }
    return true;
  }

  protected boolean acceptModel(LocalAnnotationModel model) {
    return isInfraConfigured(model.getModule());
  }

  private Set<Pair<LocalModel<?>, LocalModelDependency>> getAutoConfigModels(LocalAnnotationModel localAnnotationModel) {
    Module module = localAnnotationModel.getModule();
    boolean nonStrictEvaluation = isNonStrictEvaluation(module);
    NotNullLazyValue<List<VirtualFile>> configFilesCache = createConfigFilesCache(module, localAnnotationModel.getActiveProfiles());
    Set<Pair<LocalModel<?>, LocalModelDependency>> autoConfigModels = new LinkedHashSet<>();
    ConditionalOnEvaluationContext sharedContext = createContext(localAnnotationModel.getConfig(), module, localAnnotationModel.getActiveProfiles(), configFilesCache, null);
    List<PsiClass> autoConfigClasses = getAutoConfigClasses(localAnnotationModel, sharedContext);
    for (PsiClass autoConfigClass : autoConfigClasses) {
      sharedContext = processConfigurationClass(autoConfigModels, autoConfigClass, nonStrictEvaluation, sharedContext, module, localAnnotationModel.getActiveProfiles(), configFilesCache);
    }
    return autoConfigModels;
  }

  private static ConditionalOnEvaluationContext createContext(PsiClass psiClass, Module module, Set<String> activeProfiles,
          NotNullLazyValue<List<VirtualFile>> configFilesCache, @Nullable ConditionalOnEvaluationContext sharedContext) {
    ConditionalOnEvaluationContextBase useSharedContext = sharedContext instanceof ConditionalOnEvaluationContextBase ? (ConditionalOnEvaluationContextBase) sharedContext : null;
    return new ConditionalOnEvaluationContextBase(psiClass, module, activeProfiles, configFilesCache, useSharedContext);
  }

  static NotNullLazyValue<List<VirtualFile>> createConfigFilesCache(Module module, Set<String> activeProfiles) {
    boolean includeTestScope = activeProfiles != null && activeProfiles.contains("_DEFAULT_TEST_PROFILE_NAME_");
    return NotNullLazyValue.lazy(() -> {
      return InfraConfigurationFileService.of().findConfigFiles(module, includeTestScope);
    });
  }

  private static boolean isNonStrictEvaluation(Module module) {
    InfraFacet facet = InfraFacet.from(module);
    CustomSetting.BOOLEAN setting = facet != null ? facet.findSetting(AppInfraFileSetEditorCustomization.NON_STRICT_SETTING) : null;
    return setting == null || setting.getBooleanValue();
  }

  private ConditionalOnEvaluationContext processConfigurationClass(Set<Pair<LocalModel<?>, LocalModelDependency>> autoConfigModels, PsiClass autoConfigClass,
          boolean nonStrictEvaluation, @Nullable ConditionalOnEvaluationContext sharedContext, Module module, Set<String> activeProfiles,
          NotNullLazyValue<List<VirtualFile>> configFilesCache) {
    ProgressManager.checkCanceled();
    ConditionalOnEvaluationContext context = createContext(autoConfigClass, module, activeProfiles, configFilesCache, sharedContext);
    AutoConfigClassConditionEvaluator evaluator = new AutoConfigClassConditionEvaluator(autoConfigClass,
            nonStrictEvaluation, context);
    if (!evaluator.isActive()) {
      return context;
    }
    LocalAnnotationModel autoConfigModel = new AutoConfigLocalAnnotationModel(autoConfigClass, module, activeProfiles, nonStrictEvaluation,
            sharedContext);
    LocalModelDependency dependency = LocalModelDependency.create(
            "Auto-configuration @" + autoConfigClass.getQualifiedName(), getModelDependencyType(), autoConfigClass.getNavigationElement());
    autoConfigModels.add(Pair.create(autoConfigModel, dependency));
    for (PsiClass innerConfig : autoConfigClass.getInnerClasses()) {
      if (innerConfig.hasModifierProperty("static")
              && JamService.getJamService(module.getProject()).getJamElement(Configuration.JAM_KEY, innerConfig) != null) {
        processConfigurationClass(autoConfigModels, innerConfig, nonStrictEvaluation, context, module, activeProfiles, configFilesCache);
      }
    }
    return context;
  }

  protected static boolean isInfraConfigured(Module module) {
    return InfraUtils.hasFacet(module) && InfraLibraryUtil.hasFrameworkLibrary(module);
  }
}
