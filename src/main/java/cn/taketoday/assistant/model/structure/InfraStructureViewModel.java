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

package cn.taketoday.assistant.model.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.xml.XmlStructureViewTreeModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.DomElementsNavigationManager;

import cn.taketoday.lang.Nullable;

public class InfraStructureViewModel extends XmlStructureViewTreeModel implements Disposable {
  private final InfraModelTreeElement myRoot;

  public InfraStructureViewModel(XmlFile xmlFile, @Nullable Editor editor) {
    this(xmlFile, DomElementsNavigationManager.getManager(xmlFile.getProject()).getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME), false, editor);
  }

  public InfraStructureViewModel(XmlFile xmlFile, DomElementNavigationProvider navigationProvider, boolean showBeanStructure, @Nullable Editor editor) {
    super(xmlFile, editor);
    this.myRoot = new InfraModelTreeElement(getPsiFile(), navigationProvider, showBeanStructure);
  }

  public StructureViewTreeElement getRoot() {
    return this.myRoot;
  }
}
