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

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraConfigurationTabSettings;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetEditorCustomization;

public class FileSetNode extends AbstractFilesetNode {
  static final Comparator<ConfigFileNode> FILENAME_COMPARATOR = new FilenameComparator();

  public FileSetNode(InfraFileSet fileSet, InfraConfigurationTabSettings settings, SimpleNode parent) {
    super(fileSet, settings, parent);
  }

  public SimpleNode[] getChildren() {
    List<SimpleNode> nodes = new ArrayList<>();
    InfraFileSet fileSet = getFileSet();
    InfraConfigurationTabSettings settings = getConfigurationTabSettings();
    Set<InfraFileSet> dependencyFileSets = fileSet.getDependencyFileSets();
    for (InfraFileSet dependencyFileSet : dependencyFileSets) {
      nodes.add(new DependencyNode(dependencyFileSet, settings, this, getFileSet().getFacet().getModule()));
    }
    Set<VirtualFilePointer> allCustomGroupFiles = new HashSet<>();
    SmartList<InfraFileSetEditorCustomization.CustomConfigFileGroup> smartList = new SmartList();
    for (InfraFileSetEditorCustomization customization : InfraFileSetEditorCustomization.array()) {
      if (customization.isApplicable(fileSet)) {
        List<InfraFileSetEditorCustomization.CustomConfigFileGroup> groups = customization.getCustomConfigFileGroups(fileSet);
        smartList.addAll(groups);
        for (InfraFileSetEditorCustomization.CustomConfigFileGroup group : groups) {
          allCustomGroupFiles.addAll(group.getFiles());
        }
      }
    }
    Set<VirtualFilePointer> xmlFiles = fileSet.getXmlFiles();
    xmlFiles.removeAll(allCustomGroupFiles);
    Set<VirtualFilePointer> codeFiles = fileSet.getCodeConfigurationFiles();
    codeFiles.removeAll(allCustomGroupFiles);
    if (!xmlFiles.isEmpty() && !codeFiles.isEmpty()) {
      nodes.add(new XmlGroupNode(this, xmlFiles));
      nodes.add(new CodeGroupNode(this, codeFiles));
    }
    else {
      List<ConfigFileNode> oneKindOfConfigFiles = new ArrayList<>();
      for (VirtualFilePointer file : xmlFiles) {
        oneKindOfConfigFiles.add(new ConfigFileNode(settings, fileSet, file, this));
      }
      for (VirtualFilePointer file2 : codeFiles) {
        oneKindOfConfigFiles.add(new ConfigFileNode(settings, fileSet, file2, this));
      }
      if (settings.isSortAlpha()) {
        oneKindOfConfigFiles.sort(FILENAME_COMPARATOR);
      }
      nodes.addAll(oneKindOfConfigFiles);
    }
    Set<VirtualFilePointer> propertiesFiles = fileSet.getPropertiesFiles();
    propertiesFiles.removeAll(allCustomGroupFiles);
    if (!propertiesFiles.isEmpty()) {
      nodes.add(new PropertiesGroupNode(this, propertiesFiles));
    }
    for (InfraFileSetEditorCustomization.CustomConfigFileGroup group2 : smartList) {
      nodes.add(new FilesetGroupNode(this, group2.getFiles()) {
        @Override
        protected String getGroupName() {
          return group2.getName();
        }

        @Override
        protected Icon getGroupNodeIcon() {
          return group2.getIcon();
        }
      });
    }
    Set<VirtualFilePointer> otherFiles = new HashSet<>(fileSet.getFiles());
    otherFiles.removeAll(xmlFiles);
    otherFiles.removeAll(codeFiles);
    otherFiles.removeAll(propertiesFiles);
    otherFiles.removeAll(allCustomGroupFiles);
    if (!otherFiles.isEmpty()) {
      nodes.add(new FilesetGroupNode(this, otherFiles) {
        @Override
        protected String getGroupName() {
          return "Other Files";
        }

        @Override
        protected Icon getGroupNodeIcon() {
          return AllIcons.FileTypes.Any_type;
        }
      });
    }
    return nodes.toArray(new SimpleNode[0]);
  }

  public boolean isAutoExpandNode() {
    return true;
  }

  protected void update(PresentationData presentation) {
    super.update(presentation);
    presentation.clearText();
    InfraFileSet fileSet = getFileSet();
    presentation.addText(fileSet.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    if (fileSet.isAutodetected()) {
      presentation.addText(" " + InfraBundle.message("facet.context.autodetected.suffix"), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
    presentation.setIcon(fileSet.getIcon());
  }

  private static class FilenameComparator implements Comparator<ConfigFileNode> {
    private FilenameComparator() {
    }

    @Override
    public int compare(ConfigFileNode o1, ConfigFileNode o2) {
      return StringUtil.naturalCompare(o1.getFilePointer().getFileName(), o2.getFilePointer().getFileName());
    }
  }
}
