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

import cn.taketoday.assistant.app.run.InfraRunBundle;

class ServerPortProperty extends AsyncApplicationProperty<Integer> {
  private static final long SERVER_PORT_RETRY_INTERVAL = 200;
  private static final int SERVER_PORT_RETRY_COUNT = 20;
  private static final Integer DEFAULT_VALUE = -1;

  ServerPortProperty(Property<InfraModuleDescriptor> moduleDescriptor, Property<String> serviceUrl,
          LifecycleErrorHandler errorHandler, Disposable parent) {
    super(moduleDescriptor, serviceUrl, errorHandler, parent, DEFAULT_VALUE);
  }

  @Override
  public Integer doCompute() throws LifecycleException {
    int serverPort = 0;
    if (isDisposed()) {
      return DEFAULT_VALUE;
    }
    InfraApplicationConnector connector = getApplicationConnector();
    Exception lastThrownException = null;
    for (int i = 0; i < SERVER_PORT_RETRY_COUNT; i++) {
      try {
        if (isDisposed()) {
          if (connector != null) {
            connector.close();
          }
          return DEFAULT_VALUE;
        }
        try {
          serverPort = connector.getServerPort();
        }
        catch (Exception e) {
          lastThrownException = e;
        }
        if (serverPort > 0) {
          Integer valueOf = serverPort;
          if (connector != null) {
            connector.close();
          }
          return valueOf;
        }
        try {
          Thread.sleep(SERVER_PORT_RETRY_INTERVAL);
        }
        catch (InterruptedException e2) {
          if (connector != null) {
            connector.close();
          }
          return DEFAULT_VALUE;
        }
      }
      catch (Throwable th) {
        if (connector != null) {
          try {
            connector.close();
          }
          catch (Throwable th2) {
            th.addSuppressed(th2);
          }
        }
        throw th;
      }
    }
    if (lastThrownException != null) {
      throw new LifecycleException(InfraRunBundle.message("infra.application.endpoints.error.failed.to.retrieve.application.port", lastThrownException.getLocalizedMessage()),
              lastThrownException);
    }
    if (connector != null) {
      connector.close();
    }
    return DEFAULT_VALUE;
  }
}
