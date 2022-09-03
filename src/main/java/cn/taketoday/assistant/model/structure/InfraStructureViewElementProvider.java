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

package cn.taketoday.assistant.model.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.xml.XmlStructureViewElementProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class InfraStructureViewElementProvider implements XmlStructureViewElementProvider {

  @Nullable
  public StructureViewTreeElement createCustomXmlTagTreeElement(XmlTag tag) {
    PsiFile containingFile = tag.getContainingFile();
    Project project = tag.getProject();

    if ((containingFile instanceof XmlFile xmlFile) && InfraDomUtils.isInfraXml(xmlFile)) {
      DomElement domElement = DomManager.getDomManager(project).getDomElement(tag);
      if (domElement instanceof Beans) {
        return new InfraModelTreeElement(xmlFile, false);
      }
    }
    return null;
  }
}
