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

import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ModuleBuildTaskImpl;
import com.intellij.task.impl.ProjectTaskList;

import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationDescriptor;

final class UpdateClassesAndResourcesPolicy extends InfraApplicationUpdatePolicy {
  private static final String ID = "UpdateClassesAndResources";

  UpdateClassesAndResourcesPolicy() {
    super(ID, InfraRunBundle.message("infra.update.policy.classes.resources.name"),
            InfraRunBundle.message("infra.update.policy.classes.resources.description"));
  }

  @Override
  public void runUpdate(InfraApplicationUpdateContext context) {
    List<ModuleBuildTask> modulesBuildTasks = context.getDescriptors()
            .stream()
            .map(InfraApplicationDescriptor::getModule)
            .distinct()
            .map(ModuleBuildTaskImpl::new)
            .collect(Collectors.toList());
    ProjectTaskManager.getInstance(context.getProject())
            .run(new ProjectTaskContext(context.isOnFrameDeactivation()), new ProjectTaskList(modulesBuildTasks));
  }
}
