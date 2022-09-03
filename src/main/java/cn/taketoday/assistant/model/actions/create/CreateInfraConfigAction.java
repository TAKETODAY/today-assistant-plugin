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

package cn.taketoday.assistant.model.actions.create;

import com.intellij.ide.actions.CreateFileAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.messagePointer;

public class CreateInfraConfigAction extends CreateFileAction {

  public CreateInfraConfigAction() {
    super(messagePointer("config.new.file"), messagePointer("create.new.configuration.file"), Icons.SpringConfig);
  }

  protected boolean isAvailable(DataContext dataContext) {
    Module module;
    return super.isAvailable(dataContext)
            && (module = PlatformCoreDataKeys.MODULE.getData(dataContext)) != null
            && InfraLibraryUtil.hasLibrary(module.getProject());
  }

  protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception {
    String fileName = getFileName(newName);
    PsiElement psiElement = InfraUtils.createXmlConfigFile(fileName, directory);
    return new PsiElement[] { psiElement };
  }

  protected String getDefaultExtension() {
    return "xml";
  }
}
