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

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author konstantin.aleev
 */
public interface InfraApplicationLifecycleManager {
  Key<Integer> JMX_PORT = Key.create("INFRA_APPLICATION_JMX_PORT");

  static InfraApplicationLifecycleManager from(Project project) {
    return project.getService(InfraApplicationLifecycleManager.class);
  }

  boolean isLifecycleManagementEnabled(ProcessHandler handler);

  /**
   * @param handler process handler associated with Spring Boot application.
   * @return {@code null} if lifecycle support is not enabled for the given process handler
   * or associated process is not started or already stopped.
   */
  @Nullable
  InfraApplicationInfo getInfraApplicationInfo(ProcessHandler handler);

  List<InfraApplicationInfo> getInfraApplicationInfos();

  /**
   * @param handler process handler.
   * @return {@code null} if process handler is not associated with Spring Boot application
   * or associated process is not started or already stopped.
   */
  @Nullable
  InfraApplicationDescriptor getInfraApplicationDescriptor(ProcessHandler handler);

  List<InfraApplicationDescriptor> getRunningInfraApplications();

  void addInfoListener(InfoListener listener);

  void removeInfoListener(InfoListener listener);

  interface InfoListener {
    void infoAdded(ProcessHandler processHandler, InfraApplicationInfo info);

    void infoRemoved(ProcessHandler processHandler, InfraApplicationInfo info);
  }
}
