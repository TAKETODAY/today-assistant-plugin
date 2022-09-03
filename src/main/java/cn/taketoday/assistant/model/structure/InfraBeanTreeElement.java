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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.Function;
import com.intellij.util.xml.DomElementNavigationProvider;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;

public class InfraBeanTreeElement implements StructureViewTreeElement, ItemPresentation {
  private final DomInfraBean myBean;
  private final DomElementNavigationProvider myNavigationProvider;
  private final boolean myShowBeanStructure;
  private static final Function<PsiType, String> CLASS_NAME_GETTER = type -> {
    return StringUtil.notNullize(type.getPresentableText());
  };

  public InfraBeanTreeElement(DomInfraBean infraBean, DomElementNavigationProvider navigationProvider, boolean showBeanStructure) {
    this.myBean = infraBean;
    this.myNavigationProvider = navigationProvider;
    this.myShowBeanStructure = showBeanStructure;
  }

  public Object getValue() {
    if (this.myBean.isValid()) {
      return this.myBean.getXmlElement();
    }
    return null;
  }

  public ItemPresentation getPresentation() {
    return this;
  }

  public String getPresentableText() {
    if (!DumbService.isDumb(this.myBean.getManager().getProject()) && this.myBean.isValid()) {
      String name = InfraPresentationProvider.getBeanName(this.myBean);
      PsiType[] psiTypes = getPsiTypes();
      String psiClassName = StringUtil.join(psiTypes, CLASS_NAME_GETTER, ",");
      if (!psiClassName.isEmpty()) {
        return name + ": " + psiClassName;
      }
      return name;
    }
    return "";
  }

  private PsiType[] getPsiTypes() {
    PsiType beanType = this.myBean.getBeanType();
    if (beanType == null) {
      return PsiType.EMPTY_ARRAY;
    }
    PsiClass psiClass = PsiTypesUtil.getPsiClass(beanType);
    if (psiClass != null && FactoryBeansManager.of().isFactoryBeanClass(psiClass)) {
      PsiType[] productTypes = FactoryBeansManager.of().getObjectTypes(beanType, this.myBean);
      if (productTypes.length > 0) {
        return productTypes;
      }
    }
    return new PsiType[] { beanType };
  }

  public Icon getIcon(boolean open) {
    return InfraPresentationProvider.getInfraIcon(this.myBean);
  }

  public TreeElement[] getChildren() {
    if (!this.myBean.isValid()) {
      return EMPTY_ARRAY;
    }
    else if ((this.myBean instanceof CustomBeanWrapper) && InfraPropertyUtils.getProperties(this.myBean).isEmpty() && ((CustomBeanWrapper) this.myBean).getCustomBeans().isEmpty()) {
      return new XmlTagTreeElement(this.myBean.getXmlTag()).getChildren();
    }
    else if (!this.myShowBeanStructure) {
      return EMPTY_ARRAY;
    }
    else {
      List<InfraInjectionTreeElement> children2 = new ArrayList<>();
      if (this.myBean instanceof InfraBean) {
        for (ConstructorArg arg : ((InfraBean) this.myBean).getConstructorArgs()) {
          children2.add(new InfraInjectionTreeElement(arg));
        }
      }
      for (InfraPropertyDefinition property : InfraPropertyUtils.getProperties(this.myBean)) {
        children2.add(new InfraInjectionTreeElement(property));
      }
      return children2.toArray(new InfraInjectionTreeElement[0]);
    }
  }

  public void navigate(boolean requestFocus) {
    if (this.myNavigationProvider != null) {
      this.myNavigationProvider.navigate(this.myBean, requestFocus);
    }
  }

  public boolean canNavigate() {
    return this.myBean.isValid() && this.myNavigationProvider != null && this.myNavigationProvider.canNavigate(this.myBean);
  }

  public boolean canNavigateToSource() {
    return this.myBean.isValid() && this.myNavigationProvider != null && this.myNavigationProvider.canNavigate(this.myBean);
  }
}
