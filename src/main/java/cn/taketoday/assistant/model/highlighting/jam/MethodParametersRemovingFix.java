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

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.util.IncorrectOperationException;

import static cn.taketoday.assistant.InfraBundle.message;

public final class MethodParametersRemovingFix extends LocalQuickFixOnPsiElement {
  private final String methodName;

  public MethodParametersRemovingFix(PsiMethod method) {
    super(method);
    this.methodName = method.getName();
  }

  public String getFamilyName() {
    return message("method.parameters.removing.fix.family.name");
  }

  public void invoke(Project project, PsiFile file, PsiElement startElement, PsiElement endElement) {
    PsiMethod finalMethod = (PsiMethod) startElement;
    if (!FileModificationService.getInstance().prepareFileForWrite(finalMethod.getContainingFile())) {
      return;
    }
    try {
      new ChangeSignatureProcessor(project, finalMethod, false, null, finalMethod.getName(), finalMethod.getReturnType(), new ParameterInfoImpl[0]).run();
    }
    catch (IncorrectOperationException e) {
      LocalQuickFixOnPsiElement.LOG.error(e);
    }
  }

  public String getText() {
    return message("method.parameters.removing.fix.text", this.methodName);
  }

  public boolean startInWriteAction() {
    return false;
  }
}
