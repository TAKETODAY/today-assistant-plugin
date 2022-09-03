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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;

import cn.taketoday.lang.Nullable;

public final class DerivedBeanPointer implements BeanPointer<CommonInfraBean> {
  private final String name;
  private final BeanPointer<?> basePointer;

  public DerivedBeanPointer(BeanPointer<?> basePointer, String name) {
    this.basePointer = basePointer;
    this.name = name;
  }

  @Override
  public BeanPointer<?> derive(String name) {
    if (name.equals(this.name)) {
      return this;
    }

    if (name.equals(basePointer.getName())) {
      return this.basePointer;
    }
    return new DerivedBeanPointer(this.basePointer, name);
  }

  @Override
  public String[] getAliases() {
    return this.basePointer.getAliases();
  }

  @Override
  public boolean isReferenceTo(@Nullable CommonInfraBean springBean) {
    return this.basePointer.isReferenceTo(springBean);
  }

  @Override
  public BeanPointer<?> getBasePointer() {
    return this.basePointer;
  }

  @Override
  public boolean isValid() {
    return this.basePointer.isValid();
  }

  @Override
  @Nullable
  public PsiClass getBeanClass() {
    return this.basePointer.getBeanClass();
  }

  @Override

  public PsiFile getContainingFile() {
    return this.basePointer.getContainingFile();
  }

  @Override
  public PsiType[] getEffectiveBeanTypes() {
    return this.basePointer.getEffectiveBeanTypes();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @Nullable
  public BeanPointer<?> getParentPointer() {
    return this.basePointer.getParentPointer();
  }

  @Nullable
  public PsiElement getPsiElement() {
    return this.basePointer.getPsiElement();
  }

  @Override

  public CommonInfraBean getBean() {
    return basePointer.getBean();
  }

  @Override
  public boolean isAbstract() {
    return this.basePointer.isAbstract();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DerivedBeanPointer that)) {
      return false;
    }
    return this.basePointer.equals(that.basePointer) && this.name.equals(that.name);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    return (31 * result) + this.basePointer.hashCode();
  }
}
