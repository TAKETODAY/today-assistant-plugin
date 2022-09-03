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

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.pom.references.PomService;
import com.intellij.psi.DelegatePsiTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.PatchedWeakReference;
import com.intellij.util.xml.DomAnchor;
import com.intellij.util.xml.DomService;
import com.intellij.util.xml.impl.DomImplUtil;

import java.lang.ref.WeakReference;
import java.util.Objects;

import cn.taketoday.assistant.model.BaseBeanPointer;
import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

public class DomBeanPointerImpl extends BaseBeanPointer<DomInfraBean> implements DomBeanPointer {

  private final DomAnchor<DomInfraBean> myPointer;
  private CachedValue<PsiClass> myClassCachedValue;
  private WeakReference<DomInfraBean> myCachedValue;

  public DomBeanPointerImpl(DomInfraBean infraBean) {
    super(infraBean.getBeanName(), infraBean.getManager().getProject());
    ProgressManager.checkCanceled();
    this.myCachedValue = new PatchedWeakReference<>(infraBean);
    this.myPointer = DomService.getInstance().createAnchor(infraBean);
  }

  @Override

  public DomInfraBean getBean() {
    DomInfraBean bean = this.myCachedValue.get();
    if (bean != null) {
      return bean;
    }
    DomInfraBean bean2 = this.myPointer.retrieveDomElement();
    if (bean2 == null) {
      throw new IllegalStateException("no Dom at pointer: " + this.myPointer + " for " + getName());
    }
    XmlElement element = bean2.getXmlElement();
    if (element != null && !element.isValid()) {
      throw new PsiInvalidElementAccessException(element, "Invalid Dom element of " + bean2.getClass());
    }
    DomImplUtil.assertValidity(bean2, "Invalid retrieved bean");
    this.myCachedValue = new PatchedWeakReference<>(bean2);
    return bean2;
  }

  @Override
  public boolean isValid() {
    DomInfraBean bean = this.myCachedValue.get();
    if (bean != null) {
      return bean.isValid();
    }
    DomInfraBean bean2 = this.myPointer.retrieveDomElement();
    if (bean2 != null && bean2.isValid()) {
      this.myCachedValue = new PatchedWeakReference(bean2);
      return true;
    }
    return false;
  }

  public PsiElement getPsiElement() {
    return PomService.convertToPsi(new DomBeanPsiTarget(getBean()));
  }

  @Override
  public PsiClass getBeanClass() {
    if (this.myClassCachedValue == null) {
      this.myClassCachedValue = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
        PsiClass beanClass = PsiTypesUtil.getPsiClass(getBean().getBeanType());
        return CachedValueProvider.Result.createSingleDependency(beanClass, PsiModificationTracker.MODIFICATION_COUNT);
      });
    }
    return this.myClassCachedValue.getValue();
  }

  @Override
  public PsiFile getContainingFile() {
    return this.myPointer.getContainingFile();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DomBeanPointerImpl other)) {
      return false;
    }
    return Objects.equals(getName(), other.getName()) && this.myPointer.equals(other.myPointer) && isValid() && other.isValid() && Comparing.equal(getPsiElement(), other.getPsiElement());
  }

  @Override
  public int hashCode() {
    return this.myPointer.hashCode();
  }

  public static BeanPsiTarget createBeanPsiTarget(DomInfraBean bean) {
    return new DomBeanPsiTarget(bean);
  }

  public static final class DomBeanPsiTarget extends DelegatePsiTarget implements BeanPsiTarget {
    private final DomInfraBean mySpringBean;

    private DomBeanPsiTarget(DomInfraBean bean) {
      super(Objects.requireNonNull(bean.getXmlElement()));
      this.mySpringBean = bean;
    }

    public String toString() {
      return "DomBeanPsiTarget(" + this.mySpringBean + ")";
    }

    public boolean isWritable() {
      return true;
    }

    public Object setName(String newName) {
      this.mySpringBean.setName(newName);
      return this;
    }

    @Override
    public CommonInfraBean getInfraBean() {
      return this.mySpringBean;
    }

    @Nullable
    public String getName() {
      return this.mySpringBean.getBeanName();
    }
  }
}
