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

import com.intellij.debugger.ui.HotSwapUIImpl;
import com.intellij.openapi.project.Project;
import com.intellij.task.ModuleResourcesBuildTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ModuleResourcesBuildTaskImpl;
import com.intellij.task.impl.ProjectTaskList;

import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationDescriptor;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

final class UpdateResourcesPolicy extends InfraApplicationUpdatePolicy {
  private static final String ID = "UpdateResources";

  UpdateResourcesPolicy() {
    super(ID, message("infra.update.policy.resources.name"), message("infra.update.policy.resources.description"));
  }

  @Override
  public void runUpdate(InfraApplicationUpdateContext context) {
    Project project = context.getProject();
    List<ModuleResourcesBuildTask> moduleResourcesBuildTasks = context.getDescriptors().stream().map(InfraApplicationDescriptor::getModule).distinct().map(ModuleResourcesBuildTaskImpl::new)
            .collect(Collectors.toList());
    ProjectTaskContext projectTaskContext = new ProjectTaskContext(context.isOnFrameDeactivation()).withUserData(HotSwapUIImpl.SKIP_HOT_SWAP_KEY, true);
    ProjectTaskManager.getInstance(project).run(projectTaskContext, new ProjectTaskList(moduleResourcesBuildTasks));
  }
}
