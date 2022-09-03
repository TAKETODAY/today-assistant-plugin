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

package cn.taketoday.assistant.model.xml.beans.impl;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ComparatorUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.lang.Nullable;

public abstract class InfraPropertyImpl extends InfraInjectionImpl implements InfraProperty {

  @Override
  public PsiType guessTypeByValue() {
    if (DomUtil.hasXml(getValue())) {
      PsiType type = getValue().getType().getValue();
      return type != null ? type : getTypeByName(String.class.getCanonicalName());
    }
    else if (DomUtil.hasXml(getRefAttr())) {
      return getPointerType(getRefAttr().getValue());
    }
    else {
      if (DomUtil.hasXml(getBean())) {
        return getPointerType(InfraBeanService.of().createBeanPointer(getBean()));
      }
      if (DomUtil.hasXml(getRef())) {
        InfraRef infraRef = getRef();
        BeanPointer<?> beanPointer = infraRef.getBean().getValue();
        if (beanPointer != null) {
          return getPointerType(beanPointer);
        }
        BeanPointer<?> localPointer = infraRef.getLocal().getValue();
        return localPointer != null ? getPointerType(localPointer) : getPointerType(infraRef.getParentAttr().getValue());
      }
      else if (DomUtil.hasXml(getIdref())) {
        return getTypeByName(String.class.getCanonicalName());
      }
      else {
        if (DomUtil.hasXml(getList())) {
          return getTypeByName(List.class.getCanonicalName());
        }
        if (DomUtil.hasXml(getMap())) {
          return getTypeByName(Map.class.getCanonicalName());
        }
        if (DomUtil.hasXml(getSet())) {
          return getTypeByName(Set.class.getCanonicalName());
        }
        if (DomUtil.hasXml(getArray())) {
          PsiClass psiClass = getArray().getValueType().getValue();
          if (psiClass != null) {
            return PsiTypesUtil.getClassType(psiClass).createArrayType();
          }
          PsiType objectType = getTypeByName("java.lang.Object");
          if (objectType == null) {
            return null;
          }
          return objectType.createArrayType();
        }
        else if (DomUtil.hasXml(getProps())) {
          return getTypeByName(Properties.class.getCanonicalName());
        }
        else {
          if (DomUtil.hasXml(getNull())) {
            return PsiType.NULL;
          }
          return null;
        }
      }
    }
  }

  @Nullable
  private PsiType getTypeByName(String name) {
    GlobalSearchScope scope = GlobalSearchScope.allScope(getManager().getProject());
    PsiClass psiClass = JavaPsiFacade.getInstance(getManager().getProject()).findClass(name, scope);
    if (psiClass != null) {
      return PsiTypesUtil.getClassType(psiClass);
    }
    return null;
  }

  @Nullable
  private static PsiType getPointerType(@Nullable BeanPointer<?> pointer) {
    if (pointer == null) {
      return null;
    }
    PsiType[] psiTypes = pointer.getEffectiveBeanTypes();
    if (psiTypes.length != 0) {
      return psiTypes[0];
    }
    return null;
  }

  @Override

  public List<PsiType> getRequiredTypes() {
    DomInfraBean bean;
    PsiClass derivedClass;
    PsiClass superClass;
    List<BeanProperty> properties = getName().getValue();
    if (properties == null || properties.isEmpty()) {
      return Collections.emptyList();
    }
    SmartList smartList = new SmartList();
    for (BeanProperty property : properties) {
      PsiType psiType = property.getPropertyType();
      if ((psiType instanceof PsiClassReferenceType) && (bean = getParentOfType(DomInfraBean.class, false)) != null && (derivedClass = PsiTypesUtil.getPsiClass(
              bean.getBeanType())) != null && (superClass = PsiTreeUtil.getParentOfType(property.getMethod(), PsiClass.class)) != null && derivedClass.isInheritor(superClass, true)) {
        PsiSubstitutor superClassSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(superClass, derivedClass, PsiSubstitutor.EMPTY);
        psiType = superClassSubstitutor.substitute(psiType);
      }
      smartList.add(psiType);
    }
    return smartList;
  }

  @Override

  @Nullable
  public String getPropertyName() {
    return getName().getRawText();
  }

  public int hashCode() {
    String name = getPropertyName();
    if (name == null) {
      return 0;
    }
    return name.hashCode();
  }

  public boolean equals(Object obj) {
    return (obj instanceof InfraProperty) && ComparatorUtil.equalsNullable(getPropertyName(), ((InfraProperty) obj).getPropertyName());
  }
}
