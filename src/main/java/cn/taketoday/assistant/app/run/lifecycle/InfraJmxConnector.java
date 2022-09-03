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

import java.io.Closeable;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

class InfraJmxConnector implements Closeable {
  private final String myServiceUrl;
  private final ObjectName myObjectName;
  private JMXConnector myConnector;
  private MBeanServerConnection myConnection;

  InfraJmxConnector(String serviceUrl, String objectName) {
    this.myServiceUrl = serviceUrl;
    this.myObjectName = toObjectName(objectName);
  }

  private JMXConnector getJmxConnector() throws IOException {
    return JMXConnectorFactory.connect(new JMXServiceURL(this.myServiceUrl), null);
  }

  protected MBeanServerConnection getJmxConnection() throws IOException {
    if (this.myConnection == null) {
      if (this.myConnector == null) {
        this.myConnector = getJmxConnector();
      }
      this.myConnection = this.myConnector.getMBeanServerConnection();
    }
    return this.myConnection;
  }

  protected ObjectName getObjectName() {
    return this.myObjectName;
  }

  @Override
  public void close() {
    if (this.myConnector != null) {
      try {
        this.myConnector.close();
      }
      catch (IOException ignored) { }
      this.myConnector = null;
    }
  }

  private static ObjectName toObjectName(String objectName) {
    try {
      return new ObjectName(objectName);
    }
    catch (MalformedObjectNameException e) {
      throw new IllegalArgumentException("Invalid JMX object name '" + objectName + "'");
    }
  }
}
