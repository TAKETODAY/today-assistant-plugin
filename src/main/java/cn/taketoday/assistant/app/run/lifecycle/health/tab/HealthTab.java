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

package cn.taketoday.assistant.app.run.lifecycle.health.tab;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.AutoExpandSimpleNodeListener;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Alarm;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.LiveProperty;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.assistant.app.run.lifecycle.tabs.InfraEndpointsTabSettings;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;
import static cn.taketoday.assistant.app.run.InfraRunBundle.messagePointer;

public class HealthTab extends EndpointTab<Map> {
  static final String HEALTH_CHECK = "HEALTH_CHECK";
  private final JComponent myComponent;
  private final HealthTreeStructure myTreeStructure;
  private final StructureTreeModel<? extends SimpleTreeStructure> myTreeModel;
  private final TreeExpander myExpander;
  private final Alarm myAlarm;
  private boolean myPendingHealthCheck;
  private final LiveProperty.LivePropertyListener myHealthListener;

  public HealthTab(Endpoint<Map> endpoint, InfraApplicationRunConfigurationBase runConfiguration, ProcessHandler processHandler) {
    super(endpoint, runConfiguration, processHandler);
    this.myTreeStructure = new HealthTreeStructure(getProject());
    this.myTreeModel = new StructureTreeModel<>(this.myTreeStructure, this);
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(this.myTreeModel, this);
    Tree tree = new Tree(asyncTreeModel);
    asyncTreeModel.addTreeModelListener(new AutoExpandSimpleNodeListener(tree));
    tree.setRootVisible(false);
    tree.setShowsRootHandles(false);
    tree.setCellRenderer(new NodeRenderer());
    new TreeSpeedSearch(tree, TreeSpeedSearch.NODE_DESCRIPTOR_TOSTRING, true);
    this.myExpander = new DefaultTreeExpander(tree);
    this.myComponent = ScrollPaneFactory.createScrollPane(tree, 20, 31);
    this.myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    new UiNotifyConnector(getRootPanel(), new Activatable() {
      public void showNotify() {
        if (HealthTab.this.myPendingHealthCheck) {
          HealthTab.this.myPendingHealthCheck = false;
          HealthTab.this.doScheduleHealthCheck();
        }
      }
    });
    this.myHealthListener = new LiveProperty.LivePropertyListener() {
      @Override
      public void propertyChanged() {
      }

      @Override
      public void computationFinished() {
        HealthTab.this.scheduleHealthCheck();
      }
    };
    getProject().getMessageBus().connect(this).subscribe(InfraEndpointsTabSettings.TOPIC, new InfraEndpointsTabSettings.Listener() {
      @Override
      public void settingsChanged(String changeType) {
        if (changeType.equals(HEALTH_CHECK)) {
          HealthTab.this.scheduleHealthCheck();
        }
      }
    });
  }

  @Override
  public String getTitle() {
    return message("infra.application.endpoints.health.tab.title");
  }

  @Override

  public Icon getIcon() {
    Icon icon = Icons.SpringBootHealth;
    return icon;
  }

  @Override
  public void dispose() {
    super.dispose();
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      getLiveProperty(info).removePropertyListener(this.myHealthListener);
    }
  }

  @Override

  protected List<AnAction> getToolbarActions() {
    List<AnAction> actions = new ArrayList<>();
    actions.add(new ExpandAllAction());
    actions.add(new CollapseAllAction());
    actions.add(Separator.getInstance());
    actions.add(new AnAction(message("infra.application.endpoints.configure.health.tab.action.name"), null, AllIcons.General.GearPlain) {

      public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(HealthTab.this.getProject(), InfraAppBundle.message("infra.name"));
      }
    });
    return actions;
  }

  @Override
  protected void doInitPropertyListeners(InfraApplicationInfo info) {
    getLiveProperty(info).addPropertyListener(this.myHealthListener);
  }

  @Override

  protected JComponent getEndpointComponent() {
    JComponent jComponent = this.myComponent;
    return jComponent;
  }

  @Override
  public void doUpdateComponent(@Nullable Map value) {
    this.myTreeStructure.setHealth(value);
    this.myTreeModel.invalidate();
  }

  @Override
  public void updateRefreshAction(AnActionEvent e, InfraApplicationInfo info) {
    super.updateRefreshAction(e, info);
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    HealthEndpointTabSettings settings = HealthEndpointTabSettings.getInstance(project);
    if (settings.isCheckHealth() && Boolean.TRUE.equals(info.getReadyState().getValue())) {
      Presentation presentation = e.getPresentation();
      presentation.setIcon(ExecutionUtil.getLiveIndicator(AllIcons.Actions.Refresh));
      presentation.setText(
              messagePointer("infra.application.endpoints.health.tab.refresh.action.name", TimeUnit.MILLISECONDS.toSeconds(settings.getHealthCheckDelay())));
    }
  }

  private void scheduleHealthCheck() {
    AppUIExecutor.onUiThread().expireWith(this).submit(() -> {
      if (getRootPanel().isShowing()) {
        this.myPendingHealthCheck = false;
        doScheduleHealthCheck();
        return;
      }
      this.myPendingHealthCheck = true;
    });
  }

  private void doScheduleHealthCheck() {
    Project project = getProject();
    if (!project.isOpen() || project.isDisposed()) {
      return;
    }
    HealthEndpointTabSettings settings = HealthEndpointTabSettings.getInstance(project);
    this.myAlarm.cancelAllRequests();
    long delay = settings.getHealthCheckDelay();
    if (settings.isCheckHealth() && delay > 0 && !this.myAlarm.isDisposed()) {
      this.myAlarm.addRequest(() -> {
        InfraApplicationInfo info = getInfo();
        if (info != null) {
          getLiveProperty(info).compute();
        }
      }, delay);
    }
  }

  private class ExpandAllAction extends AnAction {

    ExpandAllAction() {
      ActionUtil.copyFrom(this, "ExpandAll");
      registerCustomShortcutSet(getShortcutSet(), HealthTab.this.myComponent);
    }

    public void actionPerformed(AnActionEvent e) {
      if (HealthTab.this.myExpander.canExpand()) {
        HealthTab.this.myExpander.expandAll();
      }
    }

    public void update(AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(HealthTab.this.myExpander.canExpand());
    }
  }

  private class CollapseAllAction extends AnAction {

    CollapseAllAction() {
      ActionUtil.copyFrom(this, "CollapseAll");
      registerCustomShortcutSet(getShortcutSet(), HealthTab.this.myComponent);
    }

    public void actionPerformed(AnActionEvent e) {
      if (HealthTab.this.myExpander.canCollapse()) {
        HealthTab.this.myExpander.collapseAll();
      }
    }

    public void update(AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(HealthTab.this.myExpander.canCollapse());
    }
  }
}
