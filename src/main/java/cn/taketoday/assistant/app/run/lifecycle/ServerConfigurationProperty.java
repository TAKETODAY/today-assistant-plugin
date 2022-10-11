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

import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.SERVER_ADDRESS_PROPERTY;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.SERVER_SSL_ENABLED_PROPERTY;
import static cn.taketoday.assistant.app.run.lifecycle.InfraApplicationConnector.SERVER_SSL_KEY_STORE_PROPERTY;

class ServerConfigurationProperty extends AsyncApplicationProperty<InfraWebServerConfig> {

  ServerConfigurationProperty(Property<InfraModuleDescriptor> moduleDescriptor,
          Property<String> serviceUrl, LifecycleErrorHandler errorHandler, Disposable parent) {
    super(moduleDescriptor, serviceUrl, errorHandler, parent);
  }

  @Override
  protected InfraWebServerConfig doCompute() {
    try (InfraApplicationConnector connector = this.getApplicationConnector()) {
      boolean sslEnabled = false;
      Object keyStore = connector.getProperty(SERVER_SSL_KEY_STORE_PROPERTY);
      if (keyStore != null) {
        sslEnabled = connector.getBooleanProperty(SERVER_SSL_ENABLED_PROPERTY, true);
      }

      Object contextPath = connector.getProperty(InfraApplicationConnector.SERVER_CONTEXT_PATH_PROPERTY);
      Object servletPath = connector.getProperty(InfraApplicationConnector.SERVER_SERVLET_PATH_PROPERTY);
      Object address = connector.getProperty(SERVER_ADDRESS_PROPERTY);
      return new InfraWebServerConfig(
              sslEnabled, toString(contextPath), toString(servletPath), toString(address));
    }
    catch (Exception e) {
      return null;
    }
  }

  @Nullable
  private static String toString(Object obj) {
    return obj == null ? null : obj.toString();
  }

}
