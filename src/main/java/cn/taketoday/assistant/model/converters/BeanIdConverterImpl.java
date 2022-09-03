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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolvingHint;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomTarget;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.values.converters.FieldRetrievingFactoryBeanConverter;
import cn.taketoday.assistant.model.values.converters.FieldRetrievingFactoryBeanConverterImpl;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraValueHolderDefinition;
import cn.taketoday.assistant.model.xml.beans.TypeHolderUtil;
import cn.taketoday.lang.Nullable;

public class BeanIdConverterImpl extends BeanIdConverter {
  private static final FieldRetrievingFactoryBeanConverterImpl.FactoryClassCondition CONDITION = new FieldRetrievingFactoryBeanConverterImpl.FactoryClassCondition();
  private static final FieldRetrievingFactoryBeanConverter CONVERTER = new FieldRetrievingFactoryBeanConverterImpl(true);

  public PsiReference[] createReferences(GenericDomValue<String> genericDomValue, PsiElement element, ConvertContext context) {
    if (genericDomValue.getParent() instanceof CustomBeanWrapper) {
      return PsiReference.EMPTY_ARRAY;
    }
    else if (CONDITION.value((GenericDomValue) context.getInvocationElement())) {
      return CONVERTER.createReferences(genericDomValue, element, context);
    }
    else {
      return new PsiReference[] { new BeanIdReference(element, genericDomValue) };
    }
  }

  private static List<String> suggestUnusedBeanNames(@Nullable DomInfraBean springBean) {
    if (springBean == null) {
      return Collections.emptyList();
    }
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    PsiClassType classType = beanClass == null ? null : PsiTypesUtil.getClassType(beanClass);
    Collection<BeanPointer<?>> list = InfraModelService.of().getModel(springBean).getAllCommonBeans();
    List<String> unusedReferences = new ArrayList<>();
    for (BeanPointer pointer : list) {
      CommonInfraBean bean = pointer.getBean();
      if (bean instanceof DomInfraBean domSpringBean) {
        for (InfraValueHolderDefinition definition : DomUtil.getDefinedChildrenOfType(domSpringBean, InfraValueHolderDefinition.class)) {
          GenericDomValue<BeanPointer<?>> refElement = definition.getRefElement();
          if (refElement != null && !StringUtil.isEmptyOrSpaces(refElement.getStringValue()) && refElement.getValue() == null) {
            String unusedBeanRef = refElement.getStringValue();
            if (classType != null) {
              PsiType requiredType = TypeHolderUtil.getRequiredType(definition);
              if (requiredType != null && requiredType.isAssignableFrom(classType)) {
                unusedReferences.add(unusedBeanRef);
              }
            }
            else {
              unusedReferences.add(unusedBeanRef);
            }
          }
        }
      }
    }
    return unusedReferences;
  }

  private static final class BeanIdReference extends PsiReferenceBase<PsiElement> implements ResolvingHint {
    private final GenericDomValue<String> myGenericDomValue;

    private BeanIdReference(PsiElement element, GenericDomValue<String> genericDomValue) {
      super(element, true);
      this.myGenericDomValue = genericDomValue;
    }

    public boolean canResolveTo(Class<? extends PsiElement> elementClass) {
      return ReflectionUtil.isAssignable(PomTargetPsiElement.class, elementClass);
    }

    public PsiElement resolve() {
      DomInfraBean springBean = getDomSpringBean();
      if (springBean == null) {
        return null;
      }
      DomTarget target = DomTarget.getTarget(springBean);
      if (target != null) {
        return PomService.convertToPsi(target);
      }
      return InfraBeanService.of().createBeanPointer(springBean).getPsiElement();
    }

    public Object[] getVariants() {
      DomInfraBean springBean = getDomSpringBean();
      List<String> names = BeanIdConverterImpl.suggestUnusedBeanNames(springBean);
      ContainerUtil.addAll(names, BeanCoreUtils.suggestBeanNames(springBean));
      return ArrayUtilRt.toStringArray(names);
    }

    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
      return element;
    }

    private DomInfraBean getDomSpringBean() {
      return this.myGenericDomValue.getParentOfType(DomInfraBean.class, false);
    }
  }
}
