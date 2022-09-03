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

import com.intellij.debugger.impl.attach.JavaDebuggerAttachUtil;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remote.RemoteProcess;
import com.intellij.util.net.NetUtils;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;

import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

class JmxServiceUrlLiveProperty extends AsyncLiveProperty<String> {
  private static final String JMX_CONNECTION_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
  private static final String LOCAL_JMX_CONNECTOR_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";
  private final ProcessHandler myProcessHandler;

  JmxServiceUrlLiveProperty(LifecycleErrorHandler errorHandler, Disposable parent, ProcessHandler processHandler) {
    super(errorHandler, parent, "");
    this.myProcessHandler = processHandler;
  }

  @Override
  public String doCompute() throws LifecycleException {
    String pid = null;
    try {
      Process process = ((BaseProcessHandler) this.myProcessHandler).getProcess();
      if (!(process instanceof RemoteProcess)) {
        pid = String.valueOf(OSProcessUtil.getProcessID(process));
      }
    }
    catch (IllegalStateException ignored) { }
    try {
      if (pid == null) {
        throw new AttachNotSupportedException();
      }
      VirtualMachine vm = JavaDebuggerAttachUtil.attachVirtualMachine(pid);
      try {
        String url = vm.getAgentProperties().getProperty(JMX_CONNECTION_ADDRESS_PROPERTY);
        if (StringUtil.isEmpty(url)) {
          throw new LifecycleException(message("infra.application.endpoints.error.jmx.agent.not.loaded"), null);
        }
        return url;
      }
      catch (Throwable e2) {
        try {
          String url2 = getConnectionUrlByPort();
          if (url2 == null) {
            throw new LifecycleException(
                    message("infra.application.endpoints.error.failed.to.retrieve.jmx.service.url"), e2);
          }
          try {
            vm.detach();
          }
          catch (IOException ignored) {
          }
          return url2;
        }
        finally {
          try {
            vm.detach();
          }
          catch (IOException ignored) {
          }
        }
      }
    }
    catch (IOException | AttachNotSupportedException e5) {
      String url3 = getConnectionUrlByPort();
      if (url3 == null) {
        throw new LifecycleException(null, new LifecycleException("", e5));
      }
      return url3;
    }
    catch (Throwable e6) {
      String url4 = getConnectionUrlByPort();
      if (url4 != null) {
        return url4;
      }
      Throwable cause = e6.getCause();
      if (cause != null && !(cause instanceof Exception)) {
        throw new LifecycleException(null, new LifecycleException("", e6));
      }
      throw new LifecycleException(message("infra.application.endpoints.error.failed.to.retrieve.jmx.service.url"), e6);
    }
  }

  @Nullable
  private String getConnectionUrlByPort() {
    Integer jmxPort = this.myProcessHandler.getUserData(InfraApplicationLifecycleManager.JMX_PORT);
    if (jmxPort == null || jmxPort <= 0) {
      return null;
    }
    return String.format(LOCAL_JMX_CONNECTOR_URL, NetUtils.getLocalHostString(), jmxPort);
  }
}
