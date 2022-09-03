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
package cn.taketoday.assistant.toolWindow;

import com.intellij.ProjectTopics;
import com.intellij.facet.ProjectWideFacetAdapter;
import com.intellij.facet.ProjectWideFacetListener;
import com.intellij.facet.ProjectWideFacetListenersRegistry;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.ContextHelpAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBusConnection;

import java.util.List;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public abstract class InfraBaseView extends SimpleToolWindowPanel implements Disposable {

  protected final Project myProject;

  protected FinderRecursivePanel<?> myRootPanel;

  protected InfraBaseView(Project project) {
    super(false, true);
    myProject = project;

    refreshContentPanel();
  }

  protected abstract FinderRecursivePanel<?> createRootPanel();

  private void refreshContentPanel() {
    myRootPanel = createRootPanel();
    myRootPanel.initPanel();
    setContent(myRootPanel);

    Disposer.register(this, myRootPanel);
  }

  /**
   * Invoked from Project/Facet settings.
   */
  protected void performFullUpdate() {
    AppUIExecutor.onUiThread().expireWith(this).submit(() -> {
      FinderRecursivePanel<?> oldPanel = myRootPanel;
      remove(oldPanel);
      Disposer.dispose(oldPanel);

      refreshContentPanel();
    });
  }

  protected void performDetailsUpdate() {
    FinderRecursivePanel<?> panel = myRootPanel;
    while (true) {
      if (!(panel.getSecondComponent() instanceof FinderRecursivePanel)) {
        panel.updateRightComponent(true);
        break;
      }
      panel = (FinderRecursivePanel<?>) panel.getSecondComponent();
    }
  }

  private void updateSelectedPath(Object... pathToSelect) {
    myRootPanel.updateSelectedPath(pathToSelect);
  }

  protected MessageBusConnection installProjectModuleListener() {
    ProjectWideFacetListener<? extends InfraFacet> myProjectWideFacetAdapter = new ProjectWideFacetAdapter<>() {
      @Override
      public void facetAdded(InfraFacet facet) {
        performFullUpdate();
      }

      @Override
      public void facetRemoved(InfraFacet facet) {
        performFullUpdate();
      }

      @Override
      public void facetConfigurationChanged(InfraFacet facet) {
        performFullUpdate();
      }
    };
    ProjectWideFacetListenersRegistry.getInstance(myProject)
            .registerListener(InfraFacet.FACET_TYPE_ID, myProjectWideFacetAdapter, this);

    MessageBusConnection messageBusConnection = myProject.getMessageBus().connect(this);
    messageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        performFullUpdate();
      }
    });
    messageBusConnection.subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void moduleAdded(Project project, Module module) {
        performFullUpdate();
      }

      @Override
      public void moduleRemoved(Project project, Module module) {
        performFullUpdate();
      }

      @Override
      public void modulesRenamed(Project project,
              List<? extends Module> modules,
              Function<? super Module, String> oldNameProvider) {
        performFullUpdate();
      }
    });
    return messageBusConnection;
  }

  /**
   * Performs recursive update for given selection path.
   *
   * @param project Project.
   * @param pathToSelect Path to select.
   * @param requestFocus Focus requested.
   * @param tabName Tab to select ({@link InfraToolWindowContent#getDisplayName}).
   */
  protected static void select(Project project, Object[] pathToSelect, boolean requestFocus, String tabName) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(InfraToolWindowContent.TOOL_WINDOW_ID);
    assert toolWindow != null;

    Runnable runnable = () -> {
      ContentManager contentManager = toolWindow.getContentManager();
      Content content = contentManager.findContent(tabName);

      if (content != null) {
        contentManager.setSelectedContentCB(content, requestFocus).doWhenDone(() -> {
          InfraBaseView infraBaseView = (InfraBaseView) content.getComponent();
          infraBaseView.updateSelectedPath(pathToSelect);
        });
      }
      else {
        // Tool window's content hasn't been initialized asynchronously yet.
        DataContext context = DataManager.getInstance().getDataContext(contentManager.getComponent());
        InfraToolWindowContentUpdater updater = InfraToolWindowContent.CONTENT_UPDATER.getData(context);
        assert updater != null;

        updater.update(() -> {
          Content updatedContent = contentManager.findContent(tabName);
          if (updatedContent == null) {
            // Corresponding tab is not available.
            return;
          }
          contentManager.setSelectedContentCB(updatedContent, requestFocus).doWhenDone(() -> {
            // Invoke active() again to avoid focus moving from selected path to the first column,
            // because tool window's FocusTask requests focus alarm with delay if component is not created.
            toolWindow.activate(() -> {
              InfraBaseView infraBaseView = (InfraBaseView) updatedContent.getComponent();
              infraBaseView.updateSelectedPath(pathToSelect);
            });
          });
        });
      }
    };
    if (requestFocus) {
      toolWindow.activate(runnable);
    }
    else {
      runnable.run();
    }
  }

  @Nullable
  @Override
  public Object getData(String dataId) {
    if (PlatformCoreDataKeys.HELP_ID.is(dataId)) {
      return getHelpId();
    }
    return super.getData(dataId);
  }

  @Override
  public void dispose() {

  }

  /**
   * @return Help ID for this view.
   */
  protected String getHelpId() {
    return "Reference.Infra.ToolWindow";
  }

  /**
   * @return Help action for use in toolbar.
   */
  protected AnAction getHelpAction() {
    return new ContextHelpAction(getHelpId());
  }
}