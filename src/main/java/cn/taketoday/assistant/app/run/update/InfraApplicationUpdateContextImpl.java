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

package cn.taketoday.assistant.app.run.update;

import com.intellij.openapi.project.Project;

import java.util.List;

import cn.taketoday.assistant.app.run.lifecycle.InfraApplicationDescriptor;

public class InfraApplicationUpdateContextImpl implements InfraApplicationUpdateContext {
  private final Project myProject;
  private final List<InfraApplicationDescriptor> myDescriptors;
  private final boolean myOnFrameDeactivation;

  public InfraApplicationUpdateContextImpl(Project project, List<InfraApplicationDescriptor> descriptors, boolean onFrameDeactivation) {
    this.myProject = project;
    this.myDescriptors = descriptors;
    this.myOnFrameDeactivation = onFrameDeactivation;
  }

  @Override
  public Project getProject() {
    return this.myProject;
  }

  @Override
  public List<InfraApplicationDescriptor> getDescriptors() {
    return this.myDescriptors;
  }

  @Override
  public boolean isOnFrameDeactivation() {
    return this.myOnFrameDeactivation;
  }
}
