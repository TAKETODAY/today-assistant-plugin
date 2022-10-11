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

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.lang.Nullable;

final class InfraApplicationUrlPathEditAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    RunDashboardRunConfigurationNode node = ServiceViewActionUtils.getTarget(e, RunDashboardRunConfigurationNode.class);
    if (node == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    RunConfiguration runConfiguration = node.getConfigurationSettings().getConfiguration();
    if (!(runConfiguration instanceof InfraApplicationRunConfig)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    InfraApplicationInfo info = getInfo(node);
    if (info == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    Integer serverPort = info.getServerPort().getValue();
    e.getPresentation().setEnabledAndVisible(serverPort != null && serverPort > 0);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setText(getTemplatePresentation().getText() + "...");
    }
  }

  public void actionPerformed(AnActionEvent e) {
    InfraApplicationInfo info;
    RunDashboardRunConfigurationNode node = ServiceViewActionUtils.getTarget(e, RunDashboardRunConfigurationNode.class);
    if (node == null) {
      return;
    }
    RunConfiguration configuration = node.getConfigurationSettings().getConfiguration();
    if (!(configuration instanceof InfraApplicationRunConfig configurable) || (info = getInfo(node)) == null) {
      return;
    }
    new InfraApplicationUrlPathConfigurable(configuration.getProject(), configurable, info).show();
  }

  @Nullable
  private static InfraApplicationInfo getInfo(RunDashboardRunConfigurationNode node) {
    RunContentDescriptor descriptor = node.getDescriptor();
    if (descriptor != null && descriptor.getProcessHandler() != null) {
      ProcessHandler handler = descriptor.getProcessHandler();
      if (!handler.isProcessTerminated()) {
        return InfraApplicationLifecycleManager.from(node.getProject()).getInfraApplicationInfo(handler);
      }
      return null;
    }
    return null;
  }

  public ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}
