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

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;

import java.util.function.Supplier;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.model.actions.generate.GenerateBeanDependenciesUtil;
import cn.taketoday.lang.Nullable;

public abstract class GenerateBeanDependencyAction extends BaseGenerateAction {

  public GenerateBeanDependencyAction(CodeInsightActionHandler handler, Supplier<String> text) {
    super(handler);
    getTemplatePresentation().setText(text);
    getTemplatePresentation().setIcon(Icons.Today);
  }

  protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
    PsiClass psiClass;
    Module module;
    return !(file instanceof PsiCompiledElement) && (psiClass = getTargetClass(editor,
            file)) != null && !psiClass.isInterface() && !psiClass.isEnum() && psiClass.getQualifiedName() != null && (module = GenerateBeanDependenciesUtil.getSpringModule(
            psiClass)) != null && checkContext(module) && acceptPsiClass(psiClass);
  }

  @Nullable
  protected PsiClass getTargetClass(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    UClass uClass = UastUtils.findContaining(element, UClass.class);
    if (uClass != null) {
      return uClass.getJavaPsi();
    }
    return null;
  }

  protected boolean checkContext(Module module) {
    if (isXmlBasedAction()) {
      InfraFacet facet = InfraFacet.from(module);
      if (facet != null) {
        for (InfraFileSet set : InfraFileSetService.of().getAllSets(facet)) {
          for (VirtualFilePointer pointer : set.getFiles()) {
            VirtualFile virtualFile = pointer.getFile();
            if (virtualFile != null && !ProjectRootManager.getInstance(module.getProject()).getFileIndex().isInLibraryClasses(virtualFile)) {
              PsiFile findFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
              if (JamCommonUtil.isPlainXmlFile(findFile)
                      && InfraDomUtils.isInfraXml((XmlFile) findFile)) {
                return true;
              }
            }
          }
        }
        return false;
      }
      return false;
    }
    return true;
  }

  protected boolean isXmlBasedAction() {
    return true;
  }

  protected boolean acceptPsiClass(PsiClass psiClass) {
    return true;
  }

  private boolean isSetterDependency() {
    return ((GenerateBeanDependenciesActionHandler) getHandler()).isSetterDependency();
  }
}
