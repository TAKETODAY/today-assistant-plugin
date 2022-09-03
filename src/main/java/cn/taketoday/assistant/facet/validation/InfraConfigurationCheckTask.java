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

package cn.taketoday.assistant.facet.validation;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.facet.FacetManager;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.event.HyperlinkEvent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.facet.InfraFrameworkDetector;
import cn.taketoday.assistant.model.jam.testContexts.jdbc.TestingSql;
import cn.taketoday.assistant.service.InfraModelProvider;
import cn.taketoday.lang.Nullable;

public class InfraConfigurationCheckTask extends Task.Backgroundable {
  private static final String NOTIFICATION_ID = "Infra Configuration Check";
  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_ID);
  private volatile List<Pair<Module, Collection<VirtualFilePointer>>> myUnmappedConfigurations;
  private volatile SmartPsiElementPointer<?>[] myProgrammaticConfigurations;

  public InfraConfigurationCheckTask(Project project) {
    super(project, InfraBundle.message("configuration.check"));
  }

  public void run(ProgressIndicator indicator) {
    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    if (modules.length == 0) {
      return;
    }
    InfraUnmappedConfigurationFilesCollector unmappedCollector = new InfraUnmappedConfigurationFilesCollector(modules);
    if (unmappedCollector.isEnabledInProject()) {
      List<Pair<Module, Collection<VirtualFilePointer>>> result = new ArrayList<>();
      indicator.setIndeterminate(false);
      int i = 0;
      for (Module module : modules) {
        indicator.setText2(InfraBundle.message("checking.infra.configuration", module));
        runCollector(progressIndicator -> {
          runUnmappedCollector(progressIndicator, module, result);
        }, indicator);
        int i2 = i;
        i++;
        indicator.setFraction(i2 / modules.length);
      }
      this.myUnmappedConfigurations = new ArrayList<>(result);
    }
    if (ApplicationManager.getApplication().isInternal()) {
      runCollector(this::runProgrammaticCollector, indicator);
    }
    snapshot.logResponsivenessSinceCreation("Infra Config Check [" + modules.length + " modules]");
  }

  private void runCollector(Consumer<ProgressIndicator> runner, ProgressIndicator indicator) {
    ReadAction.nonBlocking(() -> {
              runner.accept(ProgressIndicatorProvider.getGlobalProgressIndicator());
            }).
            inSmartMode(this.myProject)
            .wrapProgress(indicator)
            .executeSynchronously();
  }

  private void runUnmappedCollector(ProgressIndicator indicator, Module module, List<Pair<Module, Collection<VirtualFilePointer>>> result) {
    InfraUnmappedConfigurationFilesCollector unmappedCollector = new InfraUnmappedConfigurationFilesCollector(module);
    unmappedCollector.collect(indicator);
    result.add(createPair(module, unmappedCollector.getResults().get(module)));
  }

  private void runProgrammaticCollector(ProgressIndicator indicator) {
    ProgrammaticConfigurationCollector programmaticCollector = new ProgrammaticConfigurationCollector(getProject());
    if (programmaticCollector.isEnabledInProject()) {
      programmaticCollector.collect(indicator);
      this.myProgrammaticConfigurations = (SmartPsiElementPointer[]) ContainerUtil.map2Array(programmaticCollector.getResults(), SmartPsiElementPointer.class, SmartPointerManager::createPointer);
    }
  }

  public void onSuccess() {
    String message;
    if (getProject().isDisposed()) {
      return;
    }
    List<Pair<SmartPsiElementPointer<?>, PsiElement>> programmaticResults = this.myProgrammaticConfigurations == null ? Collections.emptyList() : ContainerUtil.mapNotNull(
            this.myProgrammaticConfigurations, pointer -> {
              PsiElement element = pointer.getElement();
              if (element == null) {
                return null;
              }
              return Pair.create(pointer, element);
            });
    MultiMap<Module, VirtualFilePointer> unmappedResults = this.myUnmappedConfigurations == null ? MultiMap.empty() : filterEmptyConfigurations(getProject(), this.myUnmappedConfigurations);
    if (unmappedResults.isEmpty() && programmaticResults.isEmpty()) {
      return;
    }
    StringBuilder notification = new StringBuilder();
    if (!unmappedResults.isEmpty()) {
      StringBuilder unmappedFilesText = new StringBuilder();
      boolean atLeastOneModuleWithoutFacet = false;
      for (Map.Entry<Module, Collection<VirtualFilePointer>> entry : unmappedResults.entrySet()) {
        Module module = entry.getKey();
        String moduleName = module.getName();
        Collection<VirtualFilePointer> pointers = entry.getValue();
        int filesCount = pointers.size();
        unmappedFilesText.append("<a href=\"config#").append(moduleName).append("\">").append(moduleName).append("</a>");
        unmappedFilesText.append(" (<a href=\"files#").append(moduleName).append("\">").append(InfraBundle.message("unmapped.configuration.files.count", filesCount)).append("</a>)");
        InfraFacet infraFacet = InfraFacet.from(module);
        if (infraFacet == null || infraFacet.getFileSets().isEmpty()) {
          unmappedFilesText.append("&nbsp;&nbsp;&nbsp;<a href=\"createDefault#").append(moduleName).append("\">")
                  .append(InfraBundle.message("unmapped.configuration.create.default.context")).append("</a>");
          atLeastOneModuleWithoutFacet = true;
        }
        unmappedFilesText.append("<br/>");
      }
      if (atLeastOneModuleWithoutFacet) {
        message = InfraBundle.message("unmapped.configuration.fix.instruction", InfraBundle.message("unmapped.configuration.create.default.context"));
      }
      else {
        message = InfraBundle.message("unmapped.configuration.configure.facet");
      }
      String fixInstruction = message;
      notification.append(InfraBundle.message("unmapped.configuration.files.found")).append("<br/><br/>").append(fixInstruction).append("<br/><br/>")
              .append(unmappedFilesText);
    }
    if (!programmaticResults.isEmpty()) {
      notification.append(notification.length() > 0 ? "<br/>" : "").append(InfraBundle.message("unmapped.configuration.programmatic.contexts.found")).append("<br/>");
      for (int i = 0; i < programmaticResults.size(); i++) {
        PsiElement psiElement = programmaticResults.get(i).second;
        PsiFile psiFile = psiElement.getContainingFile();
        notification.append("<a href=\"psi#").append(i).append("\">").append(psiFile.getName()).append("</a><br/>");
      }
    }
    String notificationText = notification.toString();
    NOTIFICATION_GROUP.createNotification(InfraBundle.message("configuration.check"), notificationText, NotificationType.WARNING)
            .setSuggestionType(true)
            .setListener(new UnmappedConfigurationsNotificationAdapter(
                    getProject(), unmappedResults, ContainerUtil.map(programmaticResults, pair -> pair.first)))
            .addAction(new NotificationAction(InfraBundle.message("infra.facet.validation.help.action")) {
              @Override
              public void actionPerformed(AnActionEvent e, Notification notification2) {
                HelpManager.getInstance().invokeHelp("infra.managing.file.sets");
              }
            }).addAction(new NotificationAction(InfraBundle.message("infra.facet.validation.disable.action")) {

              public void actionPerformed(AnActionEvent e, Notification notification2) {
                int result = Messages.showYesNoDialog(InfraConfigurationCheckTask.this.getProject(), InfraBundle.message("infra.facet.detection.will.be.disabled.for.whole.project"),
                        InfraBundle.message("infra.facet.config.detection"), InfraBundle.message("infra.facet.detection.disable.detection"),
                        CommonBundle.getCancelButtonText(), Messages.getWarningIcon());
                if (result == 0) {
                  DetectionExcludesConfiguration detectionExcludesConfiguration = DetectionExcludesConfiguration.getInstance(InfraConfigurationCheckTask.this.getProject());
                  detectionExcludesConfiguration.addExcludedFramework(InfraFrameworkDetector.getSpringFrameworkType());
                  notification2.hideBalloon();
                }
              }
            }).setIcon(Icons.Today).notify(getProject());
  }

  private static Pair<Module, Collection<VirtualFilePointer>> createPair(Module module, Collection<PsiFile> files) {
    List<VirtualFilePointer> pointers = ContainerUtil.map(files, file -> {
      return VirtualFilePointerManager.getInstance().create(file.getVirtualFile(), InfraManager.from(file.getProject()), null);
    });
    return Pair.create(module, pointers);
  }

  private static MultiMap<Module, VirtualFilePointer> filterEmptyConfigurations(
          Project project, List<Pair<Module, Collection<VirtualFilePointer>>> unmappedConfigurations) {
    var virtualFileMapper = InfraUnmappedConfigurationFilesCollector.getVirtualFileMapper(project);
    var result = new MultiMap<Module, VirtualFilePointer>();
    for (Pair<Module, Collection<VirtualFilePointer>> unmappedConfiguration : unmappedConfigurations) {
      if (!unmappedConfiguration.second.isEmpty()) {
        var pointers = ContainerUtil.filter(unmappedConfiguration.second, pointer -> virtualFileMapper.fun(pointer) != null);
        if (!pointers.isEmpty()) {
          result.put(unmappedConfiguration.first, pointers);
        }
      }
    }

    return result;
  }

  private static final class UnmappedConfigurationsNotificationAdapter extends NotificationListener.Adapter {
    private final Project myProject;
    private final MultiMap<Module, VirtualFilePointer> myUnmappedConfigurations;
    private final List<SmartPsiElementPointer<?>> myProgrammaticConfigurations;

    private UnmappedConfigurationsNotificationAdapter(Project project, MultiMap<Module, VirtualFilePointer> unmappedConfigurations, List<SmartPsiElementPointer<?>> programmaticConfigurations) {
      this.myProject = project;
      this.myUnmappedConfigurations = unmappedConfigurations;
      this.myProgrammaticConfigurations = programmaticConfigurations;
    }

    protected void hyperlinkActivated(Notification notification, HyperlinkEvent e) {
      if (this.myProject.isDisposed()) {
        return;
      }
      String description = e.getDescription();
      String navigationTarget = StringUtil.substringAfter(description, "#");
      assert navigationTarget != null;
      if (description.startsWith(TestingSql.CONFIG_ATTR_NAME)) {
        Module module = findModuleByName(navigationTarget);
        if (module == null) {
          return;
        }
        InfraFacet infraFacet = InfraFacet.from(module);
        if (infraFacet != null) {
          ModulesConfigurator.showFacetSettingsDialog(infraFacet, null);
        }
        else {
          ModulesConfigurator.showDialog(this.myProject, navigationTarget, null);
        }
        updateNotification(notification);
      }
      else if (description.startsWith("createDefault")) {
        Module module2 = findModuleByName(navigationTarget);
        if (module2 == null) {
          return;
        }
        DumbService dumbService = DumbService.getInstance(this.myProject);
        if (dumbService.isDumb()) {
          dumbService.showDumbModeNotification(
                  InfraBundle.message("unmapped.configuration.is.not.available.during.index.update", InfraBundle.message("unmapped.configuration.create.default.context")));
          return;
        }
        InfraFacet infraFacet2 = InfraFacet.from(module2);
        if (infraFacet2 == null) {
          infraFacet2 = FacetUtil.addFacet(module2, InfraFacet.getFacetType());
        }
        for (InfraModelProvider provider : InfraFileSetService.MODEL_PROVIDER_EP_NAME.getExtensions()) {
          if (!provider.getFilesets(infraFacet2).isEmpty()) {
            int result = Messages.showYesNoDialog(this.myProject,
                    InfraBundle.message("unmapped.configuration.autodetected.context.found.for", provider.getName()),
                    InfraBundle.message("unmapped.configuration.infra.configuration"), InfraBundle.message("unmapped.configuration.open.facet.configuration"),
                    CommonBundle.getCloseButtonText(), Messages.getInformationIcon());
            if (result == 0) {
              ModulesConfigurator.showFacetSettingsDialog(infraFacet2, null);
            }
            updateNotification(notification);
            return;
          }
        }
        Set<InfraFileSet> existingFileSets = Collections.emptySet();
        String id = InfraFileSetService.of().getUniqueId(existingFileSets);
        String name = InfraFileSetService.of().getUniqueName(InfraBundle.message("facet.context.default.name"), existingFileSets);
        InfraFileSet fileSet = infraFacet2.addFileSet(id, name);
        Collection<VirtualFilePointer> pointers = this.myUnmappedConfigurations.get(module2);
        NullableFunction<VirtualFilePointer, PsiFile> virtualFileMapper = InfraUnmappedConfigurationFilesCollector.getVirtualFileMapper(module2.getProject());
        for (VirtualFilePointer pointer : pointers) {
          PsiFile psiFile = virtualFileMapper.fun(pointer);
          if (psiFile != null) {
            fileSet.addFile(psiFile.getVirtualFile());
          }
        }
        FacetManager.getInstance(module2).facetConfigurationChanged(infraFacet2);
        updateNotification(notification);
      }
      else if (description.startsWith("files")) {
        Module module3 = findModuleByName(navigationTarget);
        if (module3 == null) {
          return;
        }
        NullableFunction<VirtualFilePointer, PsiFile> virtualFileMapper2 = InfraUnmappedConfigurationFilesCollector.getVirtualFileMapper(module3.getProject());
        Collection<PsiFile> files = ContainerUtil.mapNotNull(this.myUnmappedConfigurations.get(module3), virtualFileMapper2);
        if (files.isEmpty()) {
          JBPopupFactory.getInstance().createMessage(InfraBundle.message("config.files.not.found")).showInFocusCenter();
          return;
        }
        JBPopup popup = NavigationUtil.getPsiElementPopup(files.toArray(PsiFile.EMPTY_ARRAY), InfraBundle.message("config.unmapped.configs.popup.title", navigationTarget));
        Object event = e.getSource();
        if (event instanceof Component) {
          popup.showInCenterOf((Component) event);
        }
        else {
          popup.showInFocusCenter();
        }
      }
      else if (description.startsWith("psi")) {
        PsiElement element = this.myProgrammaticConfigurations.get(Integer.parseInt(navigationTarget)).getElement();
        if (element != null) {
          NavigationUtil.activateFileWithPsiElement(element);
        }
        else {
          JBPopupFactory.getInstance().createMessage(InfraBundle.message("config.file.not.found")).showInFocusCenter();
        }
      }
    }

    private void updateNotification(Notification notification) {
      notification.expire();
      ApplicationManager.getApplication().invokeLater(() -> {
        new InfraConfigurationCheckTask(this.myProject).queue();
      }, this.myProject.getDisposed());
    }

    @Nullable
    private Module findModuleByName(String navigationTarget) {
      return ModuleManager.getInstance(this.myProject).findModuleByName(navigationTarget);
    }

  }

}
