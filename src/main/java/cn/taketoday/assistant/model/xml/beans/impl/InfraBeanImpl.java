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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.converters.InfraBeanFactoryMethodConverterImpl;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgResolveUtil;
import cn.taketoday.assistant.model.jam.qualifiers.InfraJamQualifier;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.DomInfraBeanImpl;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.CNamespaceDomElement;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraDomQualifier;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class InfraBeanImpl extends DomInfraBeanImpl implements InfraBean {
  public static final Function<InfraBean, Collection<InfraPropertyDefinition>> PROPERTIES_GETTER;
  public static final Function<InfraBean, Collection<ConstructorArg>> CTOR_ARGS_GETTER;

  @Override
  public abstract GenericAttributeValue<BeanPointer<?>> getFactoryBean();

  @Override
  public abstract GenericAttributeValue<PsiMethod> getFactoryMethod();

  static {
    PROPERTIES_GETTER = InfraPropertyUtils::getProperties;
    CTOR_ARGS_GETTER = InfraBean::getConstructorArgs;
  }

  @Override
  @Nullable
  public PsiClass getInstantiationClass() {
    GenericAttributeValue<PsiMethod> factoryMethodAttribute = this.getFactoryMethod();
    if (DomUtil.hasXml(factoryMethodAttribute)) {
      PsiMethod factoryMethod = factoryMethodAttribute.getValue();
      if (factoryMethod != null) {
        return InfraBeanFactoryMethodConverterImpl.getFactoryClass(this);
      }
    }
    return PsiTypesUtil.getPsiClass(getBeanType());
  }

  @Override

  public List<PsiMethod> getInstantiationMethods() {
    String factoryMethod;
    GenericAttributeValue<PsiMethod> factoryMethodAttribute = this.getFactoryMethod();
    if (DomUtil.hasXml(factoryMethodAttribute) && (factoryMethod = factoryMethodAttribute.getStringValue()) != null) {
      return InfraBeanFactoryMethodConverterImpl.getFactoryMethodCandidates(this, factoryMethod);
    }
    PsiClass beanClass = PsiTypesUtil.getPsiClass(getBeanType());
    if (beanClass != null) {
      return Arrays.asList(beanClass.getConstructors());
    }
    return Collections.emptyList();
  }

  @Override
  @Nullable
  public String getBeanName() {
    String name;
    if (DomUtil.hasXml(getId())) {
      return getId().getRawText();
    }
    GenericAttributeValue<List<String>> nameAttribute = getName();
    if (DomUtil.hasXml(nameAttribute) && (name = nameAttribute.getRawText()) != null) {
      List<String> list = InfraUtils.tokenize(name);
      return ContainerUtil.getFirstItem(list);
    }
    return null;
  }

  @Override
  public String[] getAliases() {
    String name = getName().getRawText();
    if (name != null) {
      List<String> list = InfraUtils.tokenize(name);
      String id = getId().getStringValue();
      if (id == null && list.size() > 1) {
        list.remove(0);
      }
      return ArrayUtilRt.toStringArray(list);
    }
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  public void setName(String newName) {
    String id = getId().getStringValue();
    if (id != null) {
      getId().setStringValue(newName);
      return;
    }
    String name = getName().getStringValue();
    if (name != null) {
      int first = StringUtil.findFirst(name, InfraUtils.ourFilter);
      if (first >= 0) {
        String newValue = newName + name.substring(first);
        getName().setStringValue(newValue);
        return;
      }
      getName().setStringValue(newName);
      return;
    }
    getId().setValue(newName);
  }

  @Override
  @Nullable
  public PsiClass getBeanClass(@Nullable Set<AbstractDomInfraBean> visited, boolean considerFactories) {
    BeanPointer<?> parent;
    if (visited == null || !visited.contains(this)) {
      PsiClass psiClass = super.getBeanClass(visited, considerFactories);
      if (psiClass != null) {
        return psiClass;
      }
      GenericAttributeValue<BeanPointer<?>> parentBeanValue = getParentBean();
      if (DomUtil.hasXml(parentBeanValue) && (parent = parentBeanValue.getValue()) != null) {
        if (visited == null) {
          visited = new HashSet<>();
        }
        visited.add(this);
        CommonInfraBean bean = parent.getBean();
        if (bean instanceof DomInfraBeanImpl) {
          return ((DomInfraBeanImpl) bean).getBeanClass(visited, considerFactories);
        }
        return null;
      }
      return null;
    }
    return null;
  }

  @Override
  public List<InfraPropertyDefinition> getAllProperties() {
    Set<InfraPropertyDefinition> list = BeanCoreUtils.getMergedSet(this, PROPERTIES_GETTER);
    return list.isEmpty() ? Collections.emptyList() : new ArrayList(list);
  }

  @Override
  public InfraPropertyDefinition getProperty(String name) {
    Ref<InfraPropertyDefinition> ref = new Ref<>();
    BeanCoreUtils.visitParents(this, false, infraBean -> {
      List<InfraPropertyDefinition> properties = InfraPropertyUtils.getProperties(infraBean);
      for (InfraPropertyDefinition property : properties) {
        if (name.equals(property.getPropertyName())) {
          ref.set(property);
          return false;
        }
      }
      return true;
    });
    return ref.get();
  }

  @Override
  public Set<ConstructorArg> getAllConstructorArgs() {
    return BeanCoreUtils.getMergedSet(this, CTOR_ARGS_GETTER);
  }

  @Override
  public ResolvedConstructorArgs getResolvedConstructorArgs() {
    return CachedValuesManager.getManager(getManager().getProject()).getCachedValue(this,
            () -> CachedValueProvider.Result.createSingleDependency(new ResolvedConstructorArgsImpl(this), PsiModificationTracker.MODIFICATION_COUNT));
  }

  @Override
  public List<CNamespaceDomElement> getCNamespaceConstructorArgDefinitions() {
    return DomUtil.getDefinedChildrenOfType(this, CNamespaceDomElement.class);
  }

  @Override
  public boolean isAbstract() {
    Boolean value;
    GenericAttributeValue<Boolean> abstractAttribute = getAbstract();
    return DomUtil.hasXml(abstractAttribute) && (value = abstractAttribute.getValue()) != null && value;
  }

  @Override
  public Autowire getBeanAutowire() {
    GenericAttributeValue<Autowire> autowireAttribute = getAutowire();
    Autowire autowire = DomUtil.hasXml(autowireAttribute) ? autowireAttribute.getValue() : null;
    if (autowire == null) {
      Beans beans = getParentOfType(Beans.class, false);
      assert beans != null;
      GenericAttributeValue<Autowire> defaultAutowireAttribute = beans.getDefaultAutowire();
      autowire = DomUtil.hasXml(defaultAutowireAttribute) ? defaultAutowireAttribute.getValue() : null;
      if (autowire == null) {
        return Autowire.DEFAULT;
      }
    }
    if (autowire == Autowire.AUTODETECT) {
      return InfraConstructorArgResolveUtil.hasEmptyConstructor(this) ? Autowire.BY_TYPE : Autowire.CONSTRUCTOR;
    }
    return autowire;
  }

  @Override
  public String toString() {
    String beanName = getBeanName();
    return beanName == null ? "Unknown" + hashCode() : beanName;
  }

  @Override
  public Collection<InfraQualifier> getInfraQualifiers() {
    PsiClass aClass;
    InfraDomQualifier qualifier = getQualifier();
    if (DomUtil.hasXml(qualifier)) {
      return Collections.singleton(qualifier);
    }
    Module module = getModule();
    if (module != null && (aClass = PsiTypesUtil.getPsiClass(getBeanType())) != null) {
      return InfraJamQualifier.findSpringJamQualifiers(module, aClass);
    }
    return Collections.emptySet();
  }

  @Override
  public boolean isPrimary() {
    Boolean primary;
    GenericAttributeValue<Boolean> primaryAttribute = getPrimary();
    if (DomUtil.hasXml(primaryAttribute) && (primary = primaryAttribute.getValue()) != null) {
      return primary;
    }
    return super.isPrimary();
  }
}
