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

package cn.taketoday.assistant.model.jam;

import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTypesUtil;

import java.util.Objects;

import cn.taketoday.assistant.model.BaseBeanPointer;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.lang.Nullable;

public final class JamBeanPointerImpl extends BaseBeanPointer<JamPsiMemberInfraBean> implements JamBeanPointer {

  private final JamPsiMemberInfraBean mySpringBean;

  public JamBeanPointerImpl(JamPsiMemberInfraBean springBean) {
    super(null, springBean.getPsiElement().getProject());
    this.mySpringBean = springBean;
  }

  @Override
  @Nullable
  public String getName() {
    return this.mySpringBean.getBeanName();
  }

  @Override
  public JamPsiMemberInfraBean getBean() {
    JamPsiMemberInfraBean jamPsiMemberSpringBean = this.mySpringBean;
    return jamPsiMemberSpringBean;
  }

  @Nullable
  public PsiElement getPsiElement() {
    JamPsiMemberInfraBean springBean = getBean();
    if (springBean.isValid()) {
      return springBean.getIdentifyingPsiElement();
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JamBeanPointerImpl)) {
      return false;
    }
    BeanPointer<?> that = (BeanPointer) o;
    return Objects.equals(getName(), that.getName()) && Comparing.equal(getBean(), that.getBean());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    return (31 * result) + this.mySpringBean.hashCode();
  }

  @Override
  public String toString() {
    return "JamSpringBeanPointerImpl{mySpringBean=" + this.mySpringBean + ", name = " + getName() + "}";
  }

  @Override
  @Nullable
  public PsiClass getBeanClass() {
    if (isValid()) {
      return PsiTypesUtil.getPsiClass(getBean().getBeanType());
    }
    return null;
  }

  public PsiManager getPsiManager() {
    return getBean().getPsiManager();
  }

  @Override
  public PsiFile getContainingFile() {
    return getBean().getContainingFile();
  }

  @Override
  public boolean isValid() {
    return getBean().isValid();
  }
}
