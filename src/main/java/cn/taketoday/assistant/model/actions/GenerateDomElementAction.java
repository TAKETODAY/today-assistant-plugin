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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.ui.actions.generate.GenerateDomElementProvider;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.dom.InfraDomUtils;

@SuppressWarnings("ComponentNotRegistered")
public class GenerateDomElementAction extends com.intellij.util.xml.ui.actions.generate.GenerateDomElementAction {

  public GenerateDomElementAction(final GenerateDomElementProvider provider) {
    super(provider);
  }

  public GenerateDomElementAction(final GenerateDomElementProvider provider, Icon icon) {
    super(provider);
    getTemplatePresentation().setIcon(icon);
  }

  @Override
  public boolean isValidForFile(Project project, Editor editor, PsiFile file) {
    return super.isValidForFile(project, editor, file) &&
            file instanceof XmlFile &&
            InfraDomUtils.isInfraXml((XmlFile) file) &&
            InfraManager.from(project).getInfraModelByFile(file) != null;
  }
}