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

package cn.taketoday.assistant.refactoring;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameInputValidatorEx;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraBeanRenameInputValidator implements RenameInputValidatorEx {

  @Nullable
  public String getErrorMessage(String newName, Project project) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      for (InfraModel infraModel : InfraManager.from(project).getAllModelsWithoutDependencies(module)) {
        BeanPointer<?> foundValue = InfraModelSearchers.findBean(infraModel, newName);
        if (foundValue != null) {
          return message("bean.already.exists", newName, foundValue.getContainingFile().getName());
        }
      }
    }
    return null;
  }

  public ElementPattern<? extends PsiElement> getPattern() {
    return PlatformPatterns.pomElement(StandardPatterns.instanceOf(BeanPsiTarget.class));
  }

  public boolean isInputValid(String newName, PsiElement element, ProcessingContext context) {
    return true;
  }
}
