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

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.FrameStateListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfiguration;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdateContextImpl;
import cn.taketoday.assistant.app.run.update.InfraApplicationUpdatePolicy;

final class InfraApplicationCompileManager {

  InfraApplicationCompileManager(Project project, Disposable parentDisposable) {

    ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(FrameStateListener.TOPIC, new FrameStateListener() {
      public void onFrameDeactivated() {
        if (!project.isDisposed() && project.isInitialized()
                && !CompilerManager.getInstance(project).isCompilationActive()
                && !LaterInvocator.isInModalContext() && !ProgressManager.getInstance()
                .hasModalProgressIndicator()) {
          ExecutorRegistry executorRegistry = ExecutorRegistry.getInstance();
          Map<InfraApplicationUpdatePolicy, List<InfraApplicationDescriptor>> appsToUpdate = new HashMap<>();
          InfraApplicationLifecycleManager.from(project).getRunningInfraApplications().forEach(descriptor -> {
            Executor executor;
            InfraApplicationUpdatePolicy policy;
            ProcessHandler handler = descriptor.getProcessHandler();
            if (!handler.isProcessTerminated() && !handler.isProcessTerminating()
                    && handler.isStartNotified()
                    && (executor = executorRegistry.getExecutorById(descriptor.getExecutorId())) != null
                    && (descriptor.getRunProfile() instanceof InfraApplicationRunConfiguration aconfig)
                    && (policy = aconfig.getFrameDeactivationUpdatePolicy()) != null
                    && policy.isAvailableForConfiguration(aconfig)
                    && policy.isAvailableForExecutor(executor)
                    && policy.isAvailableOnFrameDeactivation()) {
              (appsToUpdate.computeIfAbsent(policy, key -> new SmartList<>())).add(descriptor);
            }
          });
          appsToUpdate.forEach((policy, descriptors) -> {
            policy.runUpdate(new InfraApplicationUpdateContextImpl(project, descriptors, true));
          });
        }
      }
    });
  }
}
