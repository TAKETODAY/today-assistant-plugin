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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.lang.Nullable;

public final class InfraAbstractBeanReferencesInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean infraBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel infraModel) {
    for (InfraValueHolderDefinition property : InfraPropertyUtils.getValueHolders(infraBean)) {
      checkAbstractBeanReferences(property, holder);
    }
  }

  private static void checkAbstractBeanReferences(InfraValueHolderDefinition definition, DomElementAnnotationHolder holder) {
    BeanPointer<?> ref;
    GenericDomValue<BeanPointer<?>> refElement = definition.getRefElement();
    if (refElement != null && (ref = refElement.getValue()) != null) {
      checkNotAbstract(refElement, ref, holder);
    }
    else if (definition instanceof InfraValueHolder springInjection) {
      checkSpringRefBeans(springInjection.getRef(), holder);
      if (DomUtil.hasXml(springInjection.getBean())) {
        InfraBean innerBean = springInjection.getBean();
        checkNotAbstract(innerBean, InfraBeanService.of().createBeanPointer(innerBean), holder);
      }
      checkIdrefBeans(springInjection.getIdref(), holder);
      if (DomUtil.hasXml(springInjection.getList())) {
        checkCollectionReferences(springInjection.getList(), holder);
      }
      if (DomUtil.hasXml(springInjection.getSet())) {
        checkCollectionReferences(springInjection.getSet(), holder);
      }
      if (DomUtil.hasXml(springInjection.getArray())) {
        checkCollectionReferences(springInjection.getArray(), holder);
      }
      if (DomUtil.hasXml(springInjection.getMap())) {
        checkMapReferences(springInjection.getMap(), holder);
      }
    }
  }

  private static void checkNotAbstract(DomElement annotated, @Nullable BeanPointer<?> infraBean, DomElementAnnotationHolder holder) {
    if (infraBean != null && infraBean.isAbstract()) {
      holder.createProblem(annotated, InfraBundle.message("bean.referenced.by.abstract.bean"));
    }
  }

  private static void checkMapReferences(InfraMap map, DomElementAnnotationHolder beans) {
    for (InfraEntry entry : map.getEntries()) {
      checkAbstractBeanReferences(entry, beans);
    }
  }

  private static void checkIdrefBeans(Idref idref, DomElementAnnotationHolder holder) {
    if (!DomUtil.hasXml(idref)) {
      return;
    }
    GenericAttributeValue<BeanPointer<?>> local = idref.getLocal();
    if (DomUtil.hasXml(local)) {
      checkNotAbstract(local, local.getValue(), holder);
    }
    GenericAttributeValue<BeanPointer<?>> bean = idref.getBean();
    if (DomUtil.hasXml(bean)) {
      checkNotAbstract(bean, bean.getValue(), holder);
    }
  }

  private static void checkSpringRefBeans(InfraRef infraRef, DomElementAnnotationHolder holder) {
    if (DomUtil.hasXml(infraRef)) {
      GenericAttributeValue<BeanPointer<?>> bean = infraRef.getBean();
      if (DomUtil.hasXml(bean)) {
        checkNotAbstract(bean, bean.getValue(), holder);
      }
      GenericAttributeValue<BeanPointer<?>> local = infraRef.getLocal();
      if (DomUtil.hasXml(local)) {
        checkNotAbstract(local, local.getValue(), holder);
      }
    }
  }

  private static void checkCollectionReferences(CollectionElements elements, DomElementAnnotationHolder holder) {
    for (InfraRef infraRef : elements.getRefs()) {
      checkSpringRefBeans(infraRef, holder);
    }
    for (Idref idref : elements.getIdrefs()) {
      checkIdrefBeans(idref, holder);
    }
    for (ListOrSet listOrSet : elements.getLists()) {
      checkCollectionReferences(listOrSet, holder);
    }
    for (ListOrSet listOrSet2 : elements.getSets()) {
      checkCollectionReferences(listOrSet2, holder);
    }
    for (ListOrSet listOrSet3 : elements.getArrays()) {
      checkCollectionReferences(listOrSet3, holder);
    }
    for (InfraBean innerBean : elements.getBeans()) {
      checkNotAbstract(innerBean, InfraBeanService.of().createBeanPointer(innerBean), holder);
    }
    for (InfraMap map : elements.getMaps()) {
      checkMapReferences(map, holder);
    }
  }
}
