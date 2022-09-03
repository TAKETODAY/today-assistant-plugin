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

package cn.taketoday.assistant.app.run.lifecycle.health.tab;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.concurrent.TimeUnit;

@State(name = "InfraHealthEndpointTabSettings", storages = { @Storage("$WORKSPACE_FILE$") })
final class HealthEndpointTabSettings implements PersistentStateComponent<HealthEndpointTabSettings> {
  private boolean checkHealth = true;
  private long healthCheckDelay = TimeUnit.SECONDS.toMillis(15);

  public static HealthEndpointTabSettings getInstance(Project project) {
    return project.getService(HealthEndpointTabSettings.class);
  }

  public HealthEndpointTabSettings getState() {
    return this;
  }

  public void loadState(HealthEndpointTabSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isCheckHealth() {
    return this.checkHealth;
  }

  public void setCheckHealth(boolean checkHealth) {
    this.checkHealth = checkHealth;
  }

  public long getHealthCheckDelay() {
    return this.healthCheckDelay;
  }

  public void setHealthCheckDelay(long healthCheckDelay) {
    this.healthCheckDelay = healthCheckDelay;
  }
}
