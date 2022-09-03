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

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;

import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.lang.Nullable;

public final class DelegateConditionalBeanPointer implements JamBeanPointer, ConditionalEvaluator {

  private final JamBeanPointer myDelegate;
  private final Condition<cn.taketoday.assistant.model.ConditionalEvaluationContext> myCondition;

  public static DelegateConditionalBeanPointer createPointer(JamBeanPointer delegate,
          Condition<cn.taketoday.assistant.model.ConditionalEvaluationContext> condition) {
    return new DelegateConditionalBeanPointer(delegate, condition);
  }

  private DelegateConditionalBeanPointer(JamBeanPointer delegate,
          Condition<cn.taketoday.assistant.model.ConditionalEvaluationContext> condition) {
    myDelegate = delegate;
    myCondition = condition;
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return myDelegate.getPsiElement();
  }

  @Override
  @Nullable
  public String getName() {
    return myDelegate.getName();
  }

  @Override
  public String[] getAliases() {
    return myDelegate.getAliases();
  }

  @Override
  public JamPsiMemberInfraBean<?> getBean() {
    return myDelegate.getBean();
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid();
  }

  @Override
  public boolean isReferenceTo(@Nullable CommonInfraBean infraBean) {
    return myDelegate.isReferenceTo(infraBean);
  }

  @Override
  public BeanPointer<?> derive(String name) {
    return myDelegate.derive(name);
  }

  @Override
  public BeanPointer<?> getBasePointer() {
    return myDelegate.getBasePointer();
  }

  @Override
  @Nullable
  public PsiClass getBeanClass() {
    return myDelegate.getBeanClass();
  }

  @Override
  public PsiType[] getEffectiveBeanTypes() {
    return myDelegate.getEffectiveBeanTypes();
  }

  @Override
  public PsiFile getContainingFile() {
    return myDelegate.getContainingFile();
  }

  @Override
  public boolean isAbstract() {
    return myDelegate.isAbstract();
  }

  @Override
  @Nullable
  public BeanPointer<?> getParentPointer() {
    return myDelegate.getParentPointer();
  }

  @Override
  public boolean isActive(ConditionalEvaluationContext context) {
    return myDelegate.isValid() && myCondition.value(context);
  }

  @Override
  public int hashCode() {
    return myDelegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DelegateConditionalBeanPointer))
      return false;

    return myDelegate.equals(((DelegateConditionalBeanPointer) o).myDelegate);
  }
}
