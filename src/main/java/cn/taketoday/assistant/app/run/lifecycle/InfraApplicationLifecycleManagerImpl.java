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

public final class InfraApplicationLifecycleManagerImpl implements InfraApplicationLifecycleManager, Disposable {
  public static final Key<Boolean> JMX_ENABLED = Key.create("INFRA_APPLICATION_JMX_ENABLED");
  private final Project myProject;
  private final Map<ProcessHandler, InfraApplicationDescriptor> myRunningApps = new ConcurrentHashMap();
  private final Map<ProcessHandler, InfraApplicationInfo> myInfos = new ConcurrentHashMap();
  private final List<InfoListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public InfraApplicationLifecycleManagerImpl(Project project) {
    this.myProject = project;
    init();
    new InfraApplicationCompileManager(this.myProject, this);
  }

  private void init() {
    this.myProject.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {

      public void processStarted(String executorId, ExecutionEnvironment env, ProcessHandler handler) {
        Module module;
        RunnerAndConfigurationSettings configurationSettings = env.getRunnerAndConfigurationSettings();
        if (configurationSettings == null
                || !(configurationSettings.getConfiguration() instanceof InfraApplicationRunConfiguration appRunConfig)
                || (module = appRunConfig.getModule()) == null) {
          return;
        }
        myRunningApps.put(handler, new InfraApplicationDescriptorImpl(handler, appRunConfig, executorId, module));
        if (!isLifecycleManagementEnabled(handler)) {
          return;
        }
        InfraApplicationInfo info = InfraApplicationInfoImpl.createInfo(
                myProject, appRunConfig.getModule(), env, appRunConfig, appRunConfig.getActiveProfiles(), handler);
        Disposer.register(InfraApplicationLifecycleManagerImpl.this, info);
        myInfos.put(handler, info);
        myListeners.forEach(listener -> {
          listener.infoAdded(handler, info);
        });

        XDebugSession session = getDebugSession(myProject, handler);
        if (session != null) {
          XDebugSessionListener sessionListener = new XDebugSessionListener() {
            public void sessionResumed() {
              myProject.getMessageBus()
                      .syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
                      .configurationChanged(appRunConfig, false);
            }
          };
          session.addSessionListener(sessionListener);
          info.getReadyState().addPropertyListener(new LiveProperty.LivePropertyListener() {
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
        myRunningApps.remove(handler);
        InfraApplicationInfo info = myInfos.remove(handler);
        if (info != null) {
          Disposer.dispose(info);
          myListeners.forEach(listener -> {
            listener.infoRemoved(handler, info);
          });
          DaemonCodeAnalyzer.getInstance(myProject).restart();
        }
      }
    });
  }

  public void dispose() {
    this.myRunningApps.clear();
    this.myInfos.forEach((handler, info) -> {
      this.myListeners.forEach(listener -> {
        listener.infoRemoved(handler, info);
      });
    });
    this.myInfos.clear();
  }

  @Override
  public boolean isLifecycleManagementEnabled(ProcessHandler handler) {
    return Boolean.TRUE.equals(handler.getUserData(JMX_ENABLED));
  }

  @Override
  @Nullable
  public InfraApplicationInfo getInfraApplicationInfo(ProcessHandler handler) {
    return this.myInfos.get(handler);
  }

  @Override
  public List<InfraApplicationInfo> getInfraApplicationInfos() {
    return new ArrayList<>(this.myInfos.values());
  }

  @Override
  @Nullable
  public InfraApplicationDescriptor getInfraApplicationDescriptor(ProcessHandler handler) {
    return this.myRunningApps.get(handler);
  }

  @Override
  public List<InfraApplicationDescriptor> getRunningInfraApplications() {
    return new ArrayList<>(this.myRunningApps.values());
  }

  @Override
  public void addInfoListener(InfraApplicationLifecycleManager.InfoListener listener) {
    this.myListeners.add(listener);
  }

  @Override
  public void removeInfoListener(InfraApplicationLifecycleManager.InfoListener listener) {
    this.myListeners.remove(listener);
  }

  @Nullable
  public static XDebugSession getDebugSession(Project project, ProcessHandler processHandler) {
    XDebugSession[] debugSessions;
    for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
      if (Comparing.equal(session.getDebugProcess().getProcessHandler(), processHandler)) {
        return session;
      }
    }
    return null;
  }
}
