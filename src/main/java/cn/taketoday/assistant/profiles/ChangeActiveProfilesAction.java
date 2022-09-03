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

package cn.taketoday.assistant.profiles;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastContextKt;

import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class ChangeActiveProfilesAction extends DumbAwareAction {
  public static final String ACTION_ID = "InfraChangeActiveProfiles";

  public void update(AnActionEvent e) {
    Project project = e.getProject();
    boolean enabled = project != null && InfraUtils.hasFacets(project) && !DumbService.getInstance(project).isDumb();
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  public ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  public void actionPerformed(AnActionEvent e) {
    InfraModel model;
    Project project = e.getProject();
    if (project != null && !DumbService.getInstance(project).isDumb()) {
      Module module = e.getData(PlatformCoreDataKeys.MODULE);
      InfraFileSet fileSet = null;
      boolean includeTests = false;
      if (module != null) {
        Object data = e.getData(PlatformCoreDataKeys.SELECTED_ITEM);
        if (data instanceof InfraFileSet) {
          fileSet = (InfraFileSet) data;
        }
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
          if (fileSet == null && (model = getInfraModel(psiFile)) != null) {
            fileSet = model.getFileSet();
          }
          VirtualFile virtualFile = psiFile.isValid() ? psiFile.getVirtualFile() : null;
          if (virtualFile != null) {
            includeTests = ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(virtualFile);
          }
        }
      }
      cn.taketoday.assistant.profiles.ChangeActiveProfileDialog dialog = new cn.taketoday.assistant.profiles.ChangeActiveProfileDialog(project, module, fileSet, includeTests);
      if (dialog.showAndGet()) {
        ProfileUtils.notifyProfilesChanged(project);
      }
    }
  }

  @Nullable
  public static InfraModel getInfraModel(PsiFile psiFile) {
    Module module;
    if (JamCommonUtil.isPlainXmlFile(psiFile) && InfraDomUtils.isInfraXml((XmlFile) psiFile)) {
      InfraManager infraManager = InfraManager.from(psiFile.getProject());
      return infraManager.getInfraModelByFile(psiFile);
    }
    UFile uFile = UastContextKt.toUElement(psiFile, UFile.class);
    if (uFile == null || (module = ModuleUtilCore.findModuleForPsiElement(psiFile)) == null || !hasConfigurations(uFile)) {
      return null;
    }
    InfraManager infraManager2 = InfraManager.from(psiFile.getProject());
    Set<InfraModel> models = infraManager2.getAllModels(module);
    for (InfraModel model : models) {
      if (InfraModelVisitorUtils.hasConfigFile(model, psiFile)) {
        return model;
      }
    }
    return null;
  }

  private static boolean hasConfigurations(UFile uFile) {
    for (UClass uClass : uFile.getClasses()) {
      PsiClass psiClass = UElementKt.getAsJavaPsiElement(uClass, PsiClass.class);
      if (psiClass != null && containsConfigurations(psiClass)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsConfigurations(PsiClass psiClass) {
    if (InfraUtils.isConfigurationOrMeta(psiClass)) {
      return true;
    }
    for (PsiClass innerClass : psiClass.getInnerClasses()) {
      if (innerClass.hasModifierProperty("static") && !psiClass.hasModifierProperty("private") && containsConfigurations(innerClass)) {
        return true;
      }
    }
    return false;
  }
}
