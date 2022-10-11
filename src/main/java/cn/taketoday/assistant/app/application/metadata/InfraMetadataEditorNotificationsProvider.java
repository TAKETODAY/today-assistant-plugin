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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.json.JsonFileType;
import com.intellij.json.psi.JsonFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.swing.JComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.assistant.app.application.metadata.additional.InfraAdditionalConfigUtils;
import cn.taketoday.assistant.app.options.InfrastructureSettings;
import cn.taketoday.assistant.editor.InfraEditorNotificationPanel;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraAppBundle.message;

final class InfraMetadataEditorNotificationsProvider implements EditorNotificationProvider, Disposable {

  private final MergingUpdateQueue updateQueue;
  private final ParameterizedCachedValue<Object, PsiFile> psiClassOwnerCachedTrigger;

  InfraMetadataEditorNotificationsProvider(Project project) {
    Disposer.register(InfrastructureSettings.getInstance(project), this);
    this.psiClassOwnerCachedTrigger = CachedValuesManager.getManager(project).createParameterizedCachedValue((psiFile) -> {
      if (!DumbService.isDumb(project) && !isRelevantProject(project)) {
        return CachedValueProvider.Result.create(null, ProjectRootManager.getInstance(project));
      }
      else {
        updateNotifications(psiFile.getVirtualFile(), project);
        return CachedValueProvider.Result.create(null, InfraModificationTrackersManager.from(project).getOuterModelsModificationTracker());
      }
    }, false);
    this.updateQueue = new MergingUpdateQueue("InfraMetadataEditorNotificationsProvider",
            2000, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      this.installListeners(project);
    }
  }

  private static boolean isRelevantProject(Project project) {
    return InfraUtils.hasFacets(project) && InfraLibraryUtil.hasFrameworkLibrary(project);
  }

  public Function<? super FileEditor, ? extends JComponent> collectNotificationData(Project project, VirtualFile file) {
    if (!InfrastructureSettings.getInstance(project).isShowAdditionalConfigNotification()) {
      return CONST_NULL;
    }
    else if (ApplicationManager.getApplication().isUnitTestMode()) {
      return CONST_NULL;
    }
    else if (!isRelevantProject(project)) {
      return CONST_NULL;
    }
    else {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile == null || !InfraAdditionalConfigUtils.isAdditionalMetadataFile(psiFile) && !isConfigurationPropertiesAnnotatedClass(psiFile)) {
        return CONST_NULL;
      }
      else {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) {
          return CONST_NULL;
        }
        else if (hasAnnotationProcessor(module)) {
          return (fileEditor) -> withAnnotationProcessor(project, fileEditor);
        }
        else {
          String bootVersion = detectInfraVersion(psiFile);
          return (fileEditor) -> {
            return configureAnnotationProcessor(project, module, fileEditor, bootVersion);
          };
        }
      }
    }
  }

  @RequiresEdt
  private static InfraEditorNotificationPanel withAnnotationProcessor(Project project, FileEditor fileEditor) {

    ApplicationManager.getApplication().assertIsDispatchThread();
    InfraEditorNotificationPanel panel = new InfraEditorNotificationPanel(fileEditor, JBColor.lightGray);
    panel.icon(Icons.Today);
    panel.text(message("configuration.properties.rerun.notification"));
    panel.createActionLabel(message("configuration.properties.hide.notification.action"), () -> {
      InfrastructureSettings.getInstance(project).setShowAdditionalConfigNotification(false);
      EditorNotifications.getInstance(project).updateAllNotifications();
    });
    panel.installOpenSettingsButton(project, message("infra.name"));
    return panel;
  }

  @RequiresEdt
  private static InfraEditorNotificationPanel configureAnnotationProcessor(
          Project project, Module module, FileEditor fileEditor, @Nullable String bootVersion) {

    ApplicationManager.getApplication().assertIsDispatchThread();
    InfraEditorNotificationPanel panel = new InfraEditorNotificationPanel(fileEditor, LightColors.RED);

    panel.icon(Icons.Today);
    panel.text(message("configuration.properties.not.configured.notification"));
    panel.createActionLabel(message("configuration.properties.open.documentation.action"), () -> {

      String page = "configuration-metadata.html";
      BrowserUtil.browse("https://docs.spring.io/spring-boot/docs/" + (bootVersion != null ? bootVersion : "current") + "/reference/html/" + page + "#configuration-metadata-annotation-processor");
    });
    panel.installOpenSettingsButton(project, message("infra.name"));

    return panel;
  }

  @Nullable
  private static String detectInfraVersion(PsiFile psiFile) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    return module != null ? InfraLibraryUtil.getVersionFromJar(module) : null;
  }

  private static void updateNotifications(VirtualFile file, Project project) {
    EditorNotifications.getInstance(project).updateNotifications(file);
  }

  private static boolean isConfigurationPropertiesAnnotatedClass(PsiFile psiFile) {
    if (!(psiFile instanceof PsiClassOwner psiClassOwner)) {
      return false;
    }
    else {
      PsiClass[] classes = psiClassOwner.getClasses();
      PsiClass psiClass = ArrayUtil.getFirstElement(classes);
      return psiClass != null && AnnotationUtil.isAnnotated(psiClass, InfraClassesConstants.CONFIGURATION_PROPERTIES, 0);
    }
  }

  private static boolean hasAnnotationProcessor(Module module) {
    Set<Module> allModules = new HashSet<>();
    ModuleUtilCore.getDependencies(module, allModules);
    return allModules.stream().anyMatch(InfraLibraryUtil::hasConfigurationMetadataAnnotationProcessor);
  }

  private void installListeners(Project project) {

    InfrastructureSettings settings = InfrastructureSettings.getInstance(project);
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      public void propertyChanged(VirtualFilePropertyEvent event) {

        VirtualFile virtualFile = event.getFile();
        if ("name".equals(event.getPropertyName()) && FileTypeRegistry.getInstance().isFileOfType(virtualFile, JsonFileType.INSTANCE)) {
          PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
          if (file instanceof JsonFile) {
            updateNotifications(virtualFile, project);
          }
        }

      }
    }, settings);
    PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      public void childMoved(PsiTreeChangeEvent event) {
        this.handleChange(event);
      }

      public void childReplaced(PsiTreeChangeEvent event) {
        this.handleChange(event);
      }

      public void childRemoved(PsiTreeChangeEvent event) {
        this.handleChange(event);
      }

      public void childAdded(PsiTreeChangeEvent event) {
        this.handleChange(event);
      }

      public void childrenChanged(PsiTreeChangeEvent event) {
        this.handleChange(event);
      }

      private void handleChange(PsiTreeChangeEvent event) {
        PsiFile file = event.getFile();
        if (file instanceof PsiClassOwner || file instanceof JsonFile) {
          if (DumbService.isDumb(project)) {
            return;
          }

          updateQueue.queue(new Update("SB metadata notification update") {
            public void run() {
              ReadAction.run(() -> psiClassOwnerCachedTrigger.getValue(file));
            }
          });
        }

      }
    }, settings);
  }

  public void dispose() {
    this.updateQueue.cancelAllUpdates();
  }
}
