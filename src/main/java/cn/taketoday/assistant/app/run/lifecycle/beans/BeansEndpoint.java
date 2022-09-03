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

package cn.taketoday.assistant.app.run.lifecycle.beans;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.CodeAnalyzerLivePropertyListener;
import cn.taketoday.assistant.app.run.lifecycle.Endpoint;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationInfo;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.impl.LiveBeansSnapshotParser;
import cn.taketoday.assistant.app.run.lifecycle.beans.tab.LiveBeansTab;
import cn.taketoday.assistant.app.run.lifecycle.tabs.EndpointTab;
import cn.taketoday.lang.Nullable;

public final class BeansEndpoint extends Endpoint<LiveBeansModel> {
  private static final String ENDPOINT_ID = "beans";

  public BeansEndpoint() {
    super(ENDPOINT_ID);
  }

  @Override
  public LiveBeansModel parseData(@Nullable Object data) {
    LiveBeansSnapshotParser parser = new LiveBeansSnapshotParser();
    return parser.parseEndpoint(data);
  }

  @Override

  public EndpointTab<LiveBeansModel> createEndpointTab(InfraApplicationRunConfigurationBase runConfiguration, ProcessHandler processHandler) {
    return new LiveBeansTab(this, runConfiguration, processHandler);
  }

  @Override
  public void infoCreated(Project project, ProcessHandler processHandler, InfraApplicationInfo info) {
    info.getEndpointData(this).addPropertyListener(new CodeAnalyzerLivePropertyListener(project));
  }

  public static Endpoint<LiveBeansModel> getInstance() {
    return Endpoint.EP_NAME.findExtension(BeansEndpoint.class);
  }
}
