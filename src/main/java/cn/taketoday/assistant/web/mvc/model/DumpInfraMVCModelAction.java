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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.text.DateFormatUtil;

import java.util.Collection;

import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.services.WebMvcService;

import static cn.taketoday.assistant.InfraAppBundle.message;

public class DumpInfraMVCModelAction extends AnAction {

  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    boolean hasSpring = project != null && InfraUtils.hasFacets(project);
    e.getPresentation().setEnabled(hasSpring);
  }

  public ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    ThrowableComputable<String, RuntimeException> computable = () -> {
      return (String) ReadAction.compute(() -> getMvcModelDump(project));
    };
    String dump = ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(computable, message("DumpInfraMVCModelAction.gathering.web.mvc.models"), true, project);
    String fileName = String.format("InfraMVCModel-%s.txt", DateFormatUtil.formatDateTime(System.currentTimeMillis()));
    LightVirtualFile file = new LightVirtualFile(fileName, dump);
    OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
    FileEditorManager.getInstance(project).openEditor(descriptor, true);
  }

  public static String getMvcModelDump(Project project) {
    ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
    pi.setIndeterminate(false);
    pi.setText(message("DumpInfraMVCModelAction.scanning.web.mvc.models"));
    Module[] modules = ModuleManager.getInstance(project).getSortedModules();
    StringBuilder dump = new StringBuilder();
    int processedModules = 0;
    for (Module module : modules) {
      pi.checkCanceled();
      String name = module.getName();
      dump.append(name).append(" ").append(StringUtil.repeatSymbol('=', Math.max(1, 119 - name.length()))).append("\n");
      InfraFacet springFacet = InfraFacet.from(module);
      if (springFacet == null) {
        dump.append("no Today facet");
      }
      else {
        Collection<WebFacet> webFacets = WebFacet.getInstances(module);
        if (webFacets.isEmpty()) {
          dump.append("no Web facets");
        }
        else {
          dumpModule(dump, springFacet, webFacets);
        }
      }
      dump.append("\n\n");
      processedModules++;
      pi.setFraction(processedModules / modules.length);
    }
    return dump.toString();
  }

  private static void dumpModule(StringBuilder dump, InfraFacet springFacet, Collection<WebFacet> webFacets) {
    dump.append("Servlet models:\n");
    Collection<InfraModel> servletModels = WebMvcService.getServletModels(springFacet.getModule());
    for (InfraModel servletModel : servletModels) {
      InfraFileSet fileSet = servletModel.getFileSet();
      dump.append(" ").append(fileSet.getId());
    }
  }
}
