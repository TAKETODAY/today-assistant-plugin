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

import java.util.Set;

import javax.management.InstanceNotFoundException;

class InfraApplicationConnector extends InfraJmxConnector {
  static final String SPRING_JMX_DEFAULT_DOMAIN_PROPERTY = "jmx.default-domain";
  static final String ENDPOINTS_JMX_ENABLED_PROPERTY_1X = "endpoints.jmx.enabled";
  static final String ENDPOINTS_JMX_EXPOSURE_INCLUDE_PROPERTY_2X = "management.endpoints.jmx.exposure.include";
  static final String ENDPOINTS_JMX_EXPOSURE_EXCLUDE_PROPERTY_2X = "management.endpoints.jmx.exposure.exclude";
  static final String SERVER_SSL_ENABLED_PROPERTY = "server.ssl.enabled";
  static final String SERVER_SSL_KEY_STORE_PROPERTY = "server.ssl.key-store";
  static final String SERVER_ADDRESS_PROPERTY = "server.address";

  private static final String READY_ATTR = "Ready";
  private static final String GET_PROPERTY_OPERATION = "getProperty";
  private static final String LOCAL_SERVER_PORT_PROPERTY = "local.server.port";
  private final InfraModuleDescriptor myModuleDescriptor;
  static final ApplicationProperty ENDPOINTS_JMX_DOMAIN_PROPERTY = new ApplicationProperty("management.endpoints.jmx.domain");
  static final ApplicationProperty ENDPOINTS_ENABLED_PROPERTY = new ApplicationProperty("management.endpoints.enabled-by-default");
  static final ApplicationProperty ENDPOINTS_ID_ENABLED_PROPERTY = new ApplicationProperty("management.endpoint.%s.enabled");
  static final ApplicationProperty SERVER_CONTEXT_PATH_PROPERTY = new ApplicationProperty("server.servlet.context-path");
  static final ApplicationProperty SERVER_SERVLET_PATH_PROPERTY = new ApplicationProperty("mvc.servlet.path");

  InfraApplicationConnector(String serviceUrl, InfraModuleDescriptor moduleDescriptor) {
    super(serviceUrl, moduleDescriptor.getApplicationAdminJmxName());
    this.myModuleDescriptor = moduleDescriptor;
  }

  Object getProperty(ApplicationProperty property) throws Exception {
    return getProperty(property.getKey());
  }

  Object getProperty(String propertyName) throws Exception {
    Object value = doGetProperty(propertyName);
    if (value != null) {
      return value;
    }
    Set<String> names = RelaxedNamesByParts.generateRelaxedNamesByParts(propertyName);
    names.remove(propertyName);
    for (String name : names) {
      Object value2 = doGetProperty(name);
      if (value2 != null) {
        return value2;
      }
    }
    return null;
  }

  boolean getBooleanProperty(String propertyName, boolean defaultValue) throws Exception {
    Object value = getProperty(propertyName);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }

  int getServerPort() throws Exception {
    Object value = getProperty(LOCAL_SERVER_PORT_PROPERTY);
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      }
      catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  boolean isReady() throws Exception {
    try {
      Object value = getJmxConnection().getAttribute(getObjectName(), READY_ATTR);
      if (!(value instanceof Boolean)) {
        return false;
      }
      return (Boolean) value;
    }
    catch (InstanceNotFoundException e) {
      return false;
    }
  }

  private Object doGetProperty(String propertyName) throws Exception {
    return getJmxConnection().invoke(getObjectName(), GET_PROPERTY_OPERATION, new String[] { propertyName }, new String[] { String.class.getName() });
  }

  static class ApplicationProperty {
    private final String key;

    ApplicationProperty(String key) {
      this.key = key;
    }

    String getKey() {
      return key;
    }
  }
}
