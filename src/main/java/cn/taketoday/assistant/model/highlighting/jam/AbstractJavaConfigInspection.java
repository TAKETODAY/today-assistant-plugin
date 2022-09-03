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
package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;

public abstract class AbstractJavaConfigInspection extends AbstractBaseJavaLocalInspectionTool {

  @Override
  public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
    if (!JamCommonUtil.isPlainJavaFile(holder.getFile()))
      return PsiElementVisitor.EMPTY_VISITOR;

    Module module = ModuleUtilCore.findModuleForFile(holder.getFile());
    if (!InfraLibraryUtil.hasLibrary(module) || !InfraUtils.hasFacet(module)) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }

    return super.buildVisitor(holder, isOnTheFly);
  }

  @Override
  public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (InfraModelService.of().hasAutoConfiguredModels(module)) {
      ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);

      checkJavaFile((PsiJavaFile) file, holder, isOnTheFly, module);

      List<ProblemDescriptor> problemDescriptors = holder.getResults();
      return problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
    }
    return null;
  }

  protected void checkJavaFile(PsiJavaFile javaFile,
          ProblemsHolder holder,
          boolean isOnTheFly,
          Module module) {
    for (PsiClass psiClass : javaFile.getClasses()) {
      checkClassInternal(psiClass, holder, module);
    }
  }

  private void checkClassInternal(PsiClass aClass, ProblemsHolder holder, Module module) {
    checkClass(aClass, holder, module);
    for (PsiClass psiClass : aClass.getInnerClasses()) {
      checkClass(psiClass, holder, module);
    }
  }

  protected abstract void checkClass(PsiClass aClass, ProblemsHolder holder, Module module);
}
