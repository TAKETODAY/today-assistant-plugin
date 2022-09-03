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

package cn.taketoday.assistant.model.actions;

import com.google.gson.GsonBuilder;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.text.DateFormatUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.wrappers.InfraWFacet;
import cn.taketoday.assistant.model.wrappers.WFileset;
import cn.taketoday.assistant.model.wrappers.WInfraDependency;
import cn.taketoday.assistant.model.wrappers.WModel;
import cn.taketoday.assistant.model.wrappers.WModule;
import cn.taketoday.assistant.model.wrappers.WProject;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public class DumpInfraConfigurationAction extends AnAction {

  public void update(AnActionEvent e) {
    Project project = e.getProject();
    boolean hasSpring = project != null && InfraUtils.hasFacets(project);
    e.getPresentation().setEnabled(hasSpring);
  }

  public ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    ThrowableComputable<String, RuntimeException> computable = () -> (String) ReadAction.compute(() -> getProjectDumpJson(project));
    String dump = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            computable, message("model.actions.dump.model.gathering.infra.models"), true, project);

    String fileName = String.format("InfraConfiguration-%s.txt", DateFormatUtil.formatDateTime(System.currentTimeMillis()));
    LightVirtualFile file = new LightVirtualFile(fileName, JsonFileType.INSTANCE, dump);
    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
    FileEditorManager.getInstance(project).openEditor(descriptor, true);
  }

  public static String getProjectDumpJson(Project project) {
    ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
    pi.setIndeterminate(false);
    pi.setText(message("model.actions.dump.model.scanning.infra.models"));
    LocalModelFactory localModelFactory = LocalModelFactory.of();
    WProject dumpProject = new WProject();
    Module[] modules = ModuleManager.getInstance(project).getSortedModules();
    int processedModules = 0;
    for (Module module : modules) {
      InfraFacet infraFacet = InfraFacet.from(module);
      if (infraFacet != null) {
        WModule dumpModule = new WModule(module.getName());
        dumpProject.modules.add(dumpModule);
        InfraWFacet dumpFacet = new InfraWFacet();
        dumpModule.facets.add(dumpFacet);
        Set<InfraFileSet> fileSets = InfraFileSetService.of().getAllSets(infraFacet);
        for (InfraFileSet fileSet : fileSets) {
          pi.checkCanceled();
          WFileset dumpFileSet = new WFileset(fileSet);
          dumpFacet.filesets.add(dumpFileSet);
          List<VirtualFilePointer> files = fileSet.getFiles();
          Set<LocalModel> visited = new LinkedHashSet<>();
          for (VirtualFilePointer file : files) {
            VirtualFile virtualFile = file.getFile();
            if (virtualFile != null) {
              PsiElement findFile = PsiManager.getInstance(project).findFile(virtualFile);
              if (JamCommonUtil.isPlainXmlFile(findFile) && InfraDomUtils.isInfraXml((XmlFile) findFile)) {
                LocalModel model = localModelFactory.getOrCreateLocalXmlModel((XmlFile) findFile, module, Collections.emptySet());
                if (model != null) {
                  visitRelated(model, visited, dumpFileSet);
                }
              }
              else if (findFile instanceof PsiClassOwner) {
                PsiClass[] psiClasses = ((PsiClassOwner) findFile).getClasses();
                for (PsiClass psiClass : psiClasses) {
                  LocalModel model2 = localModelFactory.getOrCreateLocalAnnotationModel(psiClass, module, Collections.emptySet());
                  if (model2 != null) {
                    visitRelated(model2, visited, dumpFileSet);
                  }
                }
              }
            }
          }
        }
        processedModules++;
        pi.setFraction(processedModules / modules.length);
      }
    }
    pi.setText(message("model.actions.dump.model.generating.json"));
    return new GsonBuilder().setPrettyPrinting().create().toJson(dumpProject);
  }

  private static void visitRelated(LocalModel<?> model, Set<LocalModel> visited, WFileset dumpFileSet) {
    WModel dumpModel = new WModel(model);
    dumpFileSet.models.add(dumpModel);
    visited.add(model);
    Set<Pair<LocalModel, LocalModelDependency>> dependentModels = model.getDependentLocalModels();
    for (Pair<LocalModel, LocalModelDependency> dependentModel : dependentModels) {
      WInfraDependency dependency = new WInfraDependency(dependentModel);
      dumpModel.dependencies.add(dependency);
      if (!visited.contains(dependentModel.first)) {
        visitRelated(dependentModel.first, visited, dumpFileSet);
      }
    }
  }
}
