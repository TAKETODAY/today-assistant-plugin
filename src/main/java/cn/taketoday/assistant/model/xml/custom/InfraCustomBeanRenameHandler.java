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

package cn.taketoday.assistant.model.xml.custom;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.RenameHandler;

import cn.taketoday.assistant.model.xml.CustomBeanPsiElement;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraCustomBeanRenameHandler implements RenameHandler {

  public boolean isAvailableOnDataContext(DataContext dataContext) {
    return false;
  }

  public boolean isRenaming(DataContext dataContext) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    return element instanceof CustomBeanPsiElement;
  }

  public void invoke(Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    doInvoke(project, editor, element);
  }

  private static void doInvoke(Project project, Editor editor, PsiElement element) {
    XmlAttribute idAttribute = ((CustomBeanPsiElement) element).getBean().getIdAttribute();
    if (idAttribute == null) {
      int i = Messages.showOkCancelDialog(project, message("custom.bean.no.id"), message("custom.bean.no.id.title"), Messages.getWarningIcon());
      if (i != 0) {
        return;
      }
    }
    PsiElementRenameHandler.invoke(element, project, element, editor);
  }

  public void invoke(Project project, PsiElement[] elements, DataContext dataContext) {
  }
}
