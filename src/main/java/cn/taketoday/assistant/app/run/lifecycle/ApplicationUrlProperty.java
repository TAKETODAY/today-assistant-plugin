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

class ApplicationUrlProperty extends AsyncProperty<String> {
  private Property<Integer> serverPort;
  private Property<InfraWebServerConfig> serverConfig;

  protected ApplicationUrlProperty(LifecycleErrorHandler errorHandler, Disposable parent) {
    super(errorHandler, parent);
  }

  ApplicationUrlProperty withServerPort(Property<Integer> serverPort) {
    this.serverPort = serverPort;
    this.serverPort.addPropertyListener(this::compute);
    return this;
  }

  ApplicationUrlProperty withServerConfiguration(Property<InfraWebServerConfig> serverConfig) {
    this.serverConfig = serverConfig;
    this.serverConfig.addPropertyListener(this::compute);
    return this;
  }

  @Override
  public String doCompute() {
    Integer serverPort;
    if (this.serverPort == null || (serverPort = this.serverPort.getValue()) == null || serverPort <= 0) {
      return null;
    }
    String path = "";
    String scheme = "http";
    String address = NetUtils.getLocalHostString();
    InfraWebServerConfig serverConfig = this.serverConfig != null ? this.serverConfig.getValue() : null;
    if (serverConfig != null) {
      if (serverConfig.sslEnabled()) {
        scheme = "https";
      }
      String contextPath = serverConfig.contextPath();
      if (contextPath != null) {
        path = path + contextPath;
      }
      String serverAddress = serverConfig.address();
      if (StringUtil.isNotEmpty(serverAddress)) {
        address = serverAddress;
      }
    }
    return scheme + "://" + address + ":" + serverPort + path;
  }
}
