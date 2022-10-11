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

package cn.taketoday.assistant.app.run.lifecycle.beans.tab;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.NaturalComparator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveContext;
import cn.taketoday.lang.Nullable;

class LiveContextsPanel extends LifecycleFinderRecursivePanel<LiveContext> {
  private static final String LIVE_CONTEXTS_PANEL_GROUP_ID = "LiveContextsPanel";

  LiveContextsPanel(Project project, InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    super(project, null, LIVE_CONTEXTS_PANEL_GROUP_ID, runConfiguration, processHandler);
  }

  protected List<LiveContext> getListItems() {
    LiveBeansModel model = getModel();
    if (model == null) {
      return Collections.emptyList();
    }
    List<LiveContext> contexts = model.getContexts();
    contexts.sort(Comparator.comparing(LiveContext::getName, NaturalComparator.INSTANCE));
    return contexts;
  }

  public String getItemText(LiveContext context) {
    return context.getName();
  }

  public boolean hasChildren(LiveContext context) {
    return true;
  }

  @Nullable
  public Icon getItemIcon(LiveContext context) {
    return Icons.FileSet;
  }

  @Nullable
  public JComponent createRightComponent(LiveContext context) {
    if (BeansEndpointTabSettings.getInstance(getProject()).isShowFiles()) {
      return new LiveResourcesPanel(getProject(), this, getGroupId(), getRunConfiguration(), getProcessHandler());
    }
    return new LiveBeansPanel(getProject(), this, getGroupId(), getRunConfiguration(), getProcessHandler());
  }

  protected boolean isEditable() {
    return false;
  }

  @Nullable
  public String getItemTooltipText(LiveContext context) {
    if (context.getParent() == null) {
      return InfraRunBundle.message("infra.application.endpoints.root.context");
    }
    return InfraRunBundle.message("infra.application.endpoints.parent.context", context.getParent().getName());
  }
}
