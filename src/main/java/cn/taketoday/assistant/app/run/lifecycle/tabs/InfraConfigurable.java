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

package cn.taketoday.assistant.app.run.lifecycle.tabs;

import com.intellij.openapi.options.ConfigurableBase;
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.app.options.InfrastructureSettings;

import static cn.taketoday.assistant.InfraAppBundle.message;

public final class InfraConfigurable extends ConfigurableBase<InfraSettingsUi, InfrastructureSettings> {
  private final Project project;

  public InfraConfigurable(Project project) {
    super("Infrastructure", message("infra.setting.display-name"), "settings.infrastructure");
    this.project = project;
  }

  public InfrastructureSettings getSettings() {
    return InfrastructureSettings.getInstance(this.project);
  }

  public InfraSettingsUi createUi() {
    return new InfraSettingsUi(this.project);
  }
}
