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

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XDebuggerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.lang.Nullable;

public final class InfraApplicationLifecycleManagerImpl
        implements InfraApplicationLifecycleManager, Disposable {
  public static final Key<Boolean> JMX_ENABLED = Key.create("INFRA_APPLICATION_JMX_ENABLED");

  private final Project project;
  private final Map<ProcessHandler, InfraApplicationInfo> infos = new ConcurrentHashMap<>();
  private final Map<ProcessHandler, InfraApplicationDescriptor> runningApps = new ConcurrentHashMap<>();
  private final List<InfoListener> listeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public InfraApplicationLifecycleManagerImpl(Project project) {
    this.project = project;
    init();
    new InfraApplicationCompileManager(this.project, this);
  }

  private void init() {
    project.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {

      @Override
      public void processStarted(String executorId, ExecutionEnvironment env, ProcessHandler handler) {
        Module module;
        RunnerAndConfigurationSettings configurationSettings = env.getRunnerAndConfigurationSettings();
        if (configurationSettings == null
                || !(configurationSettings.getConfiguration() instanceof InfraApplicationRunConfiguration appRunConfig)
                || (module = appRunConfig.getModule()) == null) {
          return;
        }
        runningApps.put(handler, new InfraApplicationDescriptorImpl(handler, appRunConfig, executorId, module));
        if (!isLifecycleManagementEnabled(handler)) {
          return;
        }
        InfraApplicationInfo info = InfraApplicationInfoImpl.createInfo(
                project, appRunConfig.getModule(), env, appRunConfig, appRunConfig.getActiveProfiles(), handler);
        Disposer.register(InfraApplicationLifecycleManagerImpl.this, info);
        infos.put(handler, info);
        listeners.forEach(listener -> listener.infoAdded(handler, info));

        XDebugSession session = getDebugSession(project, handler);
        if (session != null) {
          XDebugSessionListener sessionListener = new XDebugSessionListener() {
            public void sessionResumed() {
              project.getMessageBus()
                      .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                      .configurationChanged(appRunConfig, false);
            }
          };
          session.addSessionListener(sessionListener);
          info.getReadyState().addPropertyListener(new Property.PropertyListener() {
            @Override
            public void propertyChanged() {
            }

            @Override
            public void computationFinished() {
              session.removeSessionListener(sessionListener);
            }
          });
          Disposer.register(info, () -> {
            session.removeSessionListener(sessionListener);
          });
        }
      }

      public void processTerminating(String executorId, ExecutionEnvironment env, ProcessHandler handler) {
        runningApps.remove(handler);
        InfraApplicationInfo info = infos.remove(handler);
        if (info != null) {
          Disposer.dispose(info);
          listeners.forEach(listener -> {
            listener.infoRemoved(handler, info);
          });
          DaemonCodeAnalyzer.getInstance(project).restart();
        }
      }
    });
  }

  public void dispose() {
    this.runningApps.clear();
    this.infos.forEach((handler, info) -> {
      this.listeners.forEach(listener -> {
        listener.infoRemoved(handler, info);
      });
    });
    this.infos.clear();
  }

  @Override
  public boolean isLifecycleManagementEnabled(ProcessHandler handler) {
    return Boolean.TRUE.equals(handler.getUserData(JMX_ENABLED));
  }

  @Override
  @Nullable
  public InfraApplicationInfo getInfraApplicationInfo(ProcessHandler handler) {
    return this.infos.get(handler);
  }

  @Override
  public List<InfraApplicationInfo> getInfraApplicationInfos() {
    return new ArrayList<>(this.infos.values());
  }

  @Override
  @Nullable
  public InfraApplicationDescriptor getInfraApplicationDescriptor(ProcessHandler handler) {
    return this.runningApps.get(handler);
  }

  @Override
  public List<InfraApplicationDescriptor> getRunningInfraApplications() {
    return new ArrayList<>(this.runningApps.values());
  }

  @Override
  public void addInfoListener(InfraApplicationLifecycleManager.InfoListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeInfoListener(InfraApplicationLifecycleManager.InfoListener listener) {
    this.listeners.remove(listener);
  }

  @Nullable
  public static XDebugSession getDebugSession(Project project, ProcessHandler processHandler) {
    for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
      if (Comparing.equal(session.getDebugProcess().getProcessHandler(), processHandler)) {
        return session;
      }
    }
    return null;
  }
}
