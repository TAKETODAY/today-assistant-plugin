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

import com.intellij.util.Processor;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;

import java.util.Set;

import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraKey;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class InfraModelVisitor {

  private final Processor<CommonInfraBean> myBeanProcessor = bean -> visitBean(this, bean);

  @Nullable
  protected Set<String> getActiveProfiles() {
    return null;
  }

  /**
   * Visits a bean.
   *
   * @param bean bean to be visited.
   * @return false to stop traversing.
   */
  protected boolean visitBean(CommonInfraBean bean) {
    return true;
  }

  protected boolean visitProperty(InfraPropertyDefinition property) {
    return true;
  }

  protected boolean visitConstructorArg(ConstructorArg arg) {
    return true;
  }

  protected boolean visitValueHolder(InfraValueHolder valueHolder) {
    return true;
  }

  protected boolean visitMapEntry(InfraEntry entry) {
    return true;
  }

  protected boolean visitRef(InfraRef ref) {
    return true;
  }

  protected boolean visitIdref(Idref idref) {
    return true;
  }

  private boolean visitChildrenBeans(DomElement parent) {
    if (!DomUtil.hasXml(parent))
      return true;

    return InfraBeanUtils.of().processChildBeans(parent, true, myBeanProcessor);
  }

  public static boolean visitBeans(InfraModelVisitor visitor, Beans beans) {
    if (ProfileUtils.isActiveProfile(beans, visitor.getActiveProfiles())) {
      if (!visitor.visitChildrenBeans(beans))
        return false;

      for (Beans beansProfiles : beans.getBeansProfiles()) {
        if (!visitBeans(visitor, beansProfiles))
          return false;
      }
    }
    return true;
  }

  public static boolean visitBean(InfraModelVisitor visitor, CommonInfraBean bean) {
    return visitBean(visitor, bean, true);
  }

  public static boolean visitBean(InfraModelVisitor visitor, CommonInfraBean bean, boolean deep) {
    if (bean instanceof DomInfraBean && !DomUtil.hasXml(((DomInfraBean) bean)))
      return true;
    if (!visitor.visitBean(bean))
      return false;

    if (deep) {
      for (InfraPropertyDefinition property : InfraPropertyUtils.getProperties(bean)) {
        if (!visitor.visitProperty(property))
          return false;
        if (property instanceof InfraValueHolder && !visitValueHolder(visitor, (InfraValueHolder) property))
          return false;
      }
      if (bean instanceof InfraBean) {
        for (ConstructorArg arg : ((InfraBean) bean).getConstructorArgs()) {
          if (!visitor.visitConstructorArg(arg))
            return false;
          if (!visitValueHolder(visitor, arg))
            return false;
        }
      }
      if (bean instanceof ListOrSet) {
        if (!visitCollection(visitor, (ListOrSet) bean))
          return false;
      }
      if (bean instanceof InfraMap) {
        if (!visitMap(visitor, (InfraMap) bean))
          return false;
      }
    }
    return true;
  }

  private static boolean visitValueHolder(InfraModelVisitor visitor, InfraValueHolder elementsHolder) {
    if (!DomUtil.hasXml(elementsHolder))
      return true;

    return visitor.visitValueHolder(elementsHolder) &&
            visitElementsHolder(visitor, elementsHolder);
  }

  private static boolean visitElementsHolder(final InfraModelVisitor visitor, final InfraElementsHolder elementsHolder) {
    if (!visitor.visitChildrenBeans(elementsHolder))
      return false;

    final InfraRef ref = elementsHolder.getRef();
    if (DomUtil.hasXml(ref) && !visitor.visitRef(ref))
      return false;
    final Idref idref = elementsHolder.getIdref();
    if (DomUtil.hasXml(idref) && !visitor.visitIdref(idref))
      return false;
    if (!visitCollection(visitor, elementsHolder.getList()))
      return false;
    if (!visitCollection(visitor, elementsHolder.getSet()))
      return false;
    if (!visitCollection(visitor, elementsHolder.getArray()))
      return false;
    if (!visitMap(visitor, elementsHolder.getMap()))
      return false;
    return true;
  }

  private static boolean visitCollection(final InfraModelVisitor visitor,
          ListOrSet collection) {
    if (!visitor.visitChildrenBeans(collection))
      return false;

    for (ListOrSet listOrSet : collection.getSets()) {
      if (!visitCollection(visitor, listOrSet))
        return false;
    }
    for (ListOrSet listOrSet : collection.getArrays()) {
      if (!visitCollection(visitor, listOrSet))
        return false;
    }
    for (ListOrSet listOrSet : collection.getLists()) {
      if (!visitCollection(visitor, listOrSet))
        return false;
    }
    for (InfraMap map : collection.getMaps()) {
      if (!visitMap(visitor, map))
        return false;
    }
    for (InfraRef ref : collection.getRefs()) {
      if (!visitor.visitRef(ref))
        return false;
    }
    for (Idref idref : collection.getIdrefs()) {
      if (!visitor.visitIdref(idref))
        return false;
    }
    return true;
  }

  private static boolean visitMap(InfraModelVisitor visitor, InfraMap map) {
    if (!DomUtil.hasXml(map))
      return true;
    for (InfraEntry entry : map.getEntries()) {
      if (!visitor.visitMapEntry(entry))
        return false;
      if (!visitValueHolder(visitor, entry))
        return false;
      final InfraKey key = entry.getKey();
      if (DomUtil.hasXml(key) && !visitElementsHolder(visitor, key))
        return false;
    }
    return true;
  }
}
