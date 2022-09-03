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

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.impl.ConvertContextFactory;
import com.intellij.util.xml.impl.DomManagerImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.converters.ConstructorArgIndexConverterImpl;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.values.InfraValueConvertersRegistry;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraRef;

public abstract class ConstructorArgImpl extends InfraInjectionImpl implements ConstructorArg {

  @Override
  public boolean isAssignable(PsiType requiredType) {
    PsiType type;
    if (DomUtil.hasXml(getType()) && (type = getType().getValue()) != null) {
      return requiredType.isAssignableFrom(type);
    }
    if (DomUtil.hasXml(getValueAttr())) {
      return isAssignableFrom(requiredType, getValueAttr());
    }
    if (DomUtil.hasXml(getRefAttr())) {
      return isAssignableFrom(requiredType, getRefAttr().getValue());
    }
    if (DomUtil.hasXml(getValue())) {
      PsiType type2 = getValue().getType().getValue();
      if (type2 != null) {
        return requiredType.isAssignableFrom(type2);
      }
      return isAssignableFrom(requiredType, getValue());
    }
    else if (DomUtil.hasXml(getBean())) {
      return isAssignableFrom(requiredType, InfraBeanService.of().createBeanPointer(getBean()));
    }
    else {
      if (DomUtil.hasXml(getRef())) {
        InfraRef infraRef = getRef();
        BeanPointer<?> beanPointer = infraRef.getBean().getValue();
        if (beanPointer != null) {
          return isAssignableFrom(requiredType, beanPointer);
        }
        BeanPointer<?> localPointer = infraRef.getLocal().getValue();
        if (localPointer != null) {
          return isAssignableFrom(requiredType, localPointer);
        }
        BeanPointer<?> parentPointer = infraRef.getParentAttr().getValue();
        if (parentPointer != null) {
          return isAssignableFrom(requiredType, parentPointer);
        }
        return false;
      }
      else if (DomUtil.hasXml(getIdref())) {
        return isAssignableFrom(requiredType, String.class);
      }
      else {
        if (DomUtil.hasXml(getList())) {
          return (requiredType instanceof PsiEllipsisType) || isAssignableFrom(requiredType, List.class);
        }
        else if (DomUtil.hasXml(getMap())) {
          return isAssignableFrom(requiredType, Map.class);
        }
        else {
          if (DomUtil.hasXml(getSet())) {
            return (requiredType instanceof PsiEllipsisType) || isAssignableFrom(requiredType, Set.class);
          }
          else if (DomUtil.hasXml(getArray())) {
            return (requiredType instanceof PsiEllipsisType) || requiredType.isAssignableFrom(InfraPropertyUtils.getArrayType(getArray()));
          }
          else if (DomUtil.hasXml(getProps())) {
            return isAssignableFrom(requiredType, Properties.class);
          }
          else {
            if (DomUtil.hasXml(getNull())) {
              return requiredType.isAssignableFrom(PsiType.NULL);
            }
            return false;
          }
        }
      }
    }
  }

  private static boolean isAssignableFrom(PsiType requiredType, GenericDomValue value) {
    PsiClass resolve;
    if (ResolvedConstructorArgsImpl.isStringOrStringArray(requiredType)) {
      return true;
    }
    if ((requiredType instanceof PsiClassType) && (resolve = ((PsiClassType) requiredType).resolve()) != null && "java.lang.Class".equals(resolve.getQualifiedName())) {
      String stringValue = value.getStringValue();
      if (StringUtil.isNotEmpty(stringValue) && JavaPsiFacade.getInstance(resolve.getProject()).findClass(stringValue, GlobalSearchScope.allScope(resolve.getProject())) != null) {
        return true;
      }
    }
    Converter<?> converter = InfraValueConvertersRegistry.of().getConverter(value, requiredType);
    return converter != null && converter.fromString(value.getStringValue(), ConvertContextFactory.createConvertContext(DomManagerImpl.getDomInvocationHandler(value))) != null;
  }

  private static boolean isAssignableFrom(PsiType type, BeanPointer<?> beanPointer) {
    if (beanPointer == null || !(type instanceof PsiClassType)) {
      return false;
    }
    PsiClassType rawType = ((PsiClassType) type).rawType();
    PsiType[] psiTypes = beanPointer.getEffectiveBeanTypes();
    for (PsiType psiType : psiTypes) {
      if (rawType.isAssignableFrom(psiType)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAssignableFrom(PsiType type, Class clazz) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(getManager().getProject());
    PsiClass psiClass = facade.findClass(clazz.getCanonicalName(), getResolveScope());
    return psiClass != null && facade.getElementFactory().createType(psiClass).isAssignableFrom(type);
  }

  @Override
  public List<PsiType> getRequiredTypes() {
    PsiType type = getType().getValue();
    if (type != null) {
      return Collections.singletonList(type);
    }
    InfraBean springBean = (InfraBean) getParent();
    GenericAttributeValue<Integer> index = getIndex();
    if (index.getValue() != null) {
      Set<PsiParameter> resolvedParameters = ConstructorArgIndexConverterImpl.multiResolve(index, springBean);
      if (resolvedParameters.size() > 0) {
        return ContainerUtil.map(resolvedParameters, PsiParameter::getType);
      }
      PsiParameter parameter = ConstructorArgIndexConverterImpl.resolve(index, springBean);
      return parameter == null ? Collections.emptyList() : Collections.singletonList(parameter.getType());
    }
    ResolvedConstructorArgs resolvedArgs = springBean.getResolvedConstructorArgs();
    String nameAttr = getNameAttr().getStringValue();
    if (nameAttr != null && resolvedArgs.getResolvedMethod() == null) {
      List<PsiType> types = new ArrayList<>();
      List<PsiMethod> psiMethods = resolvedArgs.getCheckedMethods();
      for (PsiMethod method : psiMethods) {
        for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
          if (nameAttr.equals(psiParameter.getName())) {
            types.add(psiParameter.getType());
          }
        }
      }
      return types;
    }
    return ContainerUtil.mapNotNull(resolvedArgs.getCandidates(), method2 -> {
      PsiParameter parameter2 = resolvedArgs.getResolvedArgs(method2).get(this);
      if (parameter2 != null) {
        return parameter2.getType();
      }
      return null;
    });
  }

  public int hashCode() {
    Integer value = getIndex().getValue();
    if (value == null) {
      return 0;
    }
    return value.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ConstructorArg that)) {
      return false;
    }
    XmlTag xmlTag = getXmlTag();
    if (xmlTag != null && xmlTag.equals(that.getXmlTag())) {
      return true;
    }
    Integer index = getIndex().getValue();
    return index != null && Comparing.equal(index, that.getIndex().getValue());
  }
}
