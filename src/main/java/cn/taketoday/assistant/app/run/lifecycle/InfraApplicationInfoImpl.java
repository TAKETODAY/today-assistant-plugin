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
  private final RunConfiguration myConfiguration;
  private final LiveProperty<InfraModuleDescriptor> myModuleDescriptor;
  private final LiveProperty<Boolean> myReadyState;
  private final LiveProperty<Integer> myServerPort;
  private final LiveProperty<InfraApplicationServerConfiguration> myServerConfiguration;
  private final Map<String, LiveProperty<?>> myEndpoints;
  private final LiveProperty<String> myApplicationUrl;

  private volatile boolean myDisposed;

  static InfraApplicationInfo createInfo(Project project, Module module,
          ExecutionEnvironment environment, RunConfiguration configuration,
          @Nullable String activeProfiles, ProcessHandler processHandler) {

    InfraApplicationInfoImpl info = new InfraApplicationInfoImpl(project, module, environment, configuration, activeProfiles, processHandler);
    for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
      endpoint.infoCreated(project, processHandler, info);
    }
    info.myModuleDescriptor.compute();
    return info;
  }

  private InfraApplicationInfoImpl(Project project, Module module, ExecutionEnvironment environment, RunConfiguration configuration,
          @Nullable String activeProfiles, ProcessHandler processHandler) {
    this.myEndpoints = new HashMap<>();
    this.myConfiguration = configuration;
    LifecycleErrorHandler errorHandler = message -> {
      if (this.myDisposed) {
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
    this.myModuleDescriptor = new AsyncLiveProperty<>(errorHandler, this,
            InfraModuleDescriptor.DEFAULT_DESCRIPTOR) {
      @Override
      public InfraModuleDescriptor doCompute() {
        DumbService dumbService = DumbService.getInstance(project);
        return dumbService.runReadActionInSmartMode(() -> {
          InfraModuleDescriptor moduleDescriptor = InfraModuleDescriptor.getDescriptor(module, activeProfiles);
          for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
            moduleDescriptor.setEndpointAvailable(endpoint, endpoint.isAvailable(module));
          }
          return moduleDescriptor;
        });
      }
    };
    LiveProperty<String> serviceUrl = new JmxServiceUrlLiveProperty(errorHandler, this, processHandler);
    this.myReadyState = new ReadyStateLiveProperty(this.myModuleDescriptor, serviceUrl, errorHandler, this);
    this.myServerPort = MappedPortProperty.withMappedPorts(
            new ServerPortLiveProperty(this.myModuleDescriptor, serviceUrl, errorHandler, this), processHandler);
    this.myServerConfiguration = new ServerConfigurationLiveProperty(this.myModuleDescriptor, serviceUrl, errorHandler, this);
    Endpoint<LiveBeansModel> beansEndpoint = BeansEndpoint.getInstance();

    var beans3Endpoint = new EndpointLiveProperty<>(beansEndpoint,
            this.myModuleDescriptor, serviceUrl, errorHandler, this, null);
    LiveProperty<LiveBeansModel> liveBeansModel = new AsyncLiveProperty<>(errorHandler, this) {
      @Override
      public LiveBeansModel doCompute() throws LifecycleException {
        InfraModuleDescriptor moduleDescriptor = Objects.requireNonNull(
                myModuleDescriptor.getValue());
        var version = moduleDescriptor.getVersion();
        if (version != null) {
          return beans3Endpoint.doCompute();
        }
        InfraLiveBeansConnector connector = new InfraLiveBeansConnector(serviceUrl.getValue());
        try {
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
        catch (Throwable th) {
          try {
            connector.close();
          }
          catch (Throwable th2) {
            th.addSuppressed(th2);
          }
          throw th;
        }
      }
    };
    this.myEndpoints.put(beansEndpoint.getId(), liveBeansModel);
    for (Endpoint<?> endpoint : Endpoint.EP_NAME.getExtensions()) {
      if (endpoint != beansEndpoint) {
        LiveProperty<?> endpointData = new EndpointLiveProperty<>(endpoint, this.myModuleDescriptor, serviceUrl, errorHandler, this, liveBeansModel);
        this.myEndpoints.put(endpoint.getId(), endpointData);
      }
    }
    this.myApplicationUrl = new ApplicationUrlLiveProperty(errorHandler, this).withServerPort(this.myServerPort)
            .withServerConfiguration(this.myServerConfiguration);
    this.myModuleDescriptor.addPropertyListener(new LiveProperty.LivePropertyListener() {
      @Override
      public void propertyChanged() {
      }

      @Override
      public void computationFinished() {
        myReadyState.compute();
      }
    });
    this.myReadyState.addPropertyListener(new LiveProperty.LivePropertyListener() {

      @Override
      public void propertyChanged() {
        project.getMessageBus()
                .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                .configurationChanged(myConfiguration, false);
        myServerPort.compute();
        liveBeansModel.compute();
      }

      @Override
      public void computationFailed(Exception e) {
        project.getMessageBus()
                .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                .configurationChanged(myConfiguration, false);
      }

    });
    liveBeansModel.addPropertyListener(new LiveProperty.LivePropertyListener() {
      @Override
      public void propertyChanged() {
      }

      @Override
      public void computationFinished() {
        for (LiveProperty<?> endpointData2 : InfraApplicationInfoImpl.this.myEndpoints.values()) {
          if (endpointData2 != liveBeansModel) {
            endpointData2.compute();
          }
        }
      }
    });

    this.myServerPort.addPropertyListener(() -> {
      project.getMessageBus()
              .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
              .configurationChanged(this.myConfiguration, false);
      this.myServerConfiguration.compute();
    });
  }

  @Override
  public RunProfile getRunProfile() {
    return this.myConfiguration;
  }

  @Override
  public LiveProperty<Boolean> getReadyState() {
    return this.myReadyState;
  }

  @Override
  public LiveProperty<Integer> getServerPort() {
    return this.myServerPort;
  }

  @Override
  public LiveProperty<InfraApplicationServerConfiguration> getServerConfiguration() {
    return this.myServerConfiguration;
  }

  @Override
  public LiveProperty<String> getApplicationUrl() {
    return this.myApplicationUrl;
  }

  @Override
  public <T> LiveProperty<T> getEndpointData(Endpoint<T> endpoint) {
    return (LiveProperty<T>) this.myEndpoints.get(endpoint.getId());
  }

  public void dispose() {
    this.myDisposed = true;
  }
}
