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

package cn.taketoday.assistant.dom.namespaces;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

class PNamespaceDescriptor extends AbstractBeanNamespaceDescriptor<PsiMethod> {

  @Override
  protected Map<String, PsiMethod> getAttributes(InfraBean springBean) {
    PsiClass beanClass = ((AbstractDomInfraBean) springBean).getBeanClass(new HashSet<>(), true);
    return beanClass == null ? Collections.emptyMap() : PropertyUtilBase.getAllProperties(beanClass, true, false);
  }

  @Override
  public PsiType getAttributeType(PsiMethod psiMethod) {
    return psiMethod.getParameterList().getParameters()[0].getType();
  }

  @Override
  public BeanAttributeDescriptor createAttributeDescriptor(String attrName, PsiMethod psiMethod, String suffix) {
    return new PAttributeDescriptor(attrName, suffix, psiMethod, getNamespace());
  }

  @Override
  protected String getNamespace() {
    return InfraConstant.P_NAMESPACE;
  }
}
