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

package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.net.NetUtils;

class ApplicationUrlLiveProperty extends cn.taketoday.assistant.app.run.lifecycle.AsyncLiveProperty<String> {
  private cn.taketoday.assistant.app.run.lifecycle.LiveProperty<Integer> myServerPort;
  private cn.taketoday.assistant.app.run.lifecycle.LiveProperty<InfraApplicationServerConfiguration> myServerConfiguration;

  protected ApplicationUrlLiveProperty(cn.taketoday.assistant.app.run.lifecycle.LifecycleErrorHandler errorHandler, Disposable parent) {
    super(errorHandler, parent);
  }

  ApplicationUrlLiveProperty withServerPort(cn.taketoday.assistant.app.run.lifecycle.LiveProperty<Integer> serverPort) {
    this.myServerPort = serverPort;
    this.myServerPort.addPropertyListener(() -> {
      compute();
    });
    return this;
  }

  ApplicationUrlLiveProperty withServerConfiguration(LiveProperty<InfraApplicationServerConfiguration> serverConfiguration) {
    this.myServerConfiguration = serverConfiguration;
    this.myServerConfiguration.addPropertyListener(() -> {
      compute();
    });
    return this;
  }

  @Override
  public String doCompute() {
    Integer serverPort;
    if (this.myServerPort == null || (serverPort = this.myServerPort.getValue()) == null || serverPort.intValue() <= 0) {
      return null;
    }
    String path = "";
    String scheme = "http";
    String address = NetUtils.getLocalHostString();
    InfraApplicationServerConfiguration serverConfiguration = this.myServerConfiguration != null ? this.myServerConfiguration.getValue() : null;
    if (serverConfiguration != null) {
      if (serverConfiguration.isSslEnabled()) {
        scheme = "https";
      }
      String contextPath = serverConfiguration.getContextPath();
      if (contextPath != null) {
        path = path + contextPath;
      }
      String serverAddress = serverConfiguration.getAddress();
      if (StringUtil.isNotEmpty(serverAddress)) {
        address = serverAddress;
      }
    }
    return scheme + "://" + address + ":" + serverPort + path;
  }
}
