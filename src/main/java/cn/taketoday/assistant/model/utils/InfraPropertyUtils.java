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
package cn.taketoday.assistant.model.utils;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraInjection;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolder;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraPropertyUtils {
  @Nullable
  public static GenericDomValue<?> getPropertyDomValue(InfraValueHolderDefinition valueHolderDefinition) {
    GenericDomValue<?> valueElement = valueHolderDefinition.getValueElement();
    return valueElement != null && valueElement.getStringValue() == null ? null : valueElement;
  }

  public static Set<String> getArrayPropertyStringValues(CommonInfraBean bean, String propertyName) {
    Set<String> set = new LinkedHashSet<>();

    String value = getPropertyStringValue(bean, propertyName);
    if (value != null) {
      set.addAll(InfraUtils.tokenize(value));
    }
    else if (bean instanceof InfraBean) {
      InfraPropertyDefinition propertyDefinition = ((InfraBean) bean).getProperty(propertyName);
      if (propertyDefinition instanceof InfraProperty) {
        set.addAll(getStringValues(((InfraProperty) propertyDefinition).getList()));
        set.addAll(getStringValues(((InfraProperty) propertyDefinition).getSet()));
        set.addAll(getStringValues(((InfraProperty) propertyDefinition).getArray()));
      }
    }

    return set;
  }

  private static Set<String> getStringValues(CollectionElements elements) {
    Set<String> strings = new LinkedHashSet<>();
    for (InfraValue infraValue : elements.getValues()) {
      ContainerUtil.addIfNotNull(strings, infraValue.getStringValue());
    }
    return strings;
  }

  @Nullable
  public static String getPropertyStringValue(CommonInfraBean bean, String propertyName) {
    if (bean instanceof InfraBean) {
      InfraPropertyDefinition property = ((InfraBean) bean).getProperty(propertyName);
      return property == null ? null : property.getValueAsString();
    }

    // @Bean annotated
    if (bean instanceof ContextJavaBean) {
      PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
      if (beanClass != null) {
        PsiMethod byMethodReturnTypeBeanClass = PropertyUtilBase.findPropertySetter(beanClass, propertyName, false, true);

        // fallback to "new MyImpl()" in @Bean
        PsiMethod setter = byMethodReturnTypeBeanClass == null ? PropertyUtilBase.findPropertySetter(PsiTypesUtil.getPsiClass(bean.getBeanType(true)), propertyName, false,
                true) : byMethodReturnTypeBeanClass;
        if (setter != null) {
          Set<UExpression> uastExpressions = InfraUtils.findParameterExpressionInMethodCalls(setter, 0, new LocalSearchScope(((ContextJavaBean) bean).getPsiElement()));
          if (!uastExpressions.isEmpty()) {
            return InfraUtils.evaluateStringExpression(uastExpressions.iterator().next());
          }
        }
      }
    }
    return null;
  }

  public static Set<String> getListOrSetValues(InfraElementsHolder elementsHolder) {
    if (DomUtil.hasXml(elementsHolder.getList())) {
      return getValues(elementsHolder.getList());
    }
    if (DomUtil.hasXml(elementsHolder.getSet())) {
      return getValues(elementsHolder.getSet());
    }
    if (DomUtil.hasXml(elementsHolder.getArray())) {
      return getValues(elementsHolder.getArray());
    }
    return Collections.emptySet();
  }

  public static Set<String> getValues(ListOrSet listOrSet) {
    Set<String> values = new LinkedHashSet<>();
    for (InfraValue value : listOrSet.getValues()) {
      ContainerUtil.addIfNotNull(values, value.getStringValue());
    }
    return values;
  }

  public static PsiArrayType getArrayType(ListOrSet array) {
    PsiClass psiClass = array.getValueType().getValue();
    if (psiClass != null) {
      return PsiTypesUtil.getClassType(psiClass).createArrayType();
    }

    GlobalSearchScope scope = GlobalSearchScope.allScope(array.getManager().getProject());
    return PsiType.getJavaLangObject(array.getXmlTag().getManager(), scope).createArrayType();
  }

  public static boolean isSpecificProperty(GenericDomValue value, String propertyName, String... classNames) {
    InfraProperty property = value.getParentOfType(InfraProperty.class, false);
    if (property != null && propertyName.equals(property.getPropertyName())) {
      InfraBean bean = property.getParentOfType(InfraBean.class, false);
      if (bean != null) {
        PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType(true));
        if (beanClass == null) {
          return false;
        }

        for (String className : classNames) {
          if (InheritanceUtil.isInheritor(beanClass, className)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  @Nullable
  public static InfraPropertyDefinition findPropertyByName(
          CommonInfraBean bean, String propertyName) {
    return bean.isValid() && bean instanceof InfraBean ? ((InfraBean) bean).getProperty(propertyName) : null;
  }

  public static List<InfraValueHolderDefinition> getValueHolders(CommonInfraBean bean) {
    return bean instanceof DomInfraBean
           ? DomUtil.getDefinedChildrenOfType((DomElement) bean, InfraValueHolderDefinition.class)
           : Collections.emptyList();
  }

  public static List<InfraPropertyDefinition> getProperties(CommonInfraBean bean) {
    return bean instanceof DomInfraBean
           ? DomUtil.getDefinedChildrenOfType((DomElement) bean, InfraPropertyDefinition.class)
           : Collections.emptyList();
  }

  public static List<BeanPointer<?>> getInfraValueHolderDependencies(InfraValueHolderDefinition valueHolder) {
    return new ArrayList<>(getValueHolderDependencies(valueHolder).keySet());
  }

  public static Map<BeanPointer<?>, DomElement> getValueHolderDependencies(InfraValueHolderDefinition valueHolder) {
    Map<BeanPointer<?>, DomElement> beans = new LinkedHashMap<>();
    addValueHolder(valueHolder, beans);
    return beans;
  }

  private static void addValueHolder(InfraValueHolderDefinition definition, Map<BeanPointer<?>, DomElement> beans) {
    GenericDomValue<BeanPointer<?>> element = definition.getRefElement();
    if (element != null) {
      addBasePointer(element, beans);
    }

    if (definition instanceof final InfraValueHolder valueHolder) {
      addInfraRefBeans(valueHolder.getRef(), beans);
      addIdrefBeans(valueHolder.getIdref(), beans);

      processCollections(beans, valueHolder);

      if (DomUtil.hasXml(valueHolder.getMap())) {
        addMapReferences(valueHolder.getMap(), beans);
      }

      InfraBean innerBean = valueHolder.getBean();
      if (DomUtil.hasXml(innerBean)) {
        beans.put(InfraBeanService.of().createBeanPointer(innerBean), innerBean);
      }
    }
  }

  private static void processCollections(Map<BeanPointer<?>, DomElement> beans, InfraValueHolder valueHolder) {
    List<ListOrSet> listOrSets = DomUtil.getChildrenOfType(valueHolder, ListOrSet.class);
    for (ListOrSet listOrSet : listOrSets) {
      DomElement domElement = listOrSet.getManager()
              .getDomElement(listOrSet.getXmlTag()); // instantiate dom element of proper type,  <util:list .../> is UtilList.class

      if (domElement instanceof DomInfraBean) {
        beans.put(InfraBeanService.of().createBeanPointer((DomInfraBean) domElement), domElement);
      }
      else {
        addCollectionReferences(listOrSet, beans);
      }
    }
  }

  private static void addBasePointer(GenericDomValue<BeanPointer<?>> value,
          Map<BeanPointer<?>, DomElement> beans) {
    BeanPointer<?> beanPointer = value.getValue();
    if (beanPointer == null) {
      return;
    }
    BeanPointer<?> basePointer = beanPointer.getBasePointer();
    beans.put(basePointer, value);
  }

  private static void addMapReferences(InfraMap map, Map<BeanPointer<?>, DomElement> beans) {
    for (InfraEntry entry : map.getEntries()) {
      addValueHolder(entry, beans);
    }
  }

  private static void addIdrefBeans(Idref idref, Map<BeanPointer<?>, DomElement> beans) {
    addBasePointer(idref.getLocal(), beans);
    addBasePointer(idref.getBean(), beans);
  }

  private static void addInfraRefBeans(InfraRef infraRef, Map<BeanPointer<?>, DomElement> beans) {
    if (DomUtil.hasXml(infraRef)) {
      addBasePointer(infraRef.getBean(), beans);
      addBasePointer(infraRef.getLocal(), beans);
    }
  }

  public static void addCollectionReferences(CollectionElements elements, Map<BeanPointer<?>, DomElement> beans) {
    for (InfraRef infraRef : elements.getRefs()) {
      addInfraRefBeans(infraRef, beans);
    }
    for (Idref idref : elements.getIdrefs()) {
      addIdrefBeans(idref, beans);
    }
    for (ListOrSet listOrSet : elements.getLists()) {
      addCollectionReferences(listOrSet, beans);
    }
    for (ListOrSet listOrSet : elements.getSets()) {
      addCollectionReferences(listOrSet, beans);
    }
    for (ListOrSet listOrSet : elements.getArrays()) {
      addCollectionReferences(listOrSet, beans);
    }

    for (InfraBean innerBean : elements.getBeans()) {
      beans.put(InfraBeanService.of().createBeanPointer(innerBean), innerBean);
    }
    for (InfraMap map : elements.getMaps()) {
      addMapReferences(map, beans);
    }
  }

  public static List<BeanPointer<?>> getCollectionElementDependencies(CollectionElements collectionElements) {
    Map<BeanPointer<?>, DomElement> beans = new LinkedHashMap<>();
    addCollectionReferences(collectionElements, beans);
    return new ArrayList<>(beans.keySet());
  }

  public static boolean processInfraValues(InfraProperty property,
          PairProcessor<? super GenericDomValue, ? super String> processor) {
    GenericAttributeValue<String> valueAttr = property.getValueAttr();
    XmlAttribute valueAttrElement = valueAttr.getXmlAttribute();
    String valueAttrString = valueAttr.getStringValue();
    if (valueAttrElement != null && valueAttrString != null && !processor.process(valueAttr, valueAttrString)) {
      return false;
    }

    InfraValue value = property.getValue();
    XmlTag valueElement = value.getXmlTag();
    String valueString = value.getStringValue();
    if (valueElement != null && valueString != null && !processor.process(value, valueString)) {
      return false;
    }

    if (!processInfraListOrSetValues(property.getList(), processor))
      return false;
    if (!processInfraListOrSetValues(property.getSet(), processor))
      return false;
    if (!processInfraListOrSetValues(property.getArray(), processor))
      return false;
    if (!processInfraPointerValue(property.getRefAttr(), processor))
      return false;
    return processInfraPointerValue(property.getRef().getBean(), processor);
  }

  private static boolean processInfraPointerValue(GenericAttributeValue<BeanPointer<?>> pointerValue,
          PairProcessor<? super GenericDomValue, ? super String> processor) {
    BeanPointer<?> pointer = pointerValue.getValue();
    CommonInfraBean bean = pointer == null ? null : pointer.getBean();
    if (bean instanceof ListOrSet) {
      return processInfraListOrSetValues((ListOrSet) bean, processor);
    }
    else if (bean != null) {
      InfraPropertyDefinition value = findPropertyByName(bean, "sourceList");
      if (value == null)
        value = findPropertyByName(bean, "sourceSet");
      if (value instanceof InfraProperty) {
        return processInfraListOrSetValues(((InfraProperty) value).getList(), processor);
      }
    }
    return true;
  }

  private static boolean processInfraListOrSetValues(ListOrSet listOrSet,
          PairProcessor<? super GenericDomValue, ? super String> processor) {
    for (InfraValue infraValue : listOrSet.getValues()) {
      XmlTag element = infraValue.getXmlTag();
      String string = infraValue.getStringValue();
      if (element != null && string != null && !processor.process(infraValue, string)) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public static BeanPointer<?> findReferencedBean(InfraPropertyDefinition definition) {
    BeanPointer<?> beanPointer = definition.getRefValue();
    if (beanPointer != null) {
      return beanPointer;
    }
    return definition instanceof InfraInjection ? findReferencedBean((InfraInjection) definition) : null;
  }

  @Nullable
  public static BeanPointer<?> findReferencedBean(InfraInjection injection) {
    BeanPointer<?> refAttrPointer = injection.getRefAttr().getValue();
    if (refAttrPointer != null) {
      return refAttrPointer;
    }
    if (DomUtil.hasXml(injection.getRef())) {
      InfraRef infraRef = injection.getRef();

      BeanPointer<?> beanPointer = infraRef.getBean().getValue();
      if (beanPointer != null) {
        return beanPointer;
      }
      BeanPointer<?> localPointer = infraRef.getLocal().getValue();
      if (localPointer != null) {
        return localPointer;
      }
      return infraRef.getParentAttr().getValue();
    }
    else if (DomUtil.hasXml(injection.getBean())) {
      return InfraBeanService.of().createBeanPointer(injection.getBean());
    }

    return null;
  }
}
