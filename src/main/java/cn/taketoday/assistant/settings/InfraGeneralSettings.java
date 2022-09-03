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

package cn.taketoday.assistant.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

/**
 * Provides access to Project-level Spring settings.
 */
public abstract class InfraGeneralSettings implements Disposable {

  public abstract boolean isShowProfilesPanel();

  public abstract void setShowProfilesPanel(boolean showProfilesPanel);

  public abstract boolean isShowMultipleContextsPanel();

  public abstract void setShowMultipleContextsPanel(boolean showMultipleContextsPanel);

  public abstract boolean isAllowAutoConfigurationMode();

  public abstract void setAllowAutoConfigurationMode(boolean allowAutoConfigurationMode);

  public static InfraGeneralSettings from(Project project) {
    return project.getService(InfraGeneralSettings.class);
  }
}
