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

package cn.taketoday.assistant.app.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.target.TargetEnvironment;
import com.intellij.execution.target.TargetEnvironmentRequest;
import com.intellij.util.net.NetUtils;

import java.util.Map;

import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManager;
import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationLifecycleManagerImpl;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

import static cn.taketoday.assistant.app.run.InfraRunBundle.message;

public abstract class InfraJmxSetup {
  private static final String JMX_REGISTRY_PORT = "com.sun.management.jmxremote.port";
  private static final String JMX_RMI_SERVER_PORT = "com.sun.management.jmxremote.rmi.port";
  private static final String JMX_REMOTE_AUTH = "com.sun.management.jmxremote.authenticate";
  private static final String JMX_REMOTE_SSL = "com.sun.management.jmxremote.ssl";
  private static final String JMX_RMI_SERVER_HOST = "java.rmi.server.hostname";
  private static final String JMX_REGISTRY_HOST = "com.sun.management.jmxremote.host";
  private static final String ANY_HOST = "0.0.0.0";
  private static final int DEFAULT_JMX_REGISTRY_PORT = 5000;

  public abstract void setupJmxParameters(JavaParameters javaParameters, TargetEnvironmentRequest targetEnvironmentRequest) throws ExecutionException;

  public abstract void handleCreatedEnvironment(TargetEnvironment targetEnvironment);

  public void setupProcessHandler(ProcessHandler processHandler) {
    processHandler.putUserData(InfraApplicationLifecycleManagerImpl.JMX_ENABLED, true);
  }

  public static final class LocalSetup extends InfraJmxSetup {
    private int myJmxPort = -1;

    @Override
    public void setupJmxParameters(JavaParameters params, TargetEnvironmentRequest targetRequest) {
      String auth;
      ParametersList vmParametersList = params.getVMParametersList();
      auth = vmParametersList.getPropertyValue("com.sun.management.jmxremote.authenticate");
      if (StringsKt.equals("false", auth, true)) {
        String ssl = vmParametersList.getPropertyValue("com.sun.management.jmxremote.ssl");
        if (StringsKt.equals("false", ssl, true)) {
          String jmxPort = vmParametersList.getPropertyValue("com.sun.management.jmxremote.port");
          if (jmxPort != null) {
            try {
              this.myJmxPort = Integer.parseInt(jmxPort);
            }
            catch (NumberFormatException ignored) { }
          }
        }
      }
    }

    @Override
    public void handleCreatedEnvironment(TargetEnvironment environment) { }

    @Override
    public void setupProcessHandler(ProcessHandler processHandler) {
      super.setupProcessHandler(processHandler);
      if (this.myJmxPort > 0) {
        processHandler.putUserData(InfraApplicationLifecycleManager.JMX_PORT, this.myJmxPort);
      }
    }
  }

  public static final class RemoteSetup extends InfraJmxSetup {
    private TargetEnvironment myResolvedEnvironment;
    private TargetEnvironment.TargetPortBinding myJmxPortBinding;

    @Override
    public void setupJmxParameters(JavaParameters params, TargetEnvironmentRequest targetRequest) {
      ParametersList vmParametersList = params.getVMParametersList();
      setupJmxPorts(vmParametersList, targetRequest);
      setupJmxHosts(vmParametersList);
      setupJmxSecurity(vmParametersList, targetRequest);
    }

    private void setupJmxPorts(ParametersList vmParametersList, TargetEnvironmentRequest targetRequest) {
      String jmxRegistryPort = vmParametersList.getPropertyValue(JMX_REGISTRY_PORT);
      String rmiServerPort = vmParametersList.getPropertyValue(JMX_RMI_SERVER_PORT);
      if (jmxRegistryPort == null || rmiServerPort == null || Intrinsics.areEqual(jmxRegistryPort, rmiServerPort)) {
        String str = jmxRegistryPort;
        if (str == null) {
          str = rmiServerPort;
        }
        if (str == null) {
          str = String.valueOf(NetUtils.tryToFindAvailableSocketPort(DEFAULT_JMX_REGISTRY_PORT));
        }
        String samePortForAll = str;
        if (jmxRegistryPort == null) {
          vmParametersList.addProperty(JMX_REGISTRY_PORT, samePortForAll);
        }
        if (rmiServerPort == null) {
          vmParametersList.addProperty(JMX_RMI_SERVER_PORT, samePortForAll);
        }
        try {
          int samePort = Integer.parseInt(samePortForAll);
          TargetEnvironment.TargetPortBinding it = new TargetEnvironment.TargetPortBinding(Integer.valueOf(samePort), samePort);
          targetRequest.getTargetPortBindings().add(it);
          this.myJmxPortBinding = it;
        }
        catch (NumberFormatException e) {
          throw new RuntimeException(message("infra.jmx.setup.cant.parse.jmx.port", jmxRegistryPort));
        }
      }
      throw new RuntimeException(message("infra.jmx.setup.for.remote.case.two.ports.should.match", jmxRegistryPort, rmiServerPort));
    }

    private void setupJmxSecurity(ParametersList vmParametersList, TargetEnvironmentRequest targetRequest) {
      vmParametersList.addProperty(JMX_REMOTE_AUTH, "false");
      vmParametersList.addProperty(JMX_REMOTE_SSL, "false");
    }

    private void setupJmxHosts(ParametersList vmParametersList) {
      addPropertyIfNotSetAlready(vmParametersList, JMX_REGISTRY_HOST, ANY_HOST);
      addPropertyIfNotSetAlready(vmParametersList, JMX_RMI_SERVER_HOST, ANY_HOST);
    }

    public void addPropertyIfNotSetAlready(ParametersList list, String propertyName, String propertyValue) {
      if (list.getPropertyValue(propertyName) == null) {
        list.addProperty(propertyName, propertyValue);
      }
    }

    @Override
    public void handleCreatedEnvironment(TargetEnvironment environment) {
      this.myResolvedEnvironment = environment;
    }

    @Override
    public void setupProcessHandler(ProcessHandler processHandler) {
      Map emptyMap;
      Integer num;
      super.setupProcessHandler(processHandler);
      if (myResolvedEnvironment != null) {
        emptyMap = myResolvedEnvironment.getTargetPortBindings();
      }
      else {
        emptyMap = MapsKt.emptyMap();
      }
      Map resolvedPorts = emptyMap;
      TargetEnvironment.TargetPortBinding it = this.myJmxPortBinding;
      if (it != null && (num = (Integer) resolvedPorts.get(it)) != null) {
        int port = num;
        processHandler.putUserData(InfraApplicationLifecycleManager.JMX_PORT, port);
      }
    }

  }

}
