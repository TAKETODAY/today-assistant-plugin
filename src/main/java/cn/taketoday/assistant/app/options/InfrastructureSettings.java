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
package cn.taketoday.assistant.app.options;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * Infra IDE settings (Languages & Frameworks | TODAY Infrastructure | App) for current project.
 */
@State(name = "InfrastructureOptions", storages = { @Storage("$WORKSPACE_FILE$") })
public class InfrastructureSettings implements Disposable, PersistentStateComponent<InfrastructureSettings> {
  private boolean showAdditionalConfigNotification = true;
  private boolean myAutoCreateRunConfiguration = true;
  private boolean myReformatAfterCreation = true;

  public InfrastructureSettings getState() {
    return this;
  }

  public void loadState(InfrastructureSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isShowAdditionalConfigNotification() {
    return this.showAdditionalConfigNotification;
  }

  public void setShowAdditionalConfigNotification(boolean showAdditionalConfigNotification) {
    this.showAdditionalConfigNotification = showAdditionalConfigNotification;
  }

  public boolean isAutoCreateRunConfiguration() {
    return this.myAutoCreateRunConfiguration;
  }

  public void setAutoCreateRunConfiguration(boolean autoCreateRunConfiguration) {
    this.myAutoCreateRunConfiguration = autoCreateRunConfiguration;
  }

  public boolean isReformatAfterCreation() {
    return this.myReformatAfterCreation;
  }

  public void setReformatAfterCreation(boolean reformatAfterCreation) {
    this.myReformatAfterCreation = reformatAfterCreation;
  }

  @Override
  public void dispose() {

  }

  public static InfrastructureSettings getInstance(Project project) {
    return project.getService(InfrastructureSettings.class);
  }
}
