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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiFile;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.util.containers.TreeTraversal;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.xml.config.ConfigFilesTreeBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import cn.taketoday.assistant.facet.InfraFileSet;

class InfraFilesTree extends CheckboxTreeBase {
  public InfraFilesTree() {
    super(new CheckboxTreeCellRendererBase() {
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ConfigFilesTreeBuilder.renderNode(value, expanded, getTextRenderer());
      }
    }, null);
    ConfigFilesTreeBuilder.installSearch(this);
  }

  public static boolean traverse(TreeNode node, Predicate<Object> traverse) {
    return TreeUtil.treeNodeTraverser(node).traverse(TreeTraversal.POST_ORDER_DFS).processEach(traverse::test);
  }

  public void updateFileSet(InfraFileSet fileSet) {
    Set<VirtualFile> configured = new HashSet<>();
    traverse((TreeNode) getModel().getRoot(), node -> {
      CheckedTreeNode checkedTreeNode = (CheckedTreeNode) node;
      if (!checkedTreeNode.isChecked()) {
        return true;
      }
      Object object = checkedTreeNode.getUserObject();
      VirtualFile virtualFile = null;
      if (object instanceof PsiFile) {
        virtualFile = ((PsiFile) object).getVirtualFile();
      }
      else if (object instanceof VirtualFile) {
        virtualFile = (VirtualFile) object;
      }
      if (virtualFile != null) {
        if (!fileSet.hasFile(virtualFile)) {
          fileSet.addFile(virtualFile);
        }
        configured.add(virtualFile);
        return true;
      }
      return true;
    });
    List<VirtualFilePointer> files = fileSet.getFiles();
    VirtualFilePointer[] pointers = files.toArray(VirtualFilePointer.EMPTY_ARRAY);
    for (VirtualFilePointer pointer : pointers) {
      VirtualFile file = pointer.getFile();
      if (file == null || !configured.contains(file)) {
        fileSet.removeFile(pointer);
      }
    }
  }
}
