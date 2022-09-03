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
package cn.taketoday.assistant.model.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.InfraModelElement;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public abstract class InfraModelService {

  public static InfraModelService of() {
    return ApplicationManager.getApplication().getService(InfraModelService.class);
  }

  public abstract CommonInfraModel getModel(InfraModelElement modelElement);

  public abstract CommonInfraModel getModel(@Nullable PsiElement element);

  public abstract CommonInfraModel getPsiClassModel(PsiClass psiClass);

  public abstract CommonInfraModel getModuleCombinedModel(PsiElement element);

  public abstract CommonInfraModel getModelByBean(@Nullable CommonInfraBean springBean);

  /**
   * Determines if given configuration file is used in test context
   *
   * @param module Module to find usages in.
   * @param file Configuration file.
   * @return {@code true} if used in test context.
   */
  public abstract boolean isTestContext(Module module, PsiFile file);

  /**
   * Determines whether given configuration file is used explicitly (fileset) or
   * implicitly (e.g. included) in any Spring model in its module (or dependent modules).
   *
   * @param configurationFile Configuration file.
   * @param checkTestFiles Whether to check configuration files used in test contexts.
   * @return {@code true} if file is used. {@code false} in dumb mode or if file is not used.
   */
  public abstract boolean isUsedConfigurationFile(PsiFile configurationFile, boolean checkTestFiles);

  public abstract boolean hasAutoConfiguredModels(@Nullable Module module);
}
