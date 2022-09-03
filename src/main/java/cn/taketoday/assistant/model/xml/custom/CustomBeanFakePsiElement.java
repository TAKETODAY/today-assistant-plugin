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

package cn.taketoday.assistant.model.xml.custom;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.xml.CustomBeanPsiElement;

public class CustomBeanFakePsiElement extends RenameableFakePsiElement implements CustomBeanPsiElement {
  private final CustomNamespaceInfraBean myBean;

  public CustomBeanFakePsiElement(CustomNamespaceInfraBean bean) {
    super(bean.getContainingFile());
    this.myBean = bean;
  }

  public XmlTag getParent() {
    return this.myBean.getXmlTag();
  }

  public String getName() {
    return this.myBean.getBeanName();
  }

  @Override
  public CustomNamespaceInfraBean getBean() {
    return this.myBean;
  }

  public PsiElement getNavigationElement() {
    return getParent();
  }

  public String getTypeName() {
    return InfraBundle.message("infra.bean");
  }

  public PsiElement setName(String name) throws IncorrectOperationException {
    XmlAttribute idAttribute = this.myBean.getIdAttribute();
    if (idAttribute != null) {
      idAttribute.setValue(name);
    }
    return super.setName(name);
  }

  public boolean isEquivalentTo(PsiElement another) {
    if (another instanceof CustomBeanFakePsiElement element) {
      return element.myBean.equals(myBean);
    }
    return false;
  }

  public Icon getIcon() {
    return Icons.SpringBean;
  }
}
