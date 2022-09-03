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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.text.DateFormatUtil;

import java.util.Arrays;

import cn.taketoday.assistant.util.InfraUtils;

public final class DumpInfraModulesSorterAction extends AnAction {

  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    boolean hasSpring = project != null && InfraUtils.hasFacets(project);
    Presentation presentation = e.getPresentation();
    presentation.setEnabled(hasSpring);
  }

  public ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null) {
      ModulesSorter modulesSorter = ModulesSorter.getInstance(project);
      Object[] objArr = { DateFormatUtil.formatDateTime(System.currentTimeMillis()) };
      String fileName = String.format("InfraModulesSorterDump-%s.txt", Arrays.copyOf(objArr, objArr.length));
      StringBuilder dump = new StringBuilder();
      modulesSorter.dump(dump);
      OpenFileDescriptor descriptor = new OpenFileDescriptor(project, new LightVirtualFile(fileName, dump));
      FileEditorManager.getInstance(project).openEditor(descriptor, true);
    }
  }
}
