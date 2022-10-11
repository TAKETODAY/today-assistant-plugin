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

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.HotSwapStatusListener;
import com.intellij.debugger.ui.HotSwapUI;
import com.intellij.debugger.ui.HotSwapUIImpl;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ModuleBuildTaskImpl;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.InfraRunBundle;

final class UpdateClassesAndTriggerFilePolicy extends InfraApplicationUpdatePolicy implements cn.taketoday.assistant.app.run.update.TriggerFilePolicy {
  private static final String ID = "UpdateClassesAndTriggerFile";

  UpdateClassesAndTriggerFilePolicy() {
    super(ID, InfraRunBundle.message("infra.update.policy.classes.trigger.name"),
            InfraRunBundle.message("infra.update.policy.classes.trigger.description"));
  }

  @Override
  public boolean isAvailableOnFrameDeactivation() {
    return false;
  }

  @Override
  public boolean isAvailableForConfiguration(InfraApplicationRunConfig configuration) {
    return false;
  }

  @Override
  public void runUpdate(InfraApplicationUpdateContext context) {

    DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx(context.getProject());
    context.getDescriptors().forEach((descriptor) -> {
      DebugProcess process = debuggerManager.getDebugProcess(descriptor.getProcessHandler());
      DebuggerSession session = process != null ? debuggerManager.getSession(process) : null;
      ProjectTaskContext projectTaskContext = new ProjectTaskContext(context.isOnFrameDeactivation());
      if (session != null) {
        projectTaskContext.withUserData(HotSwapUIImpl.SKIP_HOT_SWAP_KEY, true);
      }

      ProjectTaskManager.getInstance(context.getProject()).run(projectTaskContext, new ModuleBuildTaskImpl(descriptor.getModule(), true, true, false)).onSuccess((taskResult) -> {
        if (!taskResult.isAborted() && !taskResult.hasErrors() && !context.getProject().isDisposed()) {
          if (session != null) {
            HotSwapUI.getInstance(context.getProject()).reloadChangedClasses(session, false, new HotSwapStatusListener() {
              public void onFailure(List<DebuggerSession> sessions) {
                TriggerFilePolicy.updateTriggerFile(Collections.singleton(descriptor.getModule()));
              }
            });
          }
          else {
            TriggerFilePolicy.updateTriggerFile(Collections.singleton(descriptor.getModule()));
          }
        }

      });
    });
  }
}
