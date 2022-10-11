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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.LayeredIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.app.run.InfraApplicationRunConfig;
import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.assistant.app.run.lifecycle.tabs.InfraEndpointsTabSettings;
import cn.taketoday.lang.Nullable;

public class LiveBeansTab extends EndpointTab<LiveBeansModel> {
  private static final String BEANS_MODE = "BEANS_MODE";
  private static final String BEANS_FULL = "BEANS_FULL";
  private static final String BEANS_UPDATE_DETAILS = "BEANS_UPDATE_DETAILS";
  private static final String BEANS_CONTENT = "BEANS_CONTENT";
  private final JComponent myWrapper;
  private final SimpleToolWindowPanel myMainPanel;
  private LifecycleFinderRecursivePanel myBeansPanel;
  private JComponent myDiagramPanel;
  private final DefaultActionGroup myDiagramActionGroup;
  private final UserDataHolder myDiagramDataHolder;
  private volatile boolean myNeedUpdateAfterRefresh;
  private final BeansEndpointTabSettings mySettings;

  public LiveBeansTab(Endpoint<LiveBeansModel> endpoint, InfraApplicationRunConfig runConfiguration, ProcessHandler processHandler) {
    super(endpoint, runConfiguration, processHandler);
    this.myMainPanel = new SimpleToolWindowPanel(false, true);
    this.myDiagramDataHolder = new UserDataHolderBase();
    this.mySettings = BeansEndpointTabSettings.getInstance(getProject());
    this.myDiagramActionGroup = new DefaultActionGroup() {

      public void update(AnActionEvent e) {
        e.getPresentation().setVisible(isDiagramMode() && !DumbService.isDumb(getProject()));
      }
    };
    refreshContentPanel();
    this.myWrapper = DumbService.getInstance(getProject()).wrapGently(this.myMainPanel, this);
    getProject().getMessageBus().connect(this).subscribe(InfraEndpointsTabSettings.TOPIC, new InfraEndpointsTabSettings.Listener() {

      @Override
      public void settingsChanged(String changeType) {
        if (changeType.equals(BEANS_MODE)) {
          performModeUpdate();
        }
        else if (changeType.equals(BEANS_FULL)) {
          performFullUpdate();
        }
        else if (changeType.equals(BEANS_UPDATE_DETAILS)) {
          performDetailsUpdate();
        }
        else if (changeType.equals(BEANS_CONTENT)) {
          updateContent();
        }
      }
    });
  }

  @Override
  public String getTitle() {
    return InfraRunBundle.message("infra.application.endpoints.beans.tab.title");
  }

  @Override

  public Icon getIcon() {
    Icon icon = cn.taketoday.assistant.Icons.SpringBean;
    return icon;
  }

  @Override

  protected List<AnAction> getToolbarActions() {
    List<AnAction> actions = new ArrayList<>();
    if (LiveBeansPanelContent.EP_NAME.getExtensions().length != 0) {
      actions.add(new EndpointToggleAction(InfraRunBundle.message("infra.application.endpoints.diagram.mode.action.name"), null,
              new LayeredIcon(AllIcons.FileTypes.Diagram, Icons.TodayOverlay)) {

        public boolean isSelected(AnActionEvent e) {
          return mySettings.isDiagramMode();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          mySettings.setDiagramMode(state);
          InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_MODE);
        }
      });
      actions.add(Separator.getInstance());
      actions.add(this.myDiagramActionGroup);
      actions.add(Separator.getInstance());
    }
    actions.add(new LiveBeansPanelToggleAction(InfraBundle.message("beans.view.show.library.beans.title"), null, AllIcons.Nodes.PpLib) {

      public boolean isSelected(AnActionEvent e) {
        return mySettings.isShowLibraryBeans();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        mySettings.setShowLibraryBeans(state);
        InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_CONTENT);
      }
    });
    actions.add(Separator.getInstance());
    actions.add(new LiveBeansPanelToggleAction(InfraRunBundle.message("infra.application.endpoints.show.context.action.name"), null, cn.taketoday.assistant.Icons.FileSet) {

      public boolean isSelected(AnActionEvent e) {
        return mySettings.isShowContexts();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        mySettings.setShowContexts(state);
        InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_FULL);
      }
    });
    actions.add(new LiveBeansPanelToggleAction(InfraBundle.message("beans.view.show.configuration.files"), null, cn.taketoday.assistant.Icons.SpringConfig) {

      public boolean isSelected(AnActionEvent e) {
        return mySettings.isShowFiles();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        mySettings.setShowFiles(state);
        InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_FULL);
      }
    });
    actions.add(Separator.getInstance());
    actions.add(new LiveBeansPanelToggleAction(InfraBundle.message("beans.view.show.bean.documentation.title"), null, AllIcons.Toolwindows.Documentation) {

      public boolean isSelected(AnActionEvent e) {
        return mySettings.isShowDoc();
      }

      public void setSelected(AnActionEvent e, boolean state) {
        mySettings.setShowDoc(state);
        InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_UPDATE_DETAILS);
      }
    });
    if (LiveBeansPanelContent.EP_NAME.getExtensions().length != 0) {
      actions.add(new LiveBeansPanelToggleAction(InfraBundle.message("beans.view.show.bean.graph.title"), null, AllIcons.FileTypes.Diagram) {

        public boolean isSelected(AnActionEvent e) {
          return mySettings.isShowLiveBeansGraph();
        }

        public void setSelected(AnActionEvent e, boolean state) {
          mySettings.setShowLiveBeansGraph(state);
          InfraEndpointsTabSettings.from(getProject()).fireSettingsChanged(BEANS_UPDATE_DETAILS);
        }
      });
    }
    return actions;
  }

  @Override

  protected JComponent getEndpointComponent() {
    JComponent jComponent = this.myWrapper;
    return jComponent;
  }

  @Override
  public void doUpdateComponent(LiveBeansModel value) {
    updateContent();
    this.myNeedUpdateAfterRefresh = true;
  }

  private void updateContent() {
    if (isDiagramMode()) {
      if (this.myDiagramPanel != null) {
        for (LiveBeansPanelContent content : LiveBeansPanelContent.EP_NAME.getExtensions()) {
          content.update(this.myDiagramDataHolder);
        }
      }
    }
    else if (this.myBeansPanel != null) {
      this.myBeansPanel.updateComponent();
    }
  }

  @Override
  protected String getErrorMessage(String cause) {
    return InfraRunBundle.message("infra.application.endpoints.error.failed.to.retrieve.application.beans.snapshot", cause);
  }

  @Override
  public void checkAvailability() {
  }

  @Override
  public void updateRefreshAction(AnActionEvent e, InfraApplicationInfo info) {
    e.getPresentation().setEnabled(true);
  }

  @Override
  @Nullable
  public Object getData(String dataId) {
    InfraApplicationInfo info;
    if (isDiagramMode() && (info = getInfo()) != null && Boolean.TRUE.equals(info.getReadyState().getValue()) && getLiveProperty(info).getValue() != null && !isTabLoading()) {
      for (LiveBeansPanelContent content : LiveBeansPanelContent.EP_NAME.getExtensions()) {
        Object data = content.getData(this.myDiagramDataHolder, dataId);
        if (data != null) {
          return data;
        }
      }
    }
    return super.getData(dataId);
  }

  private boolean isDiagramMode() {
    return LiveBeansPanelContent.EP_NAME.getExtensions().length != 0 && this.mySettings.isDiagramMode();
  }

  private void refreshContentPanel() {
    LifecycleFinderRecursivePanel lifecycleFinderRecursivePanel;
    if (isDiagramMode()) {
      if (this.myDiagramPanel == null) {
        for (LiveBeansPanelContent content : LiveBeansPanelContent.EP_NAME.getExtensions()) {
          this.myDiagramPanel = content.createComponent(getProject(), this.myDiagramDataHolder, this, () -> {
            LiveBeansModel liveBeansModel;
            InfraApplicationInfo info = getInfo();
            if (info != null && (liveBeansModel = getLiveProperty(info).getValue()) != null) {
              return liveBeansModel.getBeans();
            }
            return Collections.emptyList();
          }, getRunConfiguration(), false);
          this.myDiagramActionGroup.removeAll();
          this.myDiagramActionGroup.add(content.createToolbarActions(this.myDiagramDataHolder));
        }
      }
      lifecycleFinderRecursivePanel = (LifecycleFinderRecursivePanel) this.myDiagramPanel;
    }
    else {
      if (this.myBeansPanel == null) {
        this.myBeansPanel = createRootBeansPanel();
        this.myBeansPanel.initPanel();
        Disposer.register(this, this.myBeansPanel);
      }
      lifecycleFinderRecursivePanel = this.myBeansPanel;
    }
    this.myMainPanel.setContent(lifecycleFinderRecursivePanel);
    if (this.myNeedUpdateAfterRefresh) {
      updateContent();
      this.myNeedUpdateAfterRefresh = false;
    }
  }

  private LifecycleFinderRecursivePanel createRootBeansPanel() {
    if (this.mySettings.isShowContexts()) {
      return new LiveContextsPanel(getProject(), getRunConfiguration(), getProcessHandler());
    }
    if (this.mySettings.isShowFiles()) {
      return new LiveResourcesPanel(getProject(), getRunConfiguration(), getProcessHandler());
    }
    return new LiveBeansPanel(getProject(), getRunConfiguration(), getProcessHandler());
  }

  private void performModeUpdate() {
    ApplicationManager.getApplication().invokeLater(this::refreshContentPanel, ModalityState.NON_MODAL, o -> {
      return Disposer.isDisposed(this);
    });
  }

  private void performFullUpdate() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (this.myBeansPanel == null) {
        return;
      }
      FinderRecursivePanel oldPanel = this.myBeansPanel;
      Disposer.dispose(oldPanel);
      this.myBeansPanel = null;
      refreshContentPanel();
    }, ModalityState.NON_MODAL, o -> Disposer.isDisposed(this));
  }

  private void performDetailsUpdate() {
    if (this.myBeansPanel == null) {
      return;
    }
    FinderRecursivePanel finderRecursivePanel = this.myBeansPanel;
    while (true) {
      FinderRecursivePanel panel = finderRecursivePanel;
      if (!(panel.getSecondComponent() instanceof FinderRecursivePanel)) {
        panel.updateRightComponent(true);
        return;
      }
      finderRecursivePanel = (FinderRecursivePanel) panel.getSecondComponent();
    }
  }

  private abstract class LiveBeansPanelToggleAction extends EndpointTab<LiveBeansModel>.EndpointToggleAction {

    LiveBeansPanelToggleAction(@NlsActions.ActionText @Nullable String text, @NlsActions.ActionDescription @Nullable String description, @Nullable Icon icon) {
      super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setVisible(!isDiagramMode());
    }
  }
}
