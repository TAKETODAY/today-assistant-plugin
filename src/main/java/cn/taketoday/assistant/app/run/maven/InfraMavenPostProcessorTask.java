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

package cn.taketoday.assistant.app.run.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import org.jetbrains.idea.maven.project.MavenConsole;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;

import cn.taketoday.assistant.app.options.InfrastructureSettings;
import cn.taketoday.assistant.app.run.InfraInitializrRunConfigurationService;

/**
 * auto create run config
 */
public class InfraMavenPostProcessorTask implements MavenProjectsProcessorTask {
  private final Module module;

  public InfraMavenPostProcessorTask(Module module) {
    this.module = module;
  }

  @Override
  public void perform(Project project, MavenEmbeddersManager manager,
          MavenConsole console, MavenProgressIndicator indicator) {
    InfrastructureSettings configuration = InfrastructureSettings.getInstance(project);
    if (configuration.isAutoCreateRunConfiguration()) {
      InfraInitializrRunConfigurationService.from(project).createRunConfiguration(module);
    }
  }
}
