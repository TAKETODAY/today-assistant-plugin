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

package cn.taketoday.assistant.model.xml.beans.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.xml.beans.CNamespaceDomElement;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinitionBase;
import cn.taketoday.lang.Nullable;

public abstract class CNamespaceValueBaseImpl extends InfraValueHolderDefinitionBase implements CNamespaceDomElement {

  @Override

  public List<PsiType> getRequiredTypes() {
    PsiParameter[] parameters;
    InfraBean bean = (InfraBean) getParent();
    Set<PsiType> psiTypes = new LinkedHashSet<>();
    List<PsiMethod> psiMethods = bean.getInstantiationMethods();
    for (PsiMethod psiMethod : psiMethods) {
      int idx = 0;
      for (PsiParameter psiParameter : psiMethod.getParameterList().getParameters()) {
        if (matchesByName(psiParameter) || matchesByIndex(idx)) {
          psiTypes.add(psiParameter.getType());
        }
        idx++;
      }
    }
    return new ArrayList(psiTypes);
  }

  @Override
  public boolean isIndexAttribute() {
    String attributeName = getAttributeName();
    return StringUtil.startsWithChar(attributeName, '_') && attributeName.length() >= 2;
  }

  @Override
  @Nullable
  public Integer getIndex() {
    String number = getAttributeName().substring(1);
    try {
      return Integer.parseInt(number);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  private boolean matchesByIndex(int matchIndex) {
    Integer myIndex;
    return isIndexAttribute() && (myIndex = getIndex()) != null && myIndex == matchIndex;
  }

  private boolean matchesByName(PsiParameter psiParameter) {
    return getAttributeName().equals(psiParameter.getName());
  }
}
