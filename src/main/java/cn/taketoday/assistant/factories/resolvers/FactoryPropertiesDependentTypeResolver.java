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

import com.intellij.psi.PsiType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.lang.Nullable;

public class FactoryPropertiesDependentTypeResolver extends AbstractTypeResolver {
  private final List<String> myPropertyNames;

  public FactoryPropertiesDependentTypeResolver(List<String> propertyNames) {
    this.myPropertyNames = propertyNames;
  }

  @Override

  public Set<PsiType> getObjectType(@Nullable CommonInfraBean context) {
    for (String propertyName : this.myPropertyNames) {
      String propertyValue = getPropertyValue(context, propertyName);
      if (propertyValue != null) {
        return BeanCoreUtils.convertToNonNullTypes(propertyValue, context);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public boolean accept(String factoryClassName) {
    return true;
  }
}
