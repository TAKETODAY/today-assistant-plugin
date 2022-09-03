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

package cn.taketoday.assistant.model.values.converters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.lang.Nullable;

public class FieldRetrievingFactoryBeanConverterImpl extends FieldRetrievingFactoryBeanConverter {

  private static final String FIELD_RETRIEVING_FACTORY_BEAN_CLASS = "cn.taketoday.beans.factory.config.FieldRetrievingFactoryBean";

  private static final String STATIC_FIELD_PROPERTY_NAME = "staticField";
  private static final Condition<GenericDomValue> PROPERTY_NAME_CONDITION = InfraValueConditionFactory.createPropertyNameCondition(STATIC_FIELD_PROPERTY_NAME);

  public FieldRetrievingFactoryBeanConverterImpl() {
    super(true);
  }

  public FieldRetrievingFactoryBeanConverterImpl(boolean soft) {
    super(soft);
  }

  public static class FactoryClassCondition implements Condition<GenericDomValue> {
    public boolean value(GenericDomValue context) {
      return checkBeanClass(context);
    }
  }

  public static class FactoryClassAndPropertyCondition implements Condition<Pair<PsiType, GenericDomValue>> {
    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      var element = pair.getSecond();
      return PROPERTY_NAME_CONDITION.value(element) && checkBeanClass(element);
    }
  }

  private static boolean checkBeanClass(DomElement element) {
    return isFieldRetrievingFactoryBean(InfraConverterUtil.getCurrentBean(element));
  }

  public static boolean isFieldRetrievingFactoryBean(@Nullable CommonInfraBean infraBean) {
    PsiClass beanClass;
    return infraBean != null && (beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType())) != null && FIELD_RETRIEVING_FACTORY_BEAN_CLASS.equals(beanClass.getQualifiedName());
  }

  public static boolean isResolved(Project project, String field) {
    PsiClass psiClass;
    PsiField[] fields;
    int index = field.lastIndexOf('.');
    if (index <= 0) {
      return false;
    }
    String className = field.substring(0, index);
    String fieldName = field.substring(index + 1);
    if (!StringUtil.isEmpty(fieldName) && !StringUtil.isEmpty(className) && (psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))) != null) {
      for (PsiField psiField : psiClass.getFields()) {
        if (psiField.hasModifierProperty("static") && fieldName.equals(psiField.getName())) {
          return true;
        }
      }
      return false;
    }
    return false;
  }
}
