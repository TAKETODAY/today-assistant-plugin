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

package cn.taketoday.assistant.app.run.lifecycle.beans.tab;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.JComponent;

import cn.taketoday.assistant.app.run.InfraApplicationRunConfigurationBase;
import cn.taketoday.assistant.app.run.lifecycle.beans.model.LiveBean;

public interface LiveBeansPanelContent {
  public static final ExtensionPointName<LiveBeansPanelContent> EP_NAME = ExtensionPointName.create("cn.taketoday.assistant.app.run.liveBeansPanelContent");

  JComponent createComponent(Project project, UserDataHolder userDataHolder, Disposable disposable, Supplier<? extends List<LiveBean>> supplier,
          InfraApplicationRunConfigurationBase infraApplicationRunConfigurationBase, boolean z);

  void update(UserDataHolder userDataHolder);

  Object getData(UserDataHolder userDataHolder,   String str);

  DefaultActionGroup createToolbarActions(UserDataHolder userDataHolder);
}
