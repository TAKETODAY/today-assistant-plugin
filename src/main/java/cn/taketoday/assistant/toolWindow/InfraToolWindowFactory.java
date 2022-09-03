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
import com.intellij.facet.Facet;
import com.intellij.facet.ProjectWideFacetAdapter;
import com.intellij.facet.ProjectWideFacetListenersRegistry;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/27 13:13
 */
public final class InfraToolWindowFactory implements ToolWindowFactory {

  private static final ExtensionPointName<InfraToolWindowContent> CONTENT_EP_NAME
          = ExtensionPointName.create("cn.taketoday.assistant.toolWindowContent");

  @Override
  public void createToolWindowContent(Project project, ToolWindow toolWindow) {

    toolWindow.setAvailable(true);
    toolWindow.setToHideOnEmptyContent(true);
    toolWindow.setContentUiType(ToolWindowContentUiType.TABBED, null);
    ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addContentManagerListener(new InfraToolWindowFactory.MyContentManagerListener(project));
    InfraToolWindowContentUpdater updater = onDone -> asyncUpdateContents(project, contentManager, onDone);
    contentManager.addDataProvider((dataId) -> InfraToolWindowContent.CONTENT_UPDATER.is(dataId) ? updater : null);
    MessageBusConnection connection = project.getMessageBus().connect(contentManager);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      public void rootsChanged(ModuleRootEvent event) {
        asyncUpdateContents(project, contentManager);
      }
    });
    ProjectWideFacetListenersRegistry.getInstance(project).registerListener(new ProjectWideFacetAdapter<>() {
      public void facetAdded(Facet facet) {
        asyncUpdateContents(project, contentManager);
      }

      public void facetRemoved(Facet facet) {
        asyncUpdateContents(project, contentManager);
      }
    }, contentManager);
    CONTENT_EP_NAME.addExtensionPointListener(new ExtensionPointListener<>() {
      public void extensionAdded(InfraToolWindowContent extension, PluginDescriptor pluginDescriptor) {
        asyncUpdateContents(project, contentManager);
      }

      public void extensionRemoved(InfraToolWindowContent extension, PluginDescriptor pluginDescriptor) {

        Content existing = contentManager.findContent(extension.getDisplayName());
        if (existing != null) {
          contentManager.removeContent(existing, true);
        }

      }
    }, contentManager);
    asyncUpdateContents(project, contentManager);
  }

  private static void asyncUpdateContents(Project project, ContentManager contentManager) {
    asyncUpdateContents(project, contentManager, null);
  }

  private static void asyncUpdateContents(Project project, ContentManager contentManager, @Nullable Runnable onDone) {
    ReadAction.nonBlocking(() -> getToolWindowContentsByAvailability(project)).inSmartMode(project)
            .expireWith(contentManager)
            .coalesceBy(new Object[] { InfraToolWindowFactory.class, project, onDone })
            .finishOnUiThread(ModalityState.any(), (toolWindowsPartition) -> {
              updateToolWindowContents(contentManager, toolWindowsPartition.first, toolWindowsPartition.second);
              if (onDone != null) {
                onDone.run();
              }

            }).submit(AppExecutorUtil.getAppExecutorService());
  }

  private static Pair<List<InfraToolWindowContent>, List<InfraToolWindowContent>> getToolWindowContentsByAvailability(
          Project project) {
    List<InfraToolWindowContent> available = new SmartList<>();
    List<InfraToolWindowContent> notAvailable = new SmartList<>();

    for (InfraToolWindowContent windowContent : CONTENT_EP_NAME.getExtensions()) {
      (windowContent.getInstance().isAvailable(project) ? available : notAvailable).add(windowContent);
    }

    return Pair.create(available, notAvailable);
  }

  private static void updateToolWindowContents(
          ContentManager contentManager, List<InfraToolWindowContent> available, List<InfraToolWindowContent> notAvailable) {

    for (InfraToolWindowContent infraToolWindowContent : notAvailable) {
      Content existing = contentManager.findContent(infraToolWindowContent.getDisplayName());
      if (existing != null) {
        contentManager.removeContent(existing, true);
      }
    }

    ContentFactory factory = contentManager.getFactory();

    for (InfraToolWindowContent infraToolWindowContent : available) {
      Content existing = contentManager.findContent(infraToolWindowContent.getDisplayName());
      if (existing == null) {
        Content content = factory.createContent(new ToolWindowContentStub(infraToolWindowContent),
                infraToolWindowContent.getDisplayName(), false);
        content.setCloseable(false);
        Icon icon = null;

        try {
          icon = IconLoader.getIcon(infraToolWindowContent.icon, infraToolWindowContent.getClass().getClassLoader());
        }
        catch (Exception ignored) { }

        content.setPopupIcon(icon);
        contentManager.addContent(content);
      }
    }

  }

  private static final class ToolWindowContentStub extends JPanel {
    private final InfraToolWindowContent myInfraToolWindowContent;

    private ToolWindowContentStub(InfraToolWindowContent infraToolWindowContent) {
      this.myInfraToolWindowContent = infraToolWindowContent;
    }

    public InfraToolWindowContent getToolWindowContent() {
      return this.myInfraToolWindowContent;
    }
  }

  private static final class MyContentManagerListener implements ContentManagerListener {
    private final Project myProject;

    private MyContentManagerListener(Project project) {
      this.myProject = project;
    }

    public void selectionChanged(ContentManagerEvent event) {
      Content content = event.getContent();
      if (content.getComponent() instanceof ToolWindowContentStub stub) {
        InfraToolWindowContent infraToolWindowContent = stub.getToolWindowContent();
        InfraBaseView view = infraToolWindowContent.getInstance().createView(this.myProject);
        content.setComponent(view);
        content.setHelpId(view.getHelpId());
      }

    }
  }
}
