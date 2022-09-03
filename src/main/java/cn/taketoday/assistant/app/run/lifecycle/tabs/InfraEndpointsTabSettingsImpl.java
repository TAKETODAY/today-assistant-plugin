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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.XmlSerializerUtil;

import cn.taketoday.lang.Nullable;

@State(name = "InfraEndpointsTabSettings", storages = { @Storage("$WORKSPACE_FILE$") })
final class InfraEndpointsTabSettingsImpl
        implements InfraEndpointsTabSettings, PersistentStateComponent<InfraEndpointsTabSettingsImpl.Settings> {
  private final MessageBus myMessageBus;
  private final Settings mySettings;

  public static class Settings {
    public String selectedTab;
  }

  InfraEndpointsTabSettingsImpl(Project project) {
    this.mySettings = new Settings();
    this.myMessageBus = project.getMessageBus();
  }

  public Settings getState() {
    return this.mySettings;
  }

  public void loadState(Settings settings) {
    XmlSerializerUtil.copyBean(settings, this.mySettings);
  }

  @Override
  public void fireSettingsChanged(String changeType) {
    this.myMessageBus.syncPublisher(TOPIC).settingsChanged(changeType);
  }

  @Override
  @Nullable
  public String getSelectedTab() {
    return this.mySettings.selectedTab;
  }

  @Override
  public void setSelectedTab(@Nullable String selectedTab) {
    this.mySettings.selectedTab = selectedTab;
  }
}
