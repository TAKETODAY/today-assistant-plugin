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

package cn.taketoday.assistant.model.properties;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.ReferenceSetBase;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

public class PropertyReferenceSet extends ReferenceSetBase<cn.taketoday.assistant.model.properties.PropertyReference> {
  @Nullable
  private final PsiClass myBeanClass;
  private final GenericDomValue<List<BeanProperty>> myGenericDomValue;
  private final ConvertContext myContext;
  private final CommonInfraBean myBean;

  public PropertyReferenceSet(PsiElement element, @Nullable PsiClass beanClass, GenericDomValue<List<BeanProperty>> genericDomValue, ConvertContext context, CommonInfraBean bean) {
    super(element);
    this.myBeanClass = beanClass;
    this.myGenericDomValue = genericDomValue;
    this.myContext = context;
    this.myBean = bean;
  }

  public PropertyReference m403createReference(TextRange range, int index) {
    return new PropertyReference(this, range, index);
  }

  public PropertyReference[] m402getPsiReferences() {
    return getReferences().toArray(new PropertyReference[0]);
  }

  public GenericDomValue<List<BeanProperty>> getGenericDomValue() {
    return this.myGenericDomValue;
  }

  @Nullable
  public PsiClass getBeanClass() {
    return this.myBeanClass;
  }

  public ConvertContext getContext() {
    return this.myContext;
  }

  public CommonInfraBean getBean() {
    return this.myBean;
  }
}
