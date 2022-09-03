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

import com.intellij.openapi.util.Ref;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public final class BeanDomPointerImpl extends DomBeanPointerImpl {
  private volatile Boolean myAbstract;
  private volatile Ref<BeanPointer<?>> myParent;

  public BeanDomPointerImpl(InfraBean infraBean) {
    super(infraBean);
  }

  @Override
  public boolean isAbstract() {
    Boolean value = this.myAbstract;
    if (value == null) {
      boolean valueOf = calcAbstract();
      value = valueOf;
      this.myAbstract = valueOf;
    }
    return value;
  }

  private boolean calcAbstract() {
    return ((InfraBean) getBean()).isAbstract();
  }

  @Override
  @Nullable
  public BeanPointer<?> getParentPointer() {
    Ref<BeanPointer<?>> ref = this.myParent;
    if (ref == null) {
      Ref<BeanPointer<?>> create = Ref.create(calcParentPointer());
      ref = create;
      this.myParent = create;
    }
    return ref.get();
  }

  private BeanPointer<?> calcParentPointer() {
    GenericAttributeValue<BeanPointer<?>> parentAttribute = ((InfraBean) getBean()).getParentBean();
    if (DomUtil.hasXml(parentAttribute)) {
      return parentAttribute.getValue();
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof BeanDomPointerImpl) {
      return super.equals(o);
    }
    return false;
  }
}
