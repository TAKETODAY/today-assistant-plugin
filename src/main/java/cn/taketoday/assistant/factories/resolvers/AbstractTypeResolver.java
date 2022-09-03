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

package cn.taketoday.assistant.factories.resolvers;

import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.converters.values.BooleanValueConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.factories.ObjectTypeResolver;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.BeanEffectiveTypeProvider;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

abstract class AbstractTypeResolver implements ObjectTypeResolver {

  private static final String CLASS_ARRAY_EDITOR_SEPARATOR = ",";

  @Nullable
  public String getPropertyValue(@Nullable CommonInfraBean bean, String propertyName) {
    if (bean instanceof InfraBean) {
      InfraPropertyDefinition property = InfraPropertyUtils.findPropertyByName(bean, propertyName);
      if (property != null) {
        return property.getValueAsString();
      }
      return null;
    }
    else if (bean instanceof InfraJavaBean) {
      return getJavaBeanPropertyValue(propertyName, (InfraJavaBean) bean);
    }
    else {
      return null;
    }
  }

  @Nullable
  private static String getJavaBeanPropertyValue(String propertyName, InfraJavaBean javaBean) {
    PsiExpression setterParamExpression;

    PsiMethod setter = findSetterMethod(javaBean, propertyName);
    if (setter == null || (setterParamExpression = getSetterParamExpression(new LocalSearchScope(javaBean.getPsiElement()), setter)) == null) {
      return null;
    }
    if (setterParamExpression instanceof PsiClassObjectAccessExpression accessExpression) {
      return evaluatePsiClassObjectAccess(accessExpression);
    }
    if (isStringValueSetter(setter)) {
      Pair<PsiElement, String> pair = StringExpressionHelper.evaluateExpression(setterParamExpression);
      if (pair != null) {
        return pair.getSecond();
      }
    }
    return null;
  }

  @Nullable
  private static String evaluatePsiClassObjectAccess(PsiClassObjectAccessExpression expression) {
    PsiClass resolvedClass;
    PsiType type = expression.getOperand().getType();
    if (!(type instanceof PsiClassType psiClassType) || (resolvedClass = psiClassType.resolve()) == null) {
      return null;
    }
    return resolvedClass.getQualifiedName();
  }

  private static boolean isStringValueSetter(PsiMethod setter) {
    PsiClass psiClass;
    PsiType type = setter.getParameterList().getParameters()[0].getType();
    return (type instanceof PsiClassType psiClassType)
            && (psiClass = psiClassType.resolve()) != null
            && "java.lang.String".equals(psiClass.getQualifiedName());
  }

  @Nullable
  private static PsiExpression getSetterParamExpression(LocalSearchScope configurationMethodScope, PsiMethod setter) {
    Set<PsiCall> methodCallExpressions = StringExpressionHelper.searchMethodCalls(setter, configurationMethodScope);
    for (PsiCall methodCallExpression : methodCallExpressions) {
      PsiExpressionList argumentList = methodCallExpression.getArgumentList();
      if (argumentList != null) {
        PsiExpression[] expressions = argumentList.getExpressions();
        if (expressions.length == 1) {
          return expressions[0];
        }
      }
    }
    return null;
  }

  @Nullable
  private static PsiMethod findSetterMethod(InfraJavaBean javaBean, String propertyName) {
    PsiType returnType = javaBean.getPsiElement().getReturnType();
    if (returnType instanceof PsiClassType psiClassType) {
      return PropertyUtilBase.findPropertySetter(psiClassType.resolve(), propertyName, false, true);
    }
    return null;
  }

  protected static Set<String> getListOrSetValues(InfraBean bean, String propertyName) {
    InfraPropertyDefinition property = InfraPropertyUtils.findPropertyByName(bean, propertyName);
    if (property instanceof InfraProperty) {
      return InfraPropertyUtils.getListOrSetValues((InfraProperty) property);
    }
    return Collections.emptySet();
  }

  public static Set<String> getTypesFromClassArrayProperty(InfraBean context, String propertyName) {
    InfraPropertyDefinition property = InfraPropertyUtils.findPropertyByName(context, propertyName);
    if (property != null) {
      String stringValue = property.getValueAsString();
      if (stringValue != null) {
        return splitAndTrim(stringValue, CLASS_ARRAY_EDITOR_SEPARATOR);
      }
      if (property instanceof InfraProperty) {
        return InfraPropertyUtils.getListOrSetValues((InfraProperty) property);
      }
    }
    return Collections.emptySet();
  }

  private static Set<String> splitAndTrim(String value, String separator) {
    List<String> parts = StringUtil.split(value, separator);
    Set<String> trimmedParts = new HashSet<>(parts.size());
    for (String part : parts) {
      trimmedParts.add(part.trim());
    }
    return trimmedParts;
  }

  public boolean isBooleanPropertySetAndTrue(InfraBean context, String propertyName) {
    String value = getPropertyValue(context, propertyName);
    return value != null && BooleanValueConverter.getInstance(true).isTrue(value);
  }

  public boolean isBooleanPropertySetAndFalse(InfraBean context, String propertyName) {
    String value = getPropertyValue(context, propertyName);
    return value != null && !BooleanValueConverter.getInstance(true).isTrue(value);
  }

  @Nullable
  public static PsiType getTypeFromProperty(InfraBean context, String propertyName) {
    InfraPropertyDefinition targetProperty = InfraPropertyUtils.findPropertyByName(context, propertyName);
    if (targetProperty != null) {
      if (targetProperty instanceof InfraProperty property) {
        InfraBean bean = property.getBean();
        if (DomUtil.hasXml(bean)) {
          PsiType[] classes = getEffectiveTypes(bean);
          PsiManager psiManager = bean.getPsiManager();
          if (classes.length > 0 && psiManager != null) {
            return classes[0];
          }
        }
      }
      return getTypeFromNonFactoryBean(InfraPropertyUtils.findReferencedBean(targetProperty));
    }
    return null;
  }

  public static PsiType[] getEffectiveTypes(CommonInfraBean bean) {
    var collectProcessor = new CommonProcessors.CollectProcessor<PsiType>();
    for (BeanEffectiveTypeProvider provider : BeanEffectiveTypeProvider.array()) {
      if (!provider.processEffectiveTypes(bean, collectProcessor)) {
        break;
      }
    }
    Collection<PsiType> results = collectProcessor.getResults();
    return results.size() > 0 ? results.toArray(PsiType.EMPTY_ARRAY) : new PsiType[] { bean.getBeanType() };
  }

  @Nullable
  public static PsiClassType getTypeFromBeanName(InfraBean context, String beanName) {
    CommonInfraModel model = InfraModelService.of().getModel(context);
    return getTypeFromNonFactoryBean(InfraModelSearchers.findBean(model, beanName));
  }

  @Nullable
  private static PsiClassType getTypeFromNonFactoryBean(@Nullable BeanPointer<?> bean) {
    PsiClass targetBeanClass;
    if (bean != null && (targetBeanClass = bean.getBeanClass()) != null && !FactoryBeansManager.of().isFactoryBeanClass(targetBeanClass)) {
      return PsiTypesUtil.getClassType(targetBeanClass);
    }
    return null;
  }
}
