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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.model.xml.beans.InfraBean;

public abstract class AbstractProxiedTypeResolver extends AbstractTypeResolver {

  protected static final String PROXY_CLASS_FLAG_PROPERTY_NAME = "proxyTargetClass";

  protected static final String OPTIMIZE_PROPERTY_NAME = "optimize";

  protected static Set<PsiClass> getAllInterfaces(PsiType psiType) {
    Set<PsiClass> interfaces = new HashSet<>();
    if (psiType instanceof PsiClassType) {
      PsiClass psiClass = ((PsiClassType) psiType).resolve();
      if (psiClass == null) {
        return Collections.emptySet();
      }
      else if (psiClass.isInterface()) {
        return Collections.singleton(psiClass);
      }
      else {
        while (psiClass != null) {
          ContainerUtil.addAll(interfaces, psiClass.getInterfaces());
          psiClass = psiClass.getSuperClass();
        }
      }
    }
    return interfaces;
  }

  public static Set<String> getAllInterfaceNames(PsiType type) {
    Set<PsiClass> interfaces = getAllInterfaces(type);
    Set<String> names = new HashSet<>(interfaces.size());
    for (PsiClass anInterface : interfaces) {
      names.add(anInterface.getQualifiedName());
    }
    return names;
  }

  public boolean isCglibExplicitlyEnabled(InfraBean context) {
    return isBooleanPropertySetAndTrue(context, PROXY_CLASS_FLAG_PROPERTY_NAME)
            || isBooleanPropertySetAndTrue(context, OPTIMIZE_PROPERTY_NAME);
  }
}
