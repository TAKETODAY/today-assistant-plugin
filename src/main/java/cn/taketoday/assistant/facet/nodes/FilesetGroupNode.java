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
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.ui.treeStructure.SimpleNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

public abstract class FilesetGroupNode extends SimpleNode {
  private final Set<VirtualFilePointer> myChildren;
  private final cn.taketoday.assistant.facet.nodes.FileSetNode myFilesetNode;

  protected abstract String getGroupName();

  protected abstract Icon getGroupNodeIcon();

  public FilesetGroupNode(FileSetNode parent, Set<VirtualFilePointer> children) {
    super(parent);
    this.myFilesetNode = parent;
    this.myChildren = children;
  }

  public SimpleNode[] getChildren() {
    List<ConfigFileNode> nodes = new ArrayList<>(this.myChildren.size());
    for (VirtualFilePointer file : this.myChildren) {
      nodes.add(createNode(file));
    }
    if (myFilesetNode.getConfigurationTabSettings().isSortAlpha()) {
      nodes.sort(FileSetNode.FILENAME_COMPARATOR);
    }
    return nodes.toArray(new SimpleNode[0]);
  }

  protected cn.taketoday.assistant.facet.nodes.ConfigFileNode createNode(VirtualFilePointer virtualFilePointer) {
    return new ConfigFileNode(myFilesetNode.getConfigurationTabSettings(), myFilesetNode.getFileSet(), virtualFilePointer, this);
  }

  protected FileSetNode getFilesetNode() {
    return this.myFilesetNode;
  }

  public Set<VirtualFilePointer> getFilePointers() {
    return this.myChildren;
  }

  public boolean isAutoExpandNode() {
    return true;
  }

  protected void update(PresentationData presentation) {
    super.update(presentation);
    presentation.setPresentableText(getGroupName());
    presentation.setIcon(getGroupNodeIcon());
  }

  public Object[] getEqualityObjects() {
    return new Object[] { getGroupName(), getParent() };
  }
}
