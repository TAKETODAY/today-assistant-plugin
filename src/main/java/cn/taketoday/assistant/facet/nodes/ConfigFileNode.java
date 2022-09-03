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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.Objects;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraConfigurationTabSettings;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class ConfigFileNode extends AbstractFilesetNode {
  private final VirtualFilePointer myFilePointer;

  public ConfigFileNode(InfraConfigurationTabSettings settings, InfraFileSet fileSet, VirtualFilePointer filePointer, SimpleNode parent) {
    super(fileSet, settings, parent);
    this.myFilePointer = filePointer;
  }

  protected void doUpdate() {
    VirtualFile file = this.myFilePointer.isValid() ? this.myFilePointer.getFile() : null;
    if (file == null) {
      renderError(InfraBundle.message("config.file.not.found"));
      return;
    }
    Project project = getFileSet().getFacet().getModule().getProject();
    if (DumbService.isDumb(project)) {
      renderFile();
      return;
    }
    PsiFile findFile = PsiManager.getInstance(project).findFile(file);
    if (findFile instanceof XmlFile xmlFile) {
      if (InfraDomUtils.isInfraXml(xmlFile)) {
        renderFile();
      }
      else {
        renderError(InfraBundle.message("config.file.is.not.infra"));
      }
    }
    else if (findFile instanceof PsiClassOwner) {
      PsiClass[] classes = ((PsiClassOwner) findFile).getClasses();
      boolean hasClass = classes.length > 0;
      boolean isMainClassConfig = hasClass && (InfraUtils.isConfigurationOrMeta(classes[0]) || InfraUtils.isComponentOrMeta(classes[0]));
      boolean isInnerClassConfig = isMainClassConfig
              || (hasClass && ContainerUtil.exists(classes[0].getInnerClasses(), InfraUtils::isConfigurationOrMeta));
      if (isMainClassConfig || isInnerClassConfig) {
        renderPsiClass(classes[0], SimpleTextAttributes.REGULAR_ATTRIBUTES, SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      else {
        renderError(InfraBundle.message("config.file.is.not.infra"));
      }
    }
    else {
      renderFile();
    }
    if (findFile != null) {
      setIcon(findFile.getIcon(0));
    }
  }

  private void renderError(String msg) {
    renderFile(SimpleTextAttributes.ERROR_ATTRIBUTES, SimpleTextAttributes.ERROR_ATTRIBUTES, msg);
  }

  private void renderFile() {
    renderFile(SimpleTextAttributes.REGULAR_ATTRIBUTES, SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
  }

  private void renderFile(SimpleTextAttributes main, SimpleTextAttributes full, @Nullable String toolTip) {
    PresentationData templatePresentation = getTemplatePresentation();
    templatePresentation.clearText();
    if (!this.myFilePointer.isValid()) {
      templatePresentation.addText(new ColoredFragment(toolTip, toolTip, main));
      return;
    }
    templatePresentation.addText(new ColoredFragment(this.myFilePointer.getFileName(), null, main));
    templatePresentation.addText(new ColoredFragment(" (" + getLocation() + ")", null, full));

    templatePresentation.setTooltip(toolTip);
  }

  private String getLocation() {
    VirtualFile file = this.myFilePointer.getFile();
    VirtualFile parent = file != null ? file.getParent() : null;
    if (parent == null) {
      return this.myFilePointer.getPresentableUrl();
    }
    Project project = getProject();
    assert project != null;
    VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) {
      return this.myFilePointer.getPresentableUrl();
    }
    String relativePath = VfsUtilCore.getRelativePath(file, baseDir);
    return ObjectUtils.notNull(relativePath, parent.getPresentableUrl());
  }

  private void renderPsiClass(PsiClass psiClass, SimpleTextAttributes main, SimpleTextAttributes full) {
    PresentationData templatePresentation = getTemplatePresentation();
    templatePresentation.clearText();

    templatePresentation.addText(new ColoredFragment(psiClass.getName(), null, main));
    templatePresentation.addText(new ColoredFragment(" (" + StringUtil.getPackageName(Objects.requireNonNull(psiClass.getQualifiedName())) + ")", null, full));
  }

  public SimpleNode[] getChildren() {
    return NO_CHILDREN;
  }

  public VirtualFilePointer getFilePointer() {
    return this.myFilePointer;
  }

  public boolean isAlwaysLeaf() {
    return true;
  }

  @Override
  public Object[] getEqualityObjects() {
    InfraFileSet set = getFileSet();
    return new Object[] { this.myFilePointer, set, set.getName(), set.getFiles(), set.getDependencyFileSets() };
  }
}
