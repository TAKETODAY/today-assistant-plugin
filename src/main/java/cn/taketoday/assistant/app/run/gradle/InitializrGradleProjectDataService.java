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

package cn.taketoday.assistant.app.run.gradle;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.NonUrgentExecutor;

import java.util.Collection;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.options.InfrastructureSettings;
import cn.taketoday.assistant.app.run.InfraInitializrRunConfigurationService;
import cn.taketoday.lang.Nullable;

@Order(3000)
final class InitializrGradleProjectDataService extends AbstractProjectDataService<ModuleData, Void> {

  public Key<ModuleData> getTargetDataKey() {
    return ProjectKeys.MODULE;
  }

  public void onSuccessImport(Collection<DataNode<ModuleData>> imported, @Nullable ProjectData projectData, Project project, IdeModelsProvider modelsProvider) {
    if (projectData == null) {
      return;
    }
    InfrastructureSettings configuration = InfrastructureSettings.getInstance(project);
    if (!configuration.isAutoCreateRunConfiguration()) {
      return;
    }
    ReadAction.nonBlocking(() -> {
      if (!InfraLibraryUtil.hasFrameworkLibrary(project)) {
        return;
      }
      ProgressManager.getInstance().executeNonCancelableSection(() -> {
        for (Module module : modelsProvider.getModules()) {
          String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
          if (rootProjectPath != null
                  && rootProjectPath.equals(projectData.getLinkedExternalProjectPath())
                  && InfraLibraryUtil.hasFrameworkLibrary(module)) {
            InfraInitializrRunConfigurationService.from(project).createRunConfiguration(module);
          }
        }
      });
    }).inSmartMode(project).expireWith(configuration).submit(NonUrgentExecutor.getInstance());
  }
}
