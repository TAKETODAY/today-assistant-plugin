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
package cn.taketoday.assistant.model.highlighting.dom;

import com.intellij.codeInsight.daemon.impl.quickfix.ExtendsListFix;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.xml.RequiredBeanType;

/**
 * @author Yann C&eacute;bron
 * @see RequiredBeanType
 */
public final class RequiredBeanTypeChecker {

  private RequiredBeanTypeChecker() {
  }

  public static void check(DomElement element, DomElementAnnotationHolder holder) {
    if (!(element instanceof GenericAttributeValue)) {
      return;
    }

    final RequiredBeanType requiredBeanType = element.getAnnotation(RequiredBeanType.class);
    if (requiredBeanType == null) {
      return;
    }

    Object value = ((GenericAttributeValue<?>) element).getValue();
    if (value == null) {
      return;
    }

    final String[] requiredClasses = requiredBeanType.value();
    short notFoundRequiredClasses = 0;
    final List<PsiClass> foundRequiredClasses = new SmartList<>();
    for (String requiredClassName : requiredClasses) {
      final PsiClass requiredClass = DomJavaUtil.findClass(requiredClassName, element);
      if (requiredClass == null) {
        notFoundRequiredClasses++;
        continue;
      }
      foundRequiredClasses.add(requiredClass);
    }

    // stop if we cannot resolve any of base class(es)
    final boolean isOneRequiredClass = requiredClasses.length == 1;
    if (notFoundRequiredClasses == requiredClasses.length) {
      final String message = isOneRequiredClass
                             ? InfraBundle.message("bean.base.class.not.found", requiredClasses[0])
                             : InfraBundle.message("bean.base.classes.not.found", StringUtil.join(requiredClasses, ","));
      holder.createProblem(element, message);
      return;
    }

    if (value instanceof BeanPointer) {
      BeanPointer<?> beanPointer = (BeanPointer) value;
      checkSpringBeanPointer(element, holder, requiredClasses, foundRequiredClasses, isOneRequiredClass, beanPointer, false);
    }
    else if (value instanceof List) {
      @SuppressWarnings("unchecked")
      List<BeanPointer<?>> pointers = (List<BeanPointer<?>>) value;
      for (BeanPointer pointer : pointers) {
        checkSpringBeanPointer(element, holder, requiredClasses, foundRequiredClasses, isOneRequiredClass, pointer, true);
      }
    }
    else {
      throw new IllegalArgumentException("must (List)SpringBeanPointer: " + element);
    }
  }

  private static void checkSpringBeanPointer(DomElement element,
          DomElementAnnotationHolder holder,
          String[] requiredClasses,
          List<PsiClass> foundRequiredClasses,
          boolean oneRequiredClass,
          BeanPointer<?> beanPointer,
          boolean multiple) {

    PsiType[] psiTypes = beanPointer.getEffectiveBeanTypes();
    for (PsiClass requiredClass : foundRequiredClasses) {
      for (PsiType psiType : psiTypes) {
        if (PsiTypesUtil.getClassType(requiredClass).isAssignableFrom(psiType)) {
          return;
        }
      }
    }

    final String classText = oneRequiredClass ? requiredClasses[0] : StringUtil.join(requiredClasses, ",");
    final String message;
    if (!multiple) {
      message = oneRequiredClass
                ? InfraBundle.message("bean.must.be.of.type", classText)
                : InfraBundle.message("bean.must.be.one.of.these.types", classText);
    }
    else {
      final String beanName = beanPointer.getName();
      message = oneRequiredClass
                ? InfraBundle.message("bean.name.must.be.of.type", beanName, classText)
                : InfraBundle.message("bean.name.must.be.one.of.these.types", beanName, classText);
    }

    List<LocalQuickFix> quickfixes = new ArrayList<>(foundRequiredClasses.size());
    if (psiTypes.length > 0) {
      final PsiClass psiClass = PsiTypesUtil.getPsiClass(psiTypes[0]);
      if (psiClass != null) {
        for (PsiClass foundRequiredClass : foundRequiredClasses) {
          quickfixes.add(new ExtendsListFix(psiClass, foundRequiredClass, true));
        }
      }
    }
    holder.createProblem(element, message, quickfixes.toArray(LocalQuickFix.EMPTY_ARRAY));
  }
}
