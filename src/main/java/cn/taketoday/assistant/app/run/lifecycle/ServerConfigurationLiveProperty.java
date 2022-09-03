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

import cn.taketoday.lang.Nullable;

class ServerConfigurationLiveProperty extends AsyncApplicationLiveProperty<InfraApplicationServerConfiguration> {
  ServerConfigurationLiveProperty(LiveProperty<InfraModuleDescriptor> moduleDescriptor, LiveProperty<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent) {
    super(moduleDescriptor, serviceUrl, errorHandler, parent);
  }

  protected InfraApplicationServerConfiguration doCompute() {
    try {
      InfraApplicationConnector connector = this.getApplicationConnector();

      MyApplicationServerConfiguration var7;
      try {
        boolean sslEnabled = false;
        Object keyStore = connector.getProperty("server.ssl.key-store");
        if (keyStore != null) {
          sslEnabled = connector.getBooleanProperty("server.ssl.enabled", true);
        }

        Object contextPath = connector.getProperty(InfraApplicationConnector.SERVER_CONTEXT_PATH_PROPERTY);
        Object servletPath = connector.getProperty(InfraApplicationConnector.SERVER_SERVLET_PATH_PROPERTY);
        Object address = connector.getProperty("server.address");
        var7 = new MyApplicationServerConfiguration(sslEnabled, contextPath == null ? null : contextPath.toString(), servletPath == null ? null : servletPath.toString(),
                address == null ? null : address.toString());
      }
      catch (Throwable var9) {
        if (connector != null) {
          try {
            connector.close();
          }
          catch (Throwable var8) {
            var9.addSuppressed(var8);
          }
        }

        throw var9;
      }

      connector.close();

      return var7;
    }
    catch (Exception var10) {
      return null;
    }
  }

  private static class MyApplicationServerConfiguration implements InfraApplicationServerConfiguration {
    private final boolean mySslEnabled;
    private final String myContextPath;
    private final String myServletPath;
    private final String myAddress;

    MyApplicationServerConfiguration(boolean sslEnabled, String contextPath, String servletPath, String address) {
      this.mySslEnabled = sslEnabled;
      this.myContextPath = contextPath;
      this.myServletPath = servletPath;
      this.myAddress = address;
    }

    public boolean isSslEnabled() {
      return this.mySslEnabled;
    }

    @Nullable
    public String getContextPath() {
      return this.myContextPath;
    }

    @Nullable
    public String getServletPath() {
      return this.myServletPath;
    }

    @Nullable
    public String getAddress() {
      return this.myAddress;
    }
  }
}
