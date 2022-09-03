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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import cn.taketoday.lang.Nullable;

@State(name = "InfraGeneralSettings", storages = { @Storage("$WORKSPACE_FILE$") })
public class InfraGeneralSettingsImpl extends InfraGeneralSettings implements PersistentStateComponent<InfraGeneralSettingsImpl> {
  private boolean showProfilesPanel = true;
  private boolean showMultipleContextsPanel = true;
  private boolean allowAutoConfigurationMode;

  @Nullable
  public InfraGeneralSettingsImpl getState() {
    return this;
  }

  public void loadState(InfraGeneralSettingsImpl state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public boolean isShowProfilesPanel() {
    return this.showProfilesPanel;
  }

  @Override
  public void setShowProfilesPanel(boolean showProfilesPanel) {
    this.showProfilesPanel = showProfilesPanel;
  }

  @Override
  public boolean isShowMultipleContextsPanel() {
    return this.showMultipleContextsPanel;
  }

  @Override
  public void setShowMultipleContextsPanel(boolean showMultipleContextsPanel) {
    this.showMultipleContextsPanel = showMultipleContextsPanel;
  }

  @Override
  public boolean isAllowAutoConfigurationMode() {
    return this.allowAutoConfigurationMode;
  }

  @Override
  public void setAllowAutoConfigurationMode(boolean allowAutoConfigurationMode) {
    this.allowAutoConfigurationMode = allowAutoConfigurationMode;
  }

  public void dispose() {
  }
}
