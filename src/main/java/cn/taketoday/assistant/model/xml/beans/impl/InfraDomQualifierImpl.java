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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomUtil;

import java.util.List;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.QualifierAttribute;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraDomAttribute;
import cn.taketoday.assistant.model.xml.beans.InfraDomQualifier;
import cn.taketoday.lang.Nullable;

public abstract class InfraDomQualifierImpl implements InfraDomQualifier {

  @Override
  public PsiClass getQualifierType() {
    return getType().getValue();
  }

  @Override
  @Nullable
  public String getQualifierValue() {
    if (DomUtil.hasXml(getValue())) {
      return getValue().getValue();
    }
    return null;
  }

  @Override

  public List<? extends QualifierAttribute> getQualifierAttributes() {
    List<InfraDomAttribute> attributes = getAttributes();
    return attributes;
  }

  @Nullable
  public CommonInfraBean getQualifiedBean() {
    return getParentOfType(InfraBean.class, false);
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    XmlTag xmlTag = getXmlTag();
    return xmlTag;
  }
}
