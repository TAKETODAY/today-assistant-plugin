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

package cn.taketoday.assistant.facet.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraConfigurationTabSettings;
import cn.taketoday.assistant.facet.InfraFileSet;

public class DependencyNode extends AbstractFilesetNode {
  private final boolean myOtherModule;

  public DependencyNode(InfraFileSet dependencyFileSet, InfraConfigurationTabSettings settings, SimpleNode parent, Module currentModule) {
    super(dependencyFileSet, settings, parent);
    this.myOtherModule = !currentModule.equals(getFileSet().getFacet().getModule());
  }

  protected void update(PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(Icons.ParentContext);
    presentation.addText(getFileSet().getName(), getPlainAttributes());
    if (this.myOtherModule) {
      presentation.addText(InfraBundle.message("dependency.node.in.module.tail", getFileSet().getFacet().getModule().getName()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  public SimpleNode[] getChildren() {
    return NO_CHILDREN;
  }

  public boolean isAlwaysLeaf() {
    return true;
  }
}
