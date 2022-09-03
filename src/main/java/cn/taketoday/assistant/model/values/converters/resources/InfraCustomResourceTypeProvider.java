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

package cn.taketoday.assistant.model.values.converters.resources;

import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.xml.GenericDomValue;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.values.converters.InfraValueConditionFactory;
import cn.taketoday.lang.Nullable;

public class InfraCustomResourceTypeProvider implements InfraResourceTypeProvider {
  private final Map<Condition<GenericDomValue>, Condition<PsiFileSystemItem>> myFilterMap = new HashMap<>();

  public InfraCustomResourceTypeProvider() {
    Condition<PsiFileSystemItem> condition = psiFileSystemItem ->
            (psiFileSystemItem instanceof PsiFile) && PropertiesImplUtil.isPropertiesFile((PsiFile) psiFileSystemItem);
    addResourceFilter(InfraConstant.PROPERTIES_FACTORY_BEAN, condition, "location", "locations");
    addResourceFilter(PlaceholderUtils.PLACEHOLDER_CONFIGURER_CLASS, condition, "location", "locations");
  }

  private void addResourceFilter(String beanClass, Condition<PsiFileSystemItem> condition, String... propertyNames) {
    this.myFilterMap.put(InfraValueConditionFactory.createBeanPropertyCondition(beanClass, propertyNames), condition);
  }

  @Override
  @Nullable
  public Condition<PsiFileSystemItem> getResourceFilter(GenericDomValue genericDomValue) {
    for (Map.Entry<Condition<GenericDomValue>, Condition<PsiFileSystemItem>> entry : this.myFilterMap.entrySet()) {
      if (entry.getKey().value(genericDomValue)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
