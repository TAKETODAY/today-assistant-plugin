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

package cn.taketoday.assistant.model.scope;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.DomBeanPointer;
import cn.taketoday.assistant.model.xml.beans.InfraEntry;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;

public class InfraCustomScopeConfigurerBeanScope extends InfraCustomBeanScope {

  private static final String CUSTOM_SCOPE_CONFIGURER_CLASSNAME
          = "cn.taketoday.beans.factory.config.CustomScopeConfigurer";
  private static final String CUSTOM_SCOPES_PROPERTY_NAME = "scopes";

  @Override
  public String getScopeClassName() {
    return CUSTOM_SCOPE_CONFIGURER_CLASSNAME;
  }

  @Override
  public boolean process(List<BeanScope> scopes, Set<InfraModel> models, PsiClass scopeClass, PsiElement psiElement) {
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(scopeClass);
    for (InfraModel model : models) {
      List<BeanPointer<?>> springBeans = InfraModelSearchers.findBeans(model, searchParameters);
      for (DomBeanPointer infraBean : ContainerUtil.findAll(springBeans, DomBeanPointer.class)) {
        InfraPropertyDefinition property = InfraPropertyUtils.findPropertyByName(infraBean.getBean(), CUSTOM_SCOPES_PROPERTY_NAME);
        if (property instanceof InfraProperty) {
          for (InfraEntry springEntry : ((InfraProperty) property).getMap().getEntries()) {
            String keyValue = springEntry.getKeyAttr().getStringValue();
            if (keyValue != null && keyValue.length() > 0) {
              scopes.add(new BeanScope(keyValue));
            }
            else {
              String keyValue2 = springEntry.getKey().getValue().getStringValue();
              if (keyValue2 != null && keyValue2.length() > 0) {
                scopes.add(new BeanScope(keyValue2));
              }
            }
          }
        }
      }
    }
    return true;
  }
}
