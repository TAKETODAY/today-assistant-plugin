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

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.assistant.app.run.InfraRunBundle;
import cn.taketoday.assistant.app.run.lifecycle.beans.BeansEndpoint;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBeansModel;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.impl.LiveBeansSnapshotParser;
import cn.taketoday.lang.Nullable;

final class InfraApplicationInfoImpl implements InfraApplicationInfo {

  private static final String NOTIFICATION_DISPLAY_ID = "Infrastructure Application Endpoints";

  private final Property<Boolean> readyState;
  private final Property<Integer> serverPort;
  private final Property<String> applicationUrl;
  private final Property<InfraWebServerConfig> serverConfig;
  private final Property<InfraModuleDescriptor> moduleDescriptor;

  private final RunConfiguration configuration;
  private final Map<String, Property<?>> endpoints;

  private volatile boolean disposed;

  static InfraApplicationInfo createInfo(Project project, Module module,
          ExecutionEnvironment environment, RunConfiguration configuration,
          @Nullable String activeProfiles, ProcessHandler processHandler) {

    var info = new InfraApplicationInfoImpl(
            project, module, environment, configuration, activeProfiles, processHandler);
    for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
      endpoint.infoCreated(project, processHandler, info);
    }
    info.moduleDescriptor.compute();
    return info;
  }

  private InfraApplicationInfoImpl(Project project, Module module,
          ExecutionEnvironment environment, RunConfiguration configuration,
          @Nullable String activeProfiles, ProcessHandler processHandler) {
    this.endpoints = new HashMap<>();
    this.configuration = configuration;
    LifecycleErrorHandler errorHandler = message -> {
      if (disposed) {
        return;
      }
      String toolWindowId = RunContentManager.getInstance(project).getToolWindowIdByEnvironment(environment);
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
      if (toolWindowManager.canShowNotification(toolWindowId)) {
        new NotificationGroup(NOTIFICATION_DISPLAY_ID, NotificationDisplayType.NONE, true, toolWindowId)
                .createNotification(configuration.getName(), message, NotificationType.ERROR)
                .notify(project);
      }
    };
    this.moduleDescriptor = new AsyncProperty<>(
            errorHandler, this, InfraModuleDescriptor.DEFAULT_DESCRIPTOR) {
      @Override
      public InfraModuleDescriptor doCompute() {
        DumbService dumbService = DumbService.getInstance(project);
        return dumbService.runReadActionInSmartMode(() -> {
          var moduleDescriptor = InfraModuleDescriptor.getDescriptor(module, activeProfiles);
          for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
            moduleDescriptor.setEndpointAvailable(endpoint, endpoint.isAvailable(module));
          }
          return moduleDescriptor;
        });
      }
    };
    var serviceUrl = new JmxServiceUrlProperty(errorHandler, this, processHandler);
    this.readyState = new ReadyStateProperty(moduleDescriptor, serviceUrl, errorHandler, this);
    this.serverPort = MappedPortProperty.withMappedPorts(
            new ServerPortProperty(moduleDescriptor, serviceUrl, errorHandler, this), processHandler);
    this.serverConfig = new ServerConfigurationProperty(moduleDescriptor, serviceUrl, errorHandler, this);
    var beansEndpoint = BeansEndpoint.of();

    var beans3Endpoint = new EndpointProperty<>(beansEndpoint,
            moduleDescriptor, serviceUrl, errorHandler, this, null);
    Property<LiveBeansModel> liveBeansModel = new AsyncProperty<>(errorHandler, this) {
      @Override
      public LiveBeansModel doCompute() throws LifecycleException {
        var moduleDescriptor = Objects.requireNonNull(InfraApplicationInfoImpl.this.moduleDescriptor.getValue());
        var version = moduleDescriptor.getVersion();
        if (version != null) {
          return beans3Endpoint.doCompute();
        }

        try (var connector = new InfraLiveBeansConnector(serviceUrl.getValue())) {
          try {
            String snapshot = connector.getSnapshot();
            LiveBeansSnapshotParser parser = new LiveBeansSnapshotParser();
            LiveBeansModel parse = parser.parse(snapshot);
            connector.close();
            return parse;
          }
          catch (Exception e) {
            throw new LifecycleException(InfraRunBundle.message("infra.application.endpoints.error.failed.to.retrieve.application.beans.snapshot", e.getLocalizedMessage()), e);
          }
        }
      }
    };
    endpoints.put(beansEndpoint.getId(), liveBeansModel);
    for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
      if (endpoint != beansEndpoint) {
        var endpointData = new EndpointProperty<>(endpoint, moduleDescriptor, serviceUrl, errorHandler, this, liveBeansModel);
        endpoints.put(endpoint.getId(), endpointData);
      }
    }
    this.applicationUrl = new ApplicationUrlProperty(errorHandler, this).withServerPort(serverPort)
            .withServerConfiguration(serverConfig);
    moduleDescriptor.addPropertyListener(new Property.PropertyListener() {
      @Override
      public void propertyChanged() { }

      @Override
      public void computationFinished() {
        readyState.compute();
      }
    });
    readyState.addPropertyListener(new Property.PropertyListener() {

      @Override
      public void propertyChanged() {
        project.getMessageBus()
                .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                .configurationChanged(InfraApplicationInfoImpl.this.configuration, false);
        serverPort.compute();
        liveBeansModel.compute();
      }

      @Override
      public void computationFailed(Exception e) {
        project.getMessageBus()
                .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                .configurationChanged(InfraApplicationInfoImpl.this.configuration, false);
      }

    });
    liveBeansModel.addPropertyListener(new Property.PropertyListener() {
      @Override
      public void propertyChanged() { }

      @Override
      public void computationFinished() {
        for (Property<?> property : endpoints.values()) {
          if (property != liveBeansModel) {
            property.compute();
          }
        }
      }
    });

    serverPort.addPropertyListener(() -> {
      project.getMessageBus()
              .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
              .configurationChanged(this.configuration, false);
      serverConfig.compute();
    });
  }

  @Override
  public RunProfile getRunProfile() {
    return configuration;
  }

  @Override
  public Property<Boolean> getReadyState() {
    return readyState;
  }

  @Override
  public Property<Integer> getServerPort() {
    return serverPort;
  }

  @Override
  public Property<InfraWebServerConfig> getServerConfig() {
    return serverConfig;
  }

  @Override
  public Property<String> getApplicationUrl() {
    return applicationUrl;
  }

  @Override
  public <T> Property<T> getEndpointData(Endpoint<T> endpoint) {
    return (Property<T>) endpoints.get(endpoint.getId());
  }

  public void dispose() {
    this.disposed = true;
  }
}
