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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastLanguagePlugin;
import org.jetbrains.uast.UastLiteralUtils;

import java.util.List;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.WebMVCReference;

public class InfraMVCViewInspection extends LocalInspectionTool {

  public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
    if (!InfraLibraryUtil.hasLibrary(ModuleUtilCore.findModuleForFile(holder.getFile()))) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    PsiFile file = holder.getFile();
    UastLanguagePlugin uastPlugin = UastLanguagePlugin.Companion.byLanguage(file.getLanguage());
    if (uastPlugin == null) {
      return super.buildVisitor(holder, isOnTheFly);
    }
    return new PsiElementVisitor() {

      public void visitElement(PsiElement element) {
        PsiLanguageInjectionHost expression;
        super.visitElement(element);
        UExpression uElement = (UExpression) uastPlugin.convertElementWithParent(element, UExpression.class);
        if (uElement == null || (expression = UastLiteralUtils.getSourceInjectionHost(uElement)) != element || !InfraMVCViewUastReferenceProvider.VIEW_PATTERN.accepts(uElement)) {
          return;
        }
        FileReferenceSet activeSet = null;
        List<PsiReference> problems = new SmartList<>();
        PsiReference[] references = expression.getReferences();
        for (PsiReference psiReference : references) {
          if (psiReference.resolve() != null) {
            if ((psiReference instanceof ViewReference) || (psiReference instanceof WebMVCReference)) {
              return;
            }
            if (!(psiReference instanceof FileReference fileRef)) {
            }
            else if (fileRef.isLast()) {
              return;
            }
            else {
              activeSet = fileRef.getFileReferenceSet();
            }
          }
          else {
            if (activeSet == null && (psiReference instanceof FileReference fileRef) && (references.length == 1 || !fileRef.isLast())) {
              activeSet = fileRef.getFileReferenceSet();
            }
            problems.add(psiReference);
          }
        }
        for (PsiReference reference : problems) {
          if (!reference.isSoft()
                  && (!(reference instanceof ViewMultiResolverReference resolverReference) || resolverReference.hasResolvers())) {
            if (reference instanceof FileReference) {
              if (activeSet != null && activeSet == ((FileReference) reference).getFileReferenceSet()) {
                holder.registerProblem(reference);
              }
            }
            else if (activeSet == null) {
              holder.registerProblem(reference);
            }
          }
        }
      }
    };
  }
}
