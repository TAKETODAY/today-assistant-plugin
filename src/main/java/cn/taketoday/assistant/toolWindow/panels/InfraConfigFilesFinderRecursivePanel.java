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

package cn.taketoday.assistant.toolWindow.panels;

import com.intellij.openapi.module.Module;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.impl.InfraCombinedModelFactory;
import cn.taketoday.lang.Nullable;

public class InfraConfigFilesFinderRecursivePanel extends InfraModelConfigurationFilesRecursivePanel {

  private final InfraFileSet myFileSet;

  public InfraConfigFilesFinderRecursivePanel(InfraFileSetFinderRecursivePanel panel, InfraFileSet fileSet, Module module) {
    super(panel, module);
    this.myFileSet = fileSet;
    getProject().getMessageBus().connect(this).subscribe(InfraFileSetService.TOPIC, new InfraFileSetService.InfraFileSetListener() {
      @Override
      public void activeProfilesChanged() {
        InfraConfigFilesFinderRecursivePanel.this.updatePanel();
      }
    });
  }

  @Override
  @Nullable
  protected InfraModel findModel() {
    for (InfraModel model : InfraManager.from(getProject()).getAllModels(getModule())) {
      if (this.myFileSet.equals(model.getFileSet())) {
        return model;
      }
    }
    return InfraCombinedModelFactory.createModel(this.myFileSet, getModule());
  }
}
