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

package cn.taketoday.assistant.model.xml;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTypesUtil;

import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.model.BaseBeanPointer;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;

public final class CustomBeanPointer extends BaseBeanPointer {
  private final BeanPointer<?> myBasePointer;
  private final int myIndex;

  private CustomBeanPointer(cn.taketoday.assistant.model.xml.CustomBeanWrapper wrapper, cn.taketoday.assistant.model.xml.CustomBean bean, int index) {
    super(bean.getBeanName(), wrapper.getManager().getProject());
    this.myIndex = index;
    this.myBasePointer = InfraBeanService.of().createBeanPointer(wrapper);
  }

  @Override

  public cn.taketoday.assistant.model.xml.CustomBean getBean() {
    cn.taketoday.assistant.model.xml.CustomBean customBean = ((cn.taketoday.assistant.model.xml.CustomBeanWrapper) this.myBasePointer.getBean()).getCustomBeans().get(this.myIndex);
    return customBean;
  }

  @Override
  public boolean isValid() {
    if (!this.myBasePointer.isValid()) {
      return false;
    }
    DomInfraBean baseBean = (DomInfraBean) this.myBasePointer.getBean();
    if (!(baseBean instanceof CustomBeanWrapper)) {
      return false;
    }
    List<CustomBean> beans = ((CustomBeanWrapper) baseBean).getCustomBeans();
    return beans.size() > this.myIndex;
  }

  public static CustomBeanPointer createCustomBeanPointer(CustomBean bean) {
    CustomBeanWrapper wrapper = bean.getWrapper();
    List<CustomBean> allBeans = wrapper.getCustomBeans();
    int index = allBeans.indexOf(bean);
    if (index < 0) {
      throw new AssertionError("Can't find custom bean " + bean + " among " + allBeans);
    }
    return new CustomBeanPointer(wrapper, bean, index);
  }

  public PsiElement getPsiElement() {
    return getBean().getIdentifyingPsiElement();
  }

  @Override
  public PsiClass getBeanClass() {
    return PsiTypesUtil.getPsiClass(getBean().getBeanType());
  }

  @Override

  public PsiFile getContainingFile() {
    return this.myBasePointer.getContainingFile();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CustomBeanPointer that) || !super.equals(o)) {
      return false;
    }
    if (this.myIndex != that.myIndex) {
      return false;
    }
    return Objects.equals(this.myBasePointer, that.myBasePointer);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    return (31 * ((31 * result) + (this.myBasePointer != null ? this.myBasePointer.hashCode() : 0))) + this.myIndex;
  }
}
