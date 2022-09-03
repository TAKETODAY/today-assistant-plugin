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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomUtil;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.lang.Nullable;

public class MethodInvokingFactoryBeanTypeResolver extends AbstractProxiedTypeResolver {

  @Override

  public Set<PsiType> getObjectType(@Nullable CommonInfraBean context) {
    InfraBean infraBean;
    InfraPropertyDefinition targetMethod;
    Module module;
    PsiClass psiClass;
    if ((context instanceof InfraBean)
            && (targetMethod = InfraPropertyUtils.findPropertyByName((infraBean = (InfraBean) context), "targetMethod")) != null) {
      String methodName = targetMethod.getValueAsString();
      if (StringUtil.isNotEmpty(methodName) && (module = context.getModule()) != null && (psiClass = getMethodInvokingPsiClass(null, module.getProject(), infraBean)) != null) {
        PsiMethod[] methodsByName = psiClass.findMethodsByName(methodName, true);
        if (0 < methodsByName.length) {
          PsiMethod method = methodsByName[0];
          return Collections.singleton(method.getReturnType());
        }
      }
    }
    return Collections.emptySet();
  }

  @Nullable
  public static PsiClass getMethodInvokingPsiClass(@Nullable GlobalSearchScope scope, Project project, InfraBean infraBean) {
    String className;
    InfraPropertyDefinition targetObjectProperty = InfraPropertyUtils.findPropertyByName(infraBean, "targetObject");
    if (targetObjectProperty != null) {
      BeanPointer<?> beanPointer = targetObjectProperty.getRefValue();
      if (beanPointer != null) {
        PsiType[] types = getEffectiveFactoryTypes(beanPointer.getBean());
        if (types.length == 1) {
          return PsiTypesUtil.getPsiClass(types[0]);
        }
      }
      else if (targetObjectProperty instanceof InfraProperty) {
        InfraBean bean = ((InfraProperty) targetObjectProperty).getBean();
        if (DomUtil.hasXml(bean)) {
          PsiType[] types2 = getEffectiveFactoryTypes(bean);
          if (types2.length == 1) {
            return PsiTypesUtil.getPsiClass(types2[0]);
          }
        }
      }
    }
    InfraPropertyDefinition targetClassProperty = InfraPropertyUtils.findPropertyByName(infraBean, "targetClass");
    if (targetClassProperty == null || (className = targetClassProperty.getValueAsString()) == null) {
      return null;
    }
    return JavaPsiFacade.getInstance(project).findClass(className, scope != null ? scope : GlobalSearchScope.allScope(project));
  }

  private static PsiType[] getEffectiveFactoryTypes(CommonInfraBean bean) {
    return AbstractTypeResolver.getEffectiveTypes(bean);
  }

  @Override
  public boolean accept(String factoryClassName) {
    return InfraConstant.METHOD_INVOKING_FACTORY_BEAN_CLASS.equals(factoryClassName);
  }
}
