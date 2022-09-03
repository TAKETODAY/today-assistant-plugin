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

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.xml.beans.InfraBean;

class CNamespaceDescriptor extends AbstractBeanNamespaceDescriptor<PsiParameter> {

  @Override
  protected Map<String, PsiParameter> getAttributes(InfraBean springBean) {
    return getBeanConstructorParams(springBean);
  }

  private static Map<String, PsiParameter> getBeanConstructorParams(InfraBean springBean) {
    Map<String, PsiParameter> map = new HashMap<>();
    for (PsiMethod constructor : springBean.getInstantiationMethods()) {
      int idx = 0;
      for (PsiParameter psiParameter : constructor.getParameterList().getParameters()) {
        map.put(psiParameter.getName(), psiParameter);
        map.put("_" + idx, psiParameter);
        idx++;
      }
    }
    return map;
  }

  @Override
  public PsiType getAttributeType(PsiParameter psiParameter) {
    return psiParameter.getType();
  }

  @Override
  public BeanAttributeDescriptor createAttributeDescriptor(String attrName, PsiParameter psiParameter, String suffix) {
    return new CAttributeDescriptor(attrName, suffix, psiParameter, getNamespace());
  }

  @Override
  protected String getNamespace() {
    return InfraConstant.C_NAMESPACE;
  }
}
