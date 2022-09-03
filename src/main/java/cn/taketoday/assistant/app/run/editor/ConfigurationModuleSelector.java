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

package cn.taketoday.assistant.app.run.editor;

import com.intellij.application.options.ModulesCombo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.InfraLibraryUtil;

public final class ConfigurationModuleSelector extends com.intellij.execution.ui.ConfigurationModuleSelector {
  final ApplicationRunConfigurationFragmentedEditor this$0;
  final ModulesCombo $modulesCombo;

  ConfigurationModuleSelector(ApplicationRunConfigurationFragmentedEditor editor,
          ModulesCombo combo, Project project, ModulesCombo modulesCombo) {
    super(project, modulesCombo);
    this.this$0 = editor;
    this.$modulesCombo = combo;
  }

  public boolean isModuleAccepted(Module module) {
    return InfraLibraryUtil.hasFrameworkLibrary(module);
  }
}
