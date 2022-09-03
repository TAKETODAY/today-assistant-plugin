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

package cn.taketoday.assistant.model.actions.patterns;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import javax.swing.Icon;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.lang.Nullable;

public abstract class AbstractInfraConfigActionGroup extends DefaultActionGroup {
  protected abstract String getDescription();

  public AbstractInfraConfigActionGroup() {
    setPopup(true);
  }

  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (project == null || editor == null) {
      presentation.setVisible(false);
      return;
    }
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    boolean enabled = (psiFile instanceof XmlFile xmlFile) && InfraDomUtils.isInfraXml(xmlFile) && isInsideRootTag(editor, xmlFile);
    presentation.setEnabledAndVisible(enabled);
    if (enabled) {
      event.getPresentation().setText(getDescription());
      event.getPresentation().setIcon(getIcon());
    }
  }

  private static boolean isInsideRootTag(Editor editor, XmlFile xmlFile) {
    XmlTag tag;
    XmlDocument document = xmlFile.getDocument();
    if (document != null && (tag = document.getRootTag()) != null) {
      TextRange textRange = tag.getTextRange();
      return textRange.contains(editor.getCaretModel().getOffset());
    }
    return false;
  }

  @Nullable
  protected Icon getIcon() {
    return null;
  }
}
