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

package cn.taketoday.assistant.model.highlighting.autowire.xml;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraPresentationProvider;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.PsiTypeUtil;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.DefaultableBoolean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public final class InfraXmlAutowiringInspection extends InfraBeanInspectionBase {

  @Override
  protected void checkBean(InfraBean springBean, Beans beans, DomElementAnnotationHolder holder, @Nullable CommonInfraModel model) {
    DefaultableBoolean autoWireCandidate;
    if (model == null) {
      return;
    }
    GenericAttributeValue<DefaultableBoolean> autowireCandidateAttribute = springBean.getAutowireCandidate();
    if (DomUtil.hasXml(autowireCandidateAttribute) && (autoWireCandidate = autowireCandidateAttribute.getValue()) != null && !autoWireCandidate.getBooleanValue()) {
      return;
    }
    checkAutowiring(springBean, model, holder);
  }

  private static void checkAutowiring(InfraBean springBean, CommonInfraModel model, DomElementAnnotationHolder holder) {
    Autowire autowire = springBean.getBeanAutowire();
    if (autowire.equals(Autowire.BY_TYPE)) {
      checkByTypeAutowire(springBean, model, holder);
    }
    else if (autowire.equals(Autowire.CONSTRUCTOR)) {
      checkByConstructorAutowire(springBean, holder);
    }
  }

  private static void checkByConstructorAutowire(InfraBean springBean, DomElementAnnotationHolder holder) {
    if (PsiTypesUtil.getPsiClass(springBean.getBeanType()) == null) {
      return;
    }
    ResolvedConstructorArgs resolvedArgs = springBean.getResolvedConstructorArgs();
    if (resolvedArgs.isResolved()) {
      return;
    }
    for (PsiMethod checkedMethod : resolvedArgs.getCheckedMethods()) {
      Map<PsiParameter, Collection<BeanPointer<?>>> autowiredParams = resolvedArgs.getAutowiredParams(checkedMethod);
      if (autowiredParams != null && autowiredParams.size() > 0) {
        Set<Map.Entry<PsiParameter, Collection<BeanPointer<?>>>> entries = autowiredParams.entrySet();
        for (Map.Entry<PsiParameter, Collection<BeanPointer<?>>> entry : entries) {
          checkAutowire(springBean, holder, checkedMethod, entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static void checkAutowire(InfraBean springBean, DomElementAnnotationHolder holder, PsiMethod checkedMethod, PsiParameter psiParameter, Collection<BeanPointer<?>> springBeans) {
    DomElement genericAttributeValue;
    PsiType psiType = psiParameter.getType();
    if (springBeans != null && springBeans.size() > 1 && !PsiTypeUtil.getInstance(psiParameter.getProject()).isCollectionType(psiType) && !(psiType instanceof PsiArrayType)) {
      List<String> beanNames = new ArrayList<>();
      for (BeanPointer pointer : springBeans) {
        beanNames.add(InfraPresentationProvider.getBeanName(pointer));
      }
      String methodName = PsiFormatUtil.formatMethod(checkedMethod, PsiSubstitutor.EMPTY, 257, 2);
      String message = InfraBundle.message("bean.autowiring.by.type", psiType.getPresentableText(), StringUtil.join(beanNames, ","), methodName);
      if (DomUtil.hasXml(springBean.getClazz())) {
        genericAttributeValue = springBean.getClazz();
      }
      else if (DomUtil.hasXml(springBean.getFactoryMethod())) {
        genericAttributeValue = springBean.getFactoryMethod();
      }
      else {
        genericAttributeValue = springBean;
      }
      holder.createProblem(genericAttributeValue, message);
    }
  }

  private static void checkByTypeAutowire(InfraBean springBean, CommonInfraModel model, DomElementAnnotationHolder holder) {
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass == null) {
      return;
    }
    for (PsiMethod psiMethod : PropertyUtilBase.getAllProperties(beanClass, true, false, true).values()) {
      if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
        checkByType(springBean, model, holder, psiMethod.getParameterList().getParameters()[0].getType(), psiMethod);
      }
    }
  }

  private static void checkByType(InfraBean springBean, CommonInfraModel model, DomElementAnnotationHolder holder, PsiType psiType, PsiMethod psiMethod) {
    Collection<BeanPointer<?>> autowireByType;
    PsiAnnotation qualifiedAnnotation = AutowireUtil.getQualifiedAnnotation(psiMethod);
    if (qualifiedAnnotation != null) {
      autowireByType = AutowireUtil.getQualifiedBeans(qualifiedAnnotation, model);
    }
    else {
      autowireByType = AutowireUtil.autowireByType(model, AutowireUtil.getAutowiredEffectiveBeanTypes(psiType));
    }
    Collection<BeanPointer<?>> beans = autowireByType;
    if (beans.size() > 1) {
      List<String> properties = new ArrayList<>();
      String propertyName = PropertyUtilBase.getPropertyNameBySetter(psiMethod);
      if (AutowireUtil.isPropertyNotDefined(springBean, propertyName)) {
        properties.add(propertyName);
      }
      if (properties.size() > 0) {
        List<String> beanNames = new ArrayList<>();
        for (BeanPointer pointer : beans) {
          beanNames.add(InfraPresentationProvider.getBeanName(pointer));
        }
        String message = InfraBundle.message("bean.autowiring.by.type", psiType.getPresentableText(), StringUtil.join(beanNames, ","), StringUtil.join(properties, ","));
        holder.createProblem(springBean, message);
      }
    }
  }
}
