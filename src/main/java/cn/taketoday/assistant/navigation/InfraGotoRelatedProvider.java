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

package cn.taketoday.assistant.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;

public class InfraGotoRelatedProvider extends GotoRelatedProvider {

  public List<? extends GotoRelatedItem> getItems(PsiElement context) {
    List<BeanProperty> properties;
    XmlTag tag = PsiTreeUtil.getParentOfType(context, XmlTag.class, false);
    if (tag == null) {
      return Collections.emptyList();
    }
    DomElement element = DomManager.getDomManager(tag.getProject()).getDomElement(tag);
    if (element == null) {
      return Collections.emptyList();
    }
    if (element instanceof InfraBean) {
      PsiClass psiClass = PsiTypesUtil.getPsiClass(((InfraBean) element).getBeanType());
      if (psiClass != null) {
        return Collections.singletonList(new GotoRelatedItem(psiClass));
      }
    }
    else if ((element instanceof InfraProperty) && (properties = ((InfraProperty) element).getName().getValue()) != null) {
      return ContainerUtil.map(properties,
              beanProperty -> new GotoRelatedItem(beanProperty.getMethod()));
    }
    return Collections.emptyList();
  }
}
