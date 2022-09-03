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
import java.util.Set;

import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

public class TransactionProxyFactoryBeanTypeResolver extends AbstractProxiedTypeResolver {
  private static final String FACTORY_CLASS = "cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean";
  private static final String PROXY_INTERFACES_PROPERTY_NAME = "proxyInterfaces";
  private static final String TARGET_PROPERTY_NAME = "target";

  @Override

  public Set<PsiType> getObjectType(@Nullable CommonInfraBean context) {
    if (context instanceof InfraBean springBean) {
      PsiType type = getTargetType(springBean);
      if (type != null && isCglibExplicitlyEnabled(springBean)) {
        return Collections.singleton(type);
      }
      Set<String> proxyInterfaceNames = getTypesFromClassArrayProperty(springBean, PROXY_INTERFACES_PROPERTY_NAME);
      if (!proxyInterfaceNames.isEmpty()) {
        return BeanCoreUtils.convertToNonNullTypes(proxyInterfaceNames, context);
      }
      else if (type != null) {
        Set<String> targetInterfaceNames = getAllInterfaceNames(type);
        if (!targetInterfaceNames.isEmpty()) {
          return BeanCoreUtils.convertToNonNullTypes(targetInterfaceNames, context);
        }
        return Collections.singleton(type);
      }
    }
    return Collections.emptySet();
  }

  @Nullable
  private static PsiType getTargetType(InfraBean context) {
    return getTypeFromProperty(context, TARGET_PROPERTY_NAME);
  }

  @Override
  public boolean accept(String factoryClassName) {
    return FACTORY_CLASS.equals(factoryClassName);
  }
}
