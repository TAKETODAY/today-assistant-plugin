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
import com.intellij.ide.structureView.impl.xml.XmlTagTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.DomElementsNavigationManager;
import com.intellij.util.xml.DomFileElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class InfraModelTreeElement implements StructureViewTreeElement, ItemPresentation {
  private final XmlFile myXmlFile;
  private final DomElementNavigationProvider myNavigationProvider;
  private final boolean myShowBeanStructure;

  public InfraModelTreeElement(XmlFile xmlFile, boolean showBeanStructure) {
    this(xmlFile, DomElementsNavigationManager.getManager(xmlFile.getProject()).getDomElementsNavigateProvider(DomElementsNavigationManager.DEFAULT_PROVIDER_NAME), showBeanStructure);
  }

  public InfraModelTreeElement(XmlFile xmlFile, DomElementNavigationProvider navigationProvider, boolean showBeanStructure) {
    this.myXmlFile = xmlFile;
    this.myNavigationProvider = navigationProvider;
    this.myShowBeanStructure = showBeanStructure;
  }

  public Object getValue() {
    return this.myXmlFile;
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public TreeElement[] getChildren() {
    LocalXmlModel springModel = getSpringModel();
    if (springModel == null) {
      return EMPTY_ARRAY;
    }
    DomFileElement<Beans> fileElement = springModel.getRoot();
    if (fileElement == null) {
      return EMPTY_ARRAY;
    }
    List<StructureViewTreeElement> treeElements = new ArrayList<>();
    for (BeanPointer pointer : InfraModelVisitorUtils.getAllDomBeans(springModel)) {
      CommonInfraBean infraBean = pointer.getBean();
      if (pointer.isValid() && infraBean.isValid() && (infraBean instanceof DomInfraBean)) {
        if (infraBean instanceof CustomBeanWrapper customBeanWrapper) {
          if (customBeanWrapper.isDummy()) {
            treeElements.add(new XmlTagTreeElement(customBeanWrapper.getXmlTag()));
          }
        }
        treeElements.add(new InfraBeanTreeElement((DomInfraBean) infraBean, this.myNavigationProvider, this.myShowBeanStructure));
      }
    }
    return treeElements.toArray(TreeElement.EMPTY_ARRAY);
  }

  @Nullable
  private LocalXmlModel getSpringModel() {
    Module module = ModuleUtilCore.findModuleForFile(this.myXmlFile);
    if (module == null) {
      return null;
    }
    return LocalModelFactory.of().getOrCreateLocalXmlModel(this.myXmlFile, module, Collections.emptySet());
  }

  public void navigate(boolean requestFocus) {
  }

  public boolean canNavigate() {
    return false;
  }

  public boolean canNavigateToSource() {
    return false;
  }

  public String getPresentableText() {
    return InfraBundle.message("beans");
  }

  public Icon getIcon(boolean open) {
    return null;
  }
}
