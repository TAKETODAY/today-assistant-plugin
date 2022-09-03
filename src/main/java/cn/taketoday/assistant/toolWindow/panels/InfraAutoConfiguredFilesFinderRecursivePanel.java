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
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.FinderRecursivePanel;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.JList;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.lang.Nullable;

public class InfraAutoConfiguredFilesFinderRecursivePanel extends InfraModelConfigurationFilesRecursivePanel {

  public InfraAutoConfiguredFilesFinderRecursivePanel(FinderRecursivePanel panel, Module module) {
    super(panel, module);
  }

  @Override
  @Nullable
  protected InfraModel findModel() {
    return new CombinedInfraModelImpl(InfraAutoConfiguredModels.discoverAutoConfiguredModels(getModule()), getModule());
  }

  public void doCustomizeCellRenderer(SimpleColoredComponent comp, JList list, SmartPsiElementPointer<NavigatablePsiElement> value, int index, boolean selected, boolean hasFocus) {
    super.doCustomizeCellRenderer(comp, list, value, index, selected, hasFocus);
    comp.append(" " + InfraBundle.message("facet.context.autoconfigured.suffix"), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
  }
}
