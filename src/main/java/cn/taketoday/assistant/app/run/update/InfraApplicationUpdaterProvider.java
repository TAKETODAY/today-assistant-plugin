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

package cn.taketoday.assistant.app.run.update;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.update.RunningApplicationUpdater;
import com.intellij.execution.update.RunningApplicationUpdaterProvider;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;

import javax.swing.Icon;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationDescriptor;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

final class InfraApplicationUpdaterProvider implements RunningApplicationUpdaterProvider {

  public RunningApplicationUpdater createUpdater(Project project, ProcessHandler process) {
    var descriptor = InfraApplicationLifecycleManager.from(project).getInfraApplicationDescriptor(process);
    if (descriptor != null) {
      if (descriptor.getRunProfile() instanceof InfraApplicationRunConfiguration applicationRunConfiguration) {
        Executor executor = ExecutorRegistry.getInstance().getExecutorById(descriptor.getExecutorId());
        if (executor != null) {
          InfraApplicationUpdatePolicy policy = applicationRunConfiguration.getUpdateActionUpdatePolicy();
          if (policy != null
                  && policy.isAvailableForConfiguration(applicationRunConfiguration)
                  && policy.isAvailableForExecutor(executor)) {
            return new ApplicationUpdater(project, descriptor, policy);
          }
        }
      }
    }
    return null;
  }

  private static class ApplicationUpdater implements RunningApplicationUpdater {
    private final Project myProject;
    private final InfraApplicationDescriptor myDescriptor;
    private final InfraApplicationUpdatePolicy myPolicy;

    ApplicationUpdater(Project project, InfraApplicationDescriptor descriptor, InfraApplicationUpdatePolicy policy) {
      this.myProject = project;
      this.myDescriptor = descriptor;
      this.myPolicy = policy;
    }

    public String getDescription() {
      return message("infra.run.config.update.application", this.myDescriptor.getRunProfile().getName());
    }

    public String getShortName() {
      return this.myDescriptor.getRunProfile().getName();
    }

    public Icon getIcon() {
      return this.myDescriptor.getRunProfile().getIcon();
    }

    public void performUpdate(AnActionEvent event) {
      this.myPolicy.runUpdate(new InfraApplicationUpdateContextImpl(this.myProject, new SmartList<>(this.myDescriptor), false));
    }
  }
}
