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

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.module.Module;

class InfraApplicationDescriptorImpl implements InfraApplicationDescriptor {
  private final ProcessHandler myProcessHandler;
  private final RunProfile myRunProfile;
  private final String myExecutorId;
  private final Module myModule;

  InfraApplicationDescriptorImpl(ProcessHandler processHandler, RunProfile runProfile, String executorId, Module module) {
    this.myProcessHandler = processHandler;
    this.myRunProfile = runProfile;
    this.myExecutorId = executorId;
    this.myModule = module;
  }

  @Override
  public ProcessHandler getProcessHandler() {
    return this.myProcessHandler;
  }

  @Override
  public RunProfile getRunProfile() {
    return this.myRunProfile;
  }

  @Override
  public String getExecutorId() {
    return this.myExecutorId;
  }

  @Override
  public Module getModule() {
    return this.myModule;
  }
}
