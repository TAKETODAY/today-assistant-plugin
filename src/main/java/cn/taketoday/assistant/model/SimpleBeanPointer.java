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
import com.intellij.psi.util.PsiTypesUtil;

import cn.taketoday.lang.Nullable;

public final class SimpleBeanPointer<T extends CommonInfraBean> extends BaseBeanPointer<T> {

  private final T infraBean;

  public SimpleBeanPointer(T infraBean) {
    super(infraBean.getBeanName(), infraBean.getPsiManager().getProject());
    this.infraBean = infraBean;
  }

  @Override
  public T getBean() {
    return infraBean;
  }

  @Nullable
  public PsiElement getPsiElement() {
    T infraBean = this.infraBean;
    if (infraBean.isValid()) {
      return infraBean.getIdentifyingPsiElement();
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || ((o instanceof SimpleBeanPointer) && this.infraBean.equals(((SimpleBeanPointer) o).infraBean));
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    return (31 * result) + this.infraBean.hashCode();
  }

  @Override
  @Nullable
  public PsiClass getBeanClass() {
    if (isValid()) {
      return PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    }
    return null;
  }

  @Override
  public PsiFile getContainingFile() {
    return infraBean.getContainingFile();
  }

  @Override
  public boolean isValid() {
    return infraBean.isValid();
  }
}
