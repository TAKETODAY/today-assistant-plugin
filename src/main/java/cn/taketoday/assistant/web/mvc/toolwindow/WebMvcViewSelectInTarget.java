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

package cn.taketoday.assistant.web.mvc.toolwindow;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.InfraLibraryUtil;

public class WebMvcViewSelectInTarget implements SelectInTarget {
  public String toString() {
    return InfraAppBundle.message("web.mvc.framework.name");
  }

  public String getToolWindowId() {
    return "Infra";
  }

  public float getWeight() {
    return 3.0f;
  }

  public boolean canSelect(SelectInContext context) {
    Project project = context.getProject();
    if (DumbService.isDumb(project)
            || !InfraLibraryUtil.isWebMVCEnabled(project)
            || ToolWindowManager.getInstance(project).getToolWindow("Infra") == null) {
      return false;
    }
    return new WebMvcViewSelectInTargetPathBuilder(context).canSelect();
  }

  public void selectIn(SelectInContext context, boolean requestFocus) {
    WebMvcView.selectIn(context.getProject(),
            new WebMvcViewSelectInTargetPathBuilder(context).getPath(), requestFocus);
  }
}
