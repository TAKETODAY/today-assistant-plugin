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

package cn.taketoday.assistant.model.converters;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.properties.PropertyReference;
import cn.taketoday.assistant.model.properties.PropertyReferenceSet;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class BeanPropertyConverterImpl extends BeanPropertyConverter {

  public List<BeanProperty> fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    else {
      GenericAttributeValue<List<BeanProperty>> value = (GenericAttributeValue<List<BeanProperty>>) context.getInvocationElement();
      PropertyReference[] references = this.createReferences(value, value.getXmlAttributeValue(), context);
      if (references.length == 0) {
        return Collections.emptyList();
      }
      else {
        ResolveResult[] results = references[references.length - 1].multiResolve(false);
        List<BeanProperty> beanProperties = new ArrayList<>(results.length);
        int var8 = results.length;
        for (ResolveResult result : results) {
          PsiMethod method = (PsiMethod) result.getElement();
          if (method != null) {
            BeanProperty beanProperty = BeanProperty.createBeanProperty(method);
            ContainerUtil.addIfNotNull(beanProperties, beanProperty);
          }
        }

        return beanProperties;
      }
    }
  }

  public String toString(@Nullable List<BeanProperty> beanProperty, ConvertContext context) {
    return null;
  }

  public PropertyReference[] createReferences(GenericDomValue<List<BeanProperty>> genericDomValue, PsiElement element, ConvertContext context) {
    if (element == null) {
      return new PropertyReference[0];
    }
    CommonInfraBean infraBean = InfraConverterUtil.getCurrentBeanCustomAware(context);
    assert infraBean != null;
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (isAbstractBeanTemplate(infraBean, beanClass, context)) {
      return new PropertyReference[0];
    }
    return new PropertyReferenceSet(element, beanClass, genericDomValue, context, infraBean).m402getPsiReferences();
  }

  public boolean isAbstractBeanTemplate(CommonInfraBean infraBean, @Nullable PsiClass beanClass, ConvertContext context) {
    InfraModel model;
    boolean isAbstractBeanWithNoClass = beanClass == null && (infraBean instanceof InfraBean) && ((InfraBean) infraBean).isAbstract();
    return isAbstractBeanWithNoClass && (model = InfraConverterUtil.getInfraModel(context)) != null && InfraModelVisitorUtils.getDescendants(model,
            InfraBeanService.of().createBeanPointer(infraBean)).size() == 1;
  }
}
