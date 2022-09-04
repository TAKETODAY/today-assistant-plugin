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

package cn.taketoday.assistant.app.run.lifecycle.tabs;

import com.intellij.diagnostic.logging.AdditionalTabComponent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.JBTabsPaneImpl;
import com.intellij.ui.TabbedPane;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.containers.ContainerUtil;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.LiveProperty;
import cn.taketoday.assistant.app.run.statistics.InfraRunUsageTriggerCollector;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;
import static cn.taketoday.assistant.app.run.InfraRunBundle.messagePointer;

public class ApplicationEndpointsTab extends AdditionalTabComponent {
  private final InfraApplicationRunConfigurationBase runConfiguration;
  private final ProcessHandler processHandler;
  private final TabbedPaneWrapper wrapper;
  private final List<EndpointTab<?>> tabs;
  private DefaultActionGroup actionGroup;
  private final Map<EndpointTab<?>, ActionGroup> tabActions;
  private final LiveProperty.LivePropertyListener readyStateListener;
  private InfraApplicationLifecycleManager.InfoListener infoListener;

  public ApplicationEndpointsTab(InfraApplicationRunConfigurationBase runConfiguration, ProcessHandler processHandler) {
    super(new BorderLayout());
    this.tabs = new ArrayList<>();
    this.tabActions = new HashMap<>();
    this.runConfiguration = runConfiguration;
    this.processHandler = processHandler;
    this.wrapper = new TabbedPaneWrapper(this) {
      @Override
      protected TabbedPane createTabbedPane(int tabPlacement) {
        TabbedPane createTabbedPane = super.createTabbedPane(tabPlacement);
        if (createTabbedPane instanceof JBTabsPaneImpl impl) {
          JBTabs tabs = impl.getTabs();
          DefaultActionGroup popupGroup = new DefaultActionGroup();
          popupGroup.add(new FocusOnStartAction(tabs));
          tabs.setPopupGroup(popupGroup, "unknown", true);
        }
        return createTabbedPane;
      }
    };
    add(this.wrapper.getComponent(), "Center");
    for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
      ContainerUtil.addIfNotNull(this.tabs, endpoint.createEndpointTab(runConfiguration, processHandler));
    }
    Endpoint.EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
      @Override
      public void extensionRemoved(Endpoint<?> extension, PluginDescriptor pluginDescriptor) {
        for (int i = 0; i < tabs.size(); i++) {
          EndpointTab<?> tab = tabs.get(i);
          if (tab.getId().equals(extension.getId())) {
            wrapper.removeTabAt(i);
            tabs.remove(tab);
            if (actionGroup != null) {
              actionGroup.remove(tabActions.remove(tab));
            }
            Disposer.dispose(tab);
            return;
          }
        }
      }
    }, this);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      DumbService.getInstance(getProject()).runReadActionInSmartMode(() -> {
        this.tabs.forEach(EndpointTab::checkAvailability);
      });
    });
    String selectedTabId = InfraEndpointsTabSettings.from(getProject()).getSelectedTab();
    for (EndpointTab<?> tab : tabs) {
      wrapper.addTab(tab.getTitle(), tab.getIcon(), tab.getComponent(), null);
      int tabIndex = wrapper.getTabCount() - 1;
      tab.setTooltipChangeListener(tooltip -> {
        wrapper.setToolTipTextAt(tabIndex, tooltip);
      });
      if (tab.getId().equals(selectedTabId)) {
        wrapper.setSelectedIndex(tabIndex);
      }
      Disposer.register(this, tab);
    }
    this.readyStateListener = new LiveProperty.LivePropertyListener() {

      @Override
      public void propertyChanged() { }

      @Override
      public void computationFailed(Exception e) {
        for (EndpointTab<?> tab2 : tabs) {
          tab2.showMessage(
                  message("infra.application.endpoints.application.ready.check.failed", e.getLocalizedMessage()));
        }
      }
    };
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      info.getReadyState().addPropertyListener(this.readyStateListener);
      for (EndpointTab<?> tab2 : this.tabs) {
        tab2.initPropertyListeners(info);
      }
    }
    else {
      this.infoListener = new InfraApplicationLifecycleManager.InfoListener() {

        @Override
        public void infoAdded(ProcessHandler handler, InfraApplicationInfo info2) {
          if (processHandler.equals(handler)) {
            info2.getReadyState().addPropertyListener(readyStateListener);
            for (EndpointTab<?> tab3 : tabs) {
              tab3.initPropertyListeners(info2);
            }
          }
        }

        @Override
        public void infoRemoved(ProcessHandler handler, InfraApplicationInfo info2) {
          if (processHandler.equals(handler)) {
            InfraApplicationLifecycleManager.from(getProject()).removeInfoListener(this);
            tabs.forEach(EndpointTab::infoRemoved);
            AppUIUtil.invokeLaterIfProjectAlive(getProject(), () -> {
              for (int i = 0; i < wrapper.getTabCount(); i++) {
                wrapper.setIconAt(i, IconLoader.getDisabledIcon(tabs.get(i).getIcon()));
              }
            });
          }
        }
      };
      InfraApplicationLifecycleManager.from(getProject()).addInfoListener(this.infoListener);
    }
    DataManager.registerDataProvider(this.wrapper.getComponent(), dataId -> {
      EndpointTab<?> selectedTab = getSelectedTab();
      if (selectedTab != null) {
        return selectedTab.getData(dataId);
      }
      return null;
    });
    this.wrapper.addChangeListener(new ChangeListener() {
      private EndpointTab<?> selectedTab;

      {
        this.selectedTab = getSelectedTab();
      }

      @Override
      public void stateChanged(ChangeEvent e) {
        EndpointTab<?> oldSelectedTab = this.selectedTab;
        this.selectedTab = getSelectedTab();
        if (oldSelectedTab != this.selectedTab && this.selectedTab != null) {
          InfraRunUsageTriggerCollector.logActuatorTabSelected(getProject(), this.selectedTab.getEndpoint().getClass());
        }
      }
    });
  }

  public void setSelected() {
    EndpointTab<?> selectedTab = getSelectedTab();
    if (selectedTab != null) {
      InfraRunUsageTriggerCollector.logActuatorTabSelected(getProject(), selectedTab.getEndpoint().getClass());
    }
  }

  public JComponent getPreferredFocusableComponent() {
    return this.wrapper.getComponent();
  }

  @Nullable
  public ActionGroup getToolbarActions() {
    this.actionGroup = new DefaultActionGroup();
    this.actionGroup.add(new AnAction() {
      @Override
      public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(messagePointer("infra.application.endpoints.refresh.action.name"));
        presentation.setDescription(messagePointer("infra.application.endpoints.refresh.action.description"));
        presentation.setIcon(AllIcons.Actions.Refresh);
        InfraApplicationInfo info = getInfo();
        EndpointTab<?> selectedTab = getSelectedTab();
        if (info != null && !Boolean.FALSE.equals(info.getReadyState().getValue()) && selectedTab != null) {
          selectedTab.updateRefreshAction(e, info);
        }
        else {
          presentation.setEnabled(false);
        }
      }

      @Override
      public void actionPerformed(AnActionEvent e) {
        InfraApplicationInfo info = getInfo();
        if (info != null && info.getReadyState().getValue() == null) {
          info.getReadyState().compute();
          tabs.forEach(EndpointTab::showLoading);
          return;
        }
        tabs.get(wrapper.getSelectedIndex()).refresh();
      }
    });
    this.actionGroup.addSeparator();
    for (EndpointTab<?> tab : this.tabs) {
      DefaultActionGroup actionGroup = new DefaultActionGroup() {
        public void update(AnActionEvent e) {
          e.getPresentation().setVisible(tab == getSelectedTab());
        }
      };
      actionGroup.addAll(tab.getToolbarActions());
      this.actionGroup.add(actionGroup);
      this.tabActions.put(tab, actionGroup);
    }
    return this.actionGroup;
  }

  @Nullable
  public JComponent getSearchComponent() {
    return null;
  }

  @Nullable
  public String getToolbarPlace() {
    return "unknown";
  }

  @Nullable
  public JComponent getToolbarContextComponent() {
    return this.wrapper.getComponent();
  }

  public boolean isContentBuiltIn() {
    return false;
  }

  public String getTabTitle() {
    return message("infra.application.endpoints.tab.title");
  }

  public void dispose() {
    if (this.infoListener != null) {
      InfraApplicationLifecycleManager.from(getProject()).removeInfoListener(this.infoListener);
    }
    InfraApplicationInfo info = getInfo();
    if (info != null) {
      info.getReadyState().removePropertyListener(this.readyStateListener);
    }
  }

  private Project getProject() {
    return this.runConfiguration.getProject();
  }

  @Nullable
  private EndpointTab<?> getSelectedTab() {
    if (this.wrapper.getSelectedIndex() >= 0) {
      return this.tabs.get(this.wrapper.getSelectedIndex());
    }
    return null;
  }

  @Nullable
  private InfraApplicationInfo getInfo() {
    Project project = getProject();
    if (project.isDisposed()) {
      return null;
    }
    return InfraApplicationLifecycleManager.from(project).getInfraApplicationInfo(this.processHandler);
  }

  private class FocusOnStartAction extends AnAction implements Toggleable {
    private final JBTabs myTabs;

    FocusOnStartAction(JBTabs tabs) {
      super(ActionsBundle.actionText("Runner.FocusOnStartup"), ActionsBundle.actionDescription("Runner.FocusOnStartup"), null);
      this.myTabs = tabs;
    }

    public void update(AnActionEvent e) {
      EndpointTab<?> currentTab = getCurrentTab();
      boolean visible = currentTab != null;
      e.getPresentation().setVisible(visible);
      if (visible) {
        Toggleable.setSelected(e.getPresentation(), isToFocus(currentTab));
      }
    }

    public void actionPerformed(AnActionEvent e) {
      EndpointTab<?> currentTab = getCurrentTab();
      if (currentTab == null) {
        return;
      }
      boolean isToFocus = isToFocus(currentTab);
      InfraEndpointsTabSettings.from(getProject()).setSelectedTab(isToFocus ? null : currentTab.getId());
    }

    private boolean isToFocus(EndpointTab<?> tab) {
      return tab.getId().equals(InfraEndpointsTabSettings.from(getProject()).getSelectedTab());
    }

    @Nullable
    private EndpointTab<?> getCurrentTab() {
      TabInfo info = this.myTabs.getTargetInfo();
      int index = info == null ? wrapper.getSelectedIndex() : this.myTabs.getIndexOf(info);
      if (index >= 0) {
        return tabs.get(index);
      }
      return null;
    }
  }
}
