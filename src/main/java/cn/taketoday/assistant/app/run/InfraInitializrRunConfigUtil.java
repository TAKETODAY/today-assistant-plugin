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

package cn.taketoday.assistant.app.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiClass;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;

import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.lang.Nullable;

final class InfraInitializrRunConfigUtil {

  static void createRunConfiguration(Executor executorService, Module module, Disposable executorDisposable) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      createRunConfiguration(InfraApplicationConfigurationType.of(), module);
    }
    else {
      ReadAction.nonBlocking(() -> {
                createRunConfiguration(InfraApplicationConfigurationType.of(), module);
              })
              .inSmartMode(module.getProject())
              .expireWith(executorDisposable)
              .expireWith(module)
              .coalesceBy(InfraInitializrRunConfigUtil.class, module)
              .submit(executorService);
    }
  }

  public static void createRunConfiguration(InfraApplicationConfigurationType type, Module module) {
    List<PsiClass> applications = InfraApplicationService.of().getInfraApplications(module);
    for (PsiClass app : applications) {
      if (!hasRunConfiguration(app, type, module)
              && InfraApplicationService.of().hasMainMethod(app)) {
        createRunConfiguration(app.getQualifiedName(), type, module);
      }
    }
  }

  private static void createRunConfiguration(
          @Nullable String mainClass, InfraApplicationConfigurationType type, Module module) {

    if (mainClass == null) {
      return;
    }
    try {
      RunManager runManager = RunManager.getInstance(module.getProject());
      RunnerAndConfigurationSettings settings =
              runManager.createConfiguration("", type.getDefaultConfigurationFactory());
      InfraApplicationRunConfigurationBase newRunConfig = (InfraApplicationRunConfigurationBase) settings.getConfiguration();
      newRunConfig.setModule(module);
      newRunConfig.setInfraMainClass(mainClass);
      settings.setName(newRunConfig.suggestedName());
      runManager.setUniqueNameIfNeeded(settings);
      runManager.addConfiguration(settings);
      if (runManager.getAllSettings().size() == 1) {
        runManager.setSelectedConfiguration(settings);
      }
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable t) {
      Logger.getInstance(InfraInitializrRunConfigUtil.class)
              .error("Error creating Infra run configuration for " + mainClass, t);
    }
  }

  private static boolean hasRunConfiguration(
          PsiClass applicationClass, ConfigurationType type, Module module) {
    Module configModule;
    RunManager runManager = RunManager.getInstance(module.getProject());
    for (RunConfiguration configuration : runManager.getConfigurationsList(type)) {
      if (configuration instanceof InfraApplicationRunConfigurationBase infraConfig) {
        if (infraConfig.getInfraMainClass().equals(applicationClass.getQualifiedName())
                && (configModule = infraConfig.getModule()) != null) {
          if (configModule.equals(module)) {
            return true;
          }
          var dependentModules = new HashSet<Module>();
          ModuleUtilCore.collectModulesDependsOn(module, dependentModules);
          if (dependentModules.contains(configModule)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
