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

package cn.taketoday.assistant.code;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.spring.dom.SpringDomUtils;
import com.intellij.spring.model.utils.SpringCommonUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 21:30
 */
public class ProblemFileHighlightFilter implements Condition<VirtualFile> {
  private final Project project;

  public ProblemFileHighlightFilter(Project project) {
    this.project = project;
  }

  @Override
  public boolean value(VirtualFile virtualFile) {
    if (!FileTypeRegistry.getInstance().isFileOfType(virtualFile, XmlFileType.INSTANCE)) {
      return false;
    }
    else {
      Module module = ModuleUtilCore.findModuleForFile(virtualFile, this.project);
      if (module == null) {
        return false;
      }
      else {
        return SpringCommonUtils.hasSpringFacet(module) && ReadAction.compute(() -> {
          PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
          return psiFile instanceof XmlFile && SpringDomUtils.isSpringXml((XmlFile) psiFile);
        });
      }
    }
  }

}
