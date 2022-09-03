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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;

import cn.taketoday.assistant.model.actions.generate.GenerateAutowiredDependenciesJvmKt;

public class GenerateAutowiredDependenciesActionHandler implements CodeInsightActionHandler {

  public void invoke(Project project, Editor editor, PsiFile file) {
    UClass uClass;
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    if (element != null && (uClass = UastUtils.findContaining(element, UClass.class)) != null) {
      GenerateAutowiredDependenciesJvmKt.generateAutowiredDependenciesFor(uClass);
    }
  }

  public boolean startInWriteAction() {
    return false;
  }
}
