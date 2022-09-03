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
package cn.taketoday.assistant.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlTag;

import java.util.Collections;
import java.util.List;

public final class DefaultInfraBeanQualifier implements InfraQualifier {

  public static DefaultInfraBeanQualifier create(CommonInfraBean bean) {
    return new DefaultInfraBeanQualifier(bean);
  }

  private final CommonInfraBean myBean;

  private DefaultInfraBeanQualifier(CommonInfraBean bean) {
    myBean = bean;
  }

  @Override
  public boolean isValid() {
    return myBean.isValid();
  }

  @Override
  public XmlTag getXmlTag() {
    return null;
  }

  @Override
  public PsiManager getPsiManager() {
    return myBean.getPsiManager();
  }

  @Override
  public Module getModule() {
    return myBean.getModule();
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    final PsiElement psiElement = myBean.getIdentifyingPsiElement();
    assert psiElement != null : myBean;
    return psiElement;
  }

  @Override
  public PsiFile getContainingFile() {
    return myBean.getContainingFile();
  }

  @Override
  public PsiClass getQualifierType() {
    return null;
  }

  @Override
  public String getQualifierValue() {
    return myBean.getBeanName();
  }

  @Override
  public List<? extends QualifierAttribute> getQualifierAttributes() {
    return Collections.emptyList();
  }
}
