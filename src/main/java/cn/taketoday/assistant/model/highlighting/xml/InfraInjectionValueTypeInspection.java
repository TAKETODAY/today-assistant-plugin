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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.impl.ConvertContextFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverter;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.PsiTypeUtil;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.CustomBean;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.CollectionElements;
import cn.taketoday.assistant.model.xml.beans.Idref;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraMap;
import cn.taketoday.assistant.model.xml.beans.InfraRef;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.beans.Props;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

public class InfraInjectionValueTypeInspection extends DomInfraBeanInspectionBase {

  private static void checkIdRef(InfraElementsHolder elementsHolder, DomElementAnnotationHolder holder, List<PsiType> injectionTypes) {
    if (DomUtil.hasXml(elementsHolder.getIdref())) {
      checkPropertyTypeByClass(elementsHolder, String.class, holder, injectionTypes, elementsHolder.getIdref());
    }
  }

  private static void checkSpringPropertyValueType(InfraElementsHolder elementsHolder, DomElementAnnotationHolder holder, List<PsiType> propertyTypes) {
    InfraValue value = elementsHolder.getValue();
    if (!DomUtil.hasXml(value)) {
      return;
    }
    GenericAttributeValue<PsiType> valueType = value.getType();
    PsiType type = valueType.getValue();
    if (type != null && !isAssignableFrom(type, propertyTypes)) {
      String message = InfraBundle.message("bean.bad.property.type", typesToString(propertyTypes), type.getCanonicalText());
      holder.createProblem(elementsHolder.getValue().getType(), message);
    }
  }

  private static String typesToString(List<PsiType> propertyTypes) {
    return StringUtil.join(propertyTypes, psiType -> {
      if (psiType == null) {
        return null;
      }
      return psiType.getCanonicalText();
    }, " or ");
  }

  private static boolean isAssignableFrom(PsiType type, List<? extends PsiType> propertyTypes) {
    for (PsiType propertyType : propertyTypes) {
      if (propertyType != null && propertyType.isAssignableFrom(type)) {
        return true;
      }
    }
    return false;
  }

  private void checkSpringPropertyListAndSet(InfraElementsHolder elementsHolder, DomElementAnnotationHolder holder, @Nullable List<PsiType> propertyType) {
    ListOrSet set = elementsHolder.getSet();
    ListOrSet list = elementsHolder.getList();
    ListOrSet array = elementsHolder.getArray();
    if (DomUtil.hasXml(array)) {
      checkSpringPropertyCollection(array, holder);
    }
    if (DomUtil.hasXml(set)) {
      checkSpringPropertyCollection(set, holder);
      if (propertyType != null) {
        checkPropertyTypeByClass(elementsHolder, Set.class, holder, propertyType, set);
      }
    }
    if (DomUtil.hasXml(list)) {
      checkSpringPropertyCollection(list, holder);
      if (propertyType != null) {
        checkPropertyTypeByClass(elementsHolder, List.class, holder, propertyType, list);
      }
    }
  }

  private void checkSpringPropertyMap(InfraElementsHolder elementsHolder, DomElementAnnotationHolder holder, List<PsiType> propertyTypes) {
    PsiType createType;
    PsiType createType2;
    InfraMap map = elementsHolder.getMap();
    if (DomUtil.hasXml(map)) {
      checkPropertyTypeByClass(elementsHolder, Map.class, holder, propertyTypes, map);
    }
    PsiClass keyClass = map.getKeyType().getValue();
    PsiClass valueClass = map.getValueType().getValue();
    for (PsiType propertyType : propertyTypes) {
      if (keyClass == null) {
        createType = PsiUtil.substituteTypeParameter(propertyType, "java.util.Map", 0, false);
      }
      else {
        createType = JavaPsiFacade.getElementFactory(keyClass.getProject()).createType(keyClass);
      }
      PsiType keyType = createType;
      if (valueClass == null) {
        createType2 = PsiUtil.substituteTypeParameter(propertyType, "java.util.Map", 1, false);
      }
      else {
        createType2 = JavaPsiFacade.getElementFactory(valueClass.getProject()).createType(valueClass);
      }
      PsiType valueType = createType2;
      List<InfraEntry> entries = map.getEntries();
      for (InfraEntry entry : entries) {
        GenericAttributeValue<BeanPointer<?>> keyRef = entry.getKeyRef();
        GenericAttributeValue<BeanPointer<?>> valueRef = entry.getValueRef();
        if (valueType != null && DomUtil.hasXml(valueRef)) {
          checkBeanClass(valueRef.getValue(), Collections.singletonList(valueType), valueRef, holder);
        }
        if (keyType != null && DomUtil.hasXml(keyRef)) {
          checkBeanClass(keyRef.getValue(), Collections.singletonList(keyType), keyRef, holder);
        }
      }
    }
  }

  private static void checkSpringPropertyProps(InfraElementsHolder property, DomElementAnnotationHolder holder, List<PsiType> propertyType) {
    Props props = property.getProps();
    if (DomUtil.hasXml(props)) {
      checkPropertyTypeByClass(property, Properties.class, holder, propertyType, props);
    }
  }

  public void checkSpringPropertyCollection(ListOrSet collection, DomElementAnnotationHolder holder) {
    if (!DomUtil.hasXml(collection)) {
      return;
    }
    PsiType psiClass = TypeHolderUtil.getRequiredType(collection);
    if (psiClass != null) {
      checkCollectionElementsType(psiClass, collection, holder);
    }
    for (ListOrSet listOrSet : collection.getLists()) {
      checkSpringPropertyCollection(listOrSet, holder);
    }
  }

  private void checkCollectionElementsType(PsiType type, CollectionElements collection, DomElementAnnotationHolder holder) {
    for (InfraRef ref : collection.getRefs()) {
      checkSpringRefType(ref, Collections.singletonList(type), holder);
    }
    for (Idref idref : collection.getIdrefs()) {
      if (!"java.lang.String".equals(type.getCanonicalText())) {
        holder.createProblem(idref, InfraBundle.message("idref.cannot.be.added.in.collection", type.getCanonicalText()));
      }
    }
    if (type instanceof PsiClassType) {
      for (InfraBean springBean : collection.getBeans()) {
        checkBeanClass(springBean, Collections.singletonList(type), springBean, holder);
      }
    }
  }

  private void checkSpringInjectionRefAttr(DomElementAnnotationHolder holder, @Nullable List<PsiType> propertyType, @Nullable GenericDomValue<BeanPointer<?>> refAttr) {
    BeanPointer<?> beanPointer;
    if (propertyType != null && refAttr != null && DomUtil.hasXml(refAttr) && (beanPointer = refAttr.getValue()) != null) {
      checkBeanClass(beanPointer, propertyType, refAttr, holder);
    }
  }

  private void checkSpringRefType(InfraRef ref, List<PsiType> psiTypes, DomElementAnnotationHolder holder) {
    if (!DomUtil.hasXml(ref)) {
      return;
    }
    checkBeanClass(ref.getBean().getValue(), psiTypes, ref.getBean(), holder);
    checkBeanClass(ref.getLocal().getValue(), psiTypes, ref.getLocal(), holder);
    checkBeanClass(ref.getParentAttr().getValue(), psiTypes, ref.getParentAttr(), holder);
  }

  private void checkSpringPropertyInnerBean(InfraElementsHolder elementsHolder, DomElementAnnotationHolder holder, List<PsiType> injectionTypes) {
    var findFirstProcessor = new CommonProcessors.FindFirstProcessor<CommonInfraBean>();
    InfraBeanUtils.of().processChildBeans(elementsHolder, false, findFirstProcessor);
    if (findFirstProcessor.isFound()) {
      CommonInfraBean bean = findFirstProcessor.getFoundValue();
      DomInfraBean springBean = bean instanceof CustomBean ? ((CustomBean) bean).getWrapper() : (DomInfraBean) bean;
      checkBeanClass(bean, injectionTypes, springBean, holder);
    }
  }

  protected void checkBeanClass(@Nullable BeanPointer<?> beanPointer, List<PsiType> psiTypes, DomElement annotatedElement, DomElementAnnotationHolder holder) {
    if (beanPointer != null && beanPointer.isValid()) {
      checkBeanClass(beanPointer.getBean(), psiTypes, annotatedElement, holder);
    }
  }

  protected void checkBeanClass(@Nullable CommonInfraBean springBean, List<PsiType> psiTypes, DomElement annotatedElement, DomElementAnnotationHolder holder) {
    if (springBean == null || !springBean.isValid()) {
      return;
    }
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null && !processEffectiveClassTypes(springBean, psiTypes, annotatedElement, holder)) {
      processSpringBeanResolveConverterRequiredTypes(springBean, annotatedElement, holder);
    }
  }

  private static void processSpringBeanResolveConverterRequiredTypes(CommonInfraBean springBean, DomElement annotatedElement, DomElementAnnotationHolder holder) {
    if (annotatedElement instanceof GenericDomValue genericDomValue) {
      Converter valueConverter = genericDomValue.getConverter();
      if (valueConverter instanceof InfraBeanResolveConverter converter) {
        List<PsiClassType> requiredClasses = converter.getRequiredClasses(ConvertContextFactory.createConvertContext(genericDomValue));
        processEffectiveClassTypes(springBean, requiredClasses, annotatedElement, holder);
      }
    }
  }

  private static boolean processEffectiveClassTypes(CommonInfraBean springBean, List<? extends PsiType> psiTypes, DomElement annotatedElement, DomElementAnnotationHolder holder) {
    List<PsiType> convertToEffectiveTypes = convertToEffectiveTypes(psiTypes, annotatedElement.getManager().getProject());
    if (convertToEffectiveTypes.size() != 0 && !BeanCoreUtils.isEffectiveClassType(convertToEffectiveTypes, springBean)) {
      String key = convertToEffectiveTypes.size() == 1 ? "bean.must.be.of.type" : "bean.must.be.one.of.these.types";
      String message = InfraBundle.message(key, typesToString(new ArrayList(convertToEffectiveTypes)));
      holder.createProblem(annotatedElement, message);
      return true;
    }
    return false;
  }

  private static List<PsiType> convertToEffectiveTypes(List<? extends PsiType> psiTypes, Project project) {
    List<PsiType> types = new ArrayList<>(psiTypes.size());
    for (PsiType psiArrayType : psiTypes) {
      if (psiArrayType != null) {
        if (psiArrayType instanceof PsiArrayType arrayType) {
          types.add(arrayType.getComponentType());
          addCollectionType(types, project);
        }
        else if (PsiTypeUtil.getInstance(project).isCollectionType(psiArrayType)) {
          PsiType genericType = PsiUtil.substituteTypeParameter(psiArrayType, "java.util.Collection", 0, true);
          if (genericType != null) {
            types.add(genericType);
          }
          else {
            addObjectType(types, project);
          }
        }
        types.add(psiArrayType);
      }
    }
    return types;
  }

  private static void addObjectType(List<PsiType> types, Project project) {
    addType(types, project, "java.lang.Object");
  }

  private static void addCollectionType(List<PsiType> types, Project project) {
    addType(types, project, "java.util.Collection");
  }

  private static void addType(List<PsiType> types, Project project, String fqn) {
    PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
    if (aClass != null) {
      types.add(PsiTypesUtil.getClassType(aClass));
    }
  }

  private static void checkPropertyTypeByClass(InfraElementsHolder injection, Class requiredClass, DomElementAnnotationHolder holder, List<PsiType> propertyTypes, DomElement value) {
    for (PsiType propertyType : propertyTypes) {
      if (propertyType != null && requiredClass.getName().equals(propertyType.getCanonicalText())) {
        return;
      }
    }
    Project project = injection.getManager().getProject();
    PsiType requiredType = PsiTypeUtil.getInstance(project).findType(requiredClass);
    checkPropertyType(holder, propertyTypes, value, project, requiredType);
  }

  private static void checkPropertyType(DomElementAnnotationHolder holder, List<PsiType> propertyTypes, DomElement value, Project project, @Nullable PsiType requiredType) {
    if (requiredType != null && propertyTypes.size() != 0 && !isAssignableFrom(requiredType, propertyTypes) && !PsiTypeUtil.getInstance(project).isConvertable(requiredType, propertyTypes)) {
      String message = InfraBundle.message("bean.bad.property.type", typesToString(propertyTypes), requiredType.getPresentableText());
      holder.createProblem(value, message);
    }
  }

  @Override
  protected void checkBean(DomInfraBean springBean, Beans beans, DomElementAnnotationHolder holder, InfraModel infraModel) {
    for (InfraValueHolderDefinition definition : InfraPropertyUtils.getValueHolders(springBean)) {
      List<PsiType> propertyTypes = ContainerUtil.skipNulls(TypeHolderUtil.getRequiredTypes(definition));
      checkSpringInjectionRefAttr(holder, propertyTypes, definition.getRefElement());
      if (definition instanceof InfraElementsHolder) {
        checkElementsHolder((InfraElementsHolder) definition, propertyTypes, holder);
      }
    }
  }

  private void checkElementsHolder(InfraElementsHolder elementsHolder, @Nullable List<PsiType> requiredTypes, DomElementAnnotationHolder holder) {
    if (requiredTypes != null && requiredTypes.size() > 0) {
      checkSpringPropertyValueType(elementsHolder, holder, requiredTypes);
      checkIdRef(elementsHolder, holder, requiredTypes);
      checkSpringPropertyProps(elementsHolder, holder, requiredTypes);
      checkSpringPropertyMap(elementsHolder, holder, requiredTypes);
      checkSpringPropertyInnerBean(elementsHolder, holder, requiredTypes);
    }
    checkSpringPropertyListAndSet(elementsHolder, holder, requiredTypes);
  }
}
