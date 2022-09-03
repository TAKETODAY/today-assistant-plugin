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

package cn.taketoday.assistant.facet.editor;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFileSet;

class ParentContextListCellRenderer extends ListCellRendererWithRightAlignedComponent<InfraFileSet> {
  public void customize(InfraFileSet fileSet) {
    String leftText;
    if (fileSet == null) {
      leftText = InfraBundle.message("facet.context.edit.parent.none.selected");
    }
    else {
      leftText = fileSet.getName();
    }
    setLeftText(leftText);
    if (fileSet != null) {
      Module module = fileSet.getFacet().getModule();
      if (module.isDisposed()) {
        clear();
        return;
      }
      setIcon(fileSet.getIcon());
      setRightText(module.getName());
      setRightIcon(ModuleType.get(module).getIcon());
      return;
    }
    clear();
  }

  private void clear() {
    setIcon(Icons.ParentContext);
    setRightText("");
    setRightIcon(null);
  }
}
