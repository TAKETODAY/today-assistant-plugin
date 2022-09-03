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

package cn.taketoday.assistant.facet.editor;

import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;

import java.util.function.Supplier;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.dom.InfraDomUtils;

public enum AddFileType {

  PROPERTIES(InfraBundle.messagePointer("facet.context.properties.files"), PropertiesFileType.INSTANCE.getIcon()),
  Infra_XML(InfraBundle.messagePointer("facet.context.xml.files"), Icons.SpringConfig),
  CODE(InfraBundle.messagePointer("facet.context.code.files"), Icons.SpringJavaConfig),
  OTHER(InfraBundle.messagePointer("facet.context.other.files"), AllIcons.FileTypes.Any_type);

  private final Supplier<String> displayName;
  private final Icon icon;

  AddFileType(Supplier supplier, Icon icon) {
    this.displayName = supplier;
    this.icon = icon;
  }

  public String getDisplayName() {
    return this.displayName.get();
  }

  public Icon getIcon() {
    return this.icon;
  }

  public Condition<VirtualFile> getFileVisibleCondition(Project project) {
    if (this == PROPERTIES) {
      return file -> {
        return FileTypeRegistry.getInstance().isFileOfType(file, PropertiesFileType.INSTANCE);
      };
    }
    if (this == CODE) {
      PsiManager psiManager = PsiManager.getInstance(project);
      return file2 -> {
        PsiFile psiFile = psiManager.findFile(file2);
        return psiFile instanceof PsiClassOwner;
      };
    }
    else if (this == Infra_XML) {
      PsiManager psiManager2 = PsiManager.getInstance(project);
      return file3 -> {
        if (!FileTypeRegistry.getInstance().isFileOfType(file3, XmlFileType.INSTANCE)) {
          return false;
        }
        PsiFile findFile = psiManager2.findFile(file3);
        return (findFile instanceof XmlFile xmlFile) && InfraDomUtils.isInfraXml(xmlFile);
      };
    }
    else {
      return Conditions.alwaysTrue();
    }
  }
}
