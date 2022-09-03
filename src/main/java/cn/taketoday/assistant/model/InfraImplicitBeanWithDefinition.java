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

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.pom.references.PomService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.PsiType;
import com.intellij.util.ArrayUtilRt;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.lang.Nullable;

/**
 * Used for beans created implicitly by other bean
 */
public class InfraImplicitBeanWithDefinition extends CommonModelElement.PsiBase
        implements CommonInfraBean {

  private final String myName;

  private final PsiClass myClass;

  private final CommonInfraBean myDefiningBean;

  private final PsiTarget myDefinitionTarget;

  /**
   * @param definingBean bean that implicitly creates this bean
   * @param definitionTarget target to psi element that is definition of this bean
   */
  public InfraImplicitBeanWithDefinition(String beanName,
          PsiClass beanClass,
          CommonInfraBean definingBean,
          PsiTarget definitionTarget) {
    myName = beanName;
    myClass = beanClass;
    myDefiningBean = definingBean;
    myDefinitionTarget = definitionTarget;
  }

  @Nullable
  @Override
  public String getBeanName() {
    return myName;
  }

  @Override
  public String[] getAliases() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Nullable
  @Override
  public PsiType getBeanType(boolean considerFactories) {
    return getBeanType();
  }

  @Nullable
  @Override
  public PsiType getBeanType() {
    return JavaPsiFacade.getElementFactory(myClass.getProject()).createType(myClass);
  }

  @Override
  public Collection<InfraQualifier> getInfraQualifiers() {
    return Collections.singleton(DefaultInfraBeanQualifier.create(this));
  }

  @Override
  public InfraProfile getProfile() {
    return myDefiningBean.getProfile();
  }

  @Override
  public boolean isPrimary() {
    return false;
  }

  @Override
  public PsiElement getPsiElement() {
    return myDefinitionTarget.getNavigationElement();
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return PomService.convertToPsi(myDefiningBean.getPsiManager().getProject(), myDefinitionTarget);
  }
}
