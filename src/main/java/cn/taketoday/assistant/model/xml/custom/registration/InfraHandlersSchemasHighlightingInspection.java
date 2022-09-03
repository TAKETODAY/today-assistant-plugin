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

package cn.taketoday.assistant.model.xml.custom.registration;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;

public class InfraHandlersSchemasHighlightingInspection extends LocalInspectionTool {

  public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    if (!(file instanceof PropertiesFileImpl)) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    String fileName = file.getName();
    boolean handlersFile = "spring.handlers".equals(fileName);
    boolean schemasFile = "spring.schemas".equals(fileName);
    if (!handlersFile && !schemasFile) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    return new PsiElementVisitor() {

      public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof PropertyValueImpl) {
          for (PsiReference reference : element.getReferences()) {
            if (handlersFile && (reference instanceof JavaClassReference) && reference.resolve() == null) {
              holder.registerProblem(reference);
            }
            else if (schemasFile && (reference instanceof FileReference fileReference) && fileReference.multiResolve(false).length == 0) {
              holder.registerProblem(reference);
            }
          }
        }
      }
    };
  }
}
