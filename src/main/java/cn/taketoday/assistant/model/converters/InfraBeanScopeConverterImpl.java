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

import com.intellij.util.xml.ConvertContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.scope.InfraBeanScopeManager;
import cn.taketoday.lang.Nullable;

public class InfraBeanScopeConverterImpl extends InfraBeanScopeConverter {

  public Collection<BeanScope> getVariants(ConvertContext context) {
    return getAllBeanScopes(context);
  }

  public BeanScope fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    for (BeanScope defaultScope : BeanScope.getDefaultScopes()) {
      if (s.equals(defaultScope.getValue())) {
        return defaultScope;
      }
    }
    for (BeanScope beanScope : getCustomBeanScopes(context)) {
      if (s.equals(beanScope.getValue())) {
        return beanScope;
      }
    }
    return null;
  }

  public String toString(@Nullable BeanScope beanScope, ConvertContext context) {
    if (beanScope == null) {
      return null;
    }
    return beanScope.getValue();
  }

  private static List<BeanScope> getAllBeanScopes(ConvertContext context) {
    List<BeanScope> scopes = new ArrayList<>(Arrays.asList(BeanScope.getDefaultScopes()));
    scopes.addAll(getCustomBeanScopes(context));
    return scopes;
  }

  private static List<BeanScope> getCustomBeanScopes(ConvertContext convertContext) {
    return InfraBeanScopeManager.getCustomBeanScopes(convertContext.getXmlElement());
  }
}
