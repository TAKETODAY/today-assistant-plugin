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

package cn.taketoday.assistant.app.run.lifecycle.health;

import com.intellij.execution.process.ProcessHandler;

import java.util.Map;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.health.tab.HealthTab;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.lang.Nullable;

final class HealthEndpoint extends Endpoint<Map> {
  private static final String ENDPOINT_ID = "health";

  public HealthEndpoint() {
    super(ENDPOINT_ID);
  }

  @Override
  public Map parseData(@Nullable Object data) {
    return (Map) data;
  }

  @Override
  public EndpointTab<Map> createEndpointTab(InfraApplicationRunConfigurationBase runConfiguration, ProcessHandler processHandler) {
    return new HealthTab(this, runConfiguration, processHandler);
  }

}
