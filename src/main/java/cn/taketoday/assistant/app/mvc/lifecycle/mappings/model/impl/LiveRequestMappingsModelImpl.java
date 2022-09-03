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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveHandlerMethod;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;

class LiveRequestMappingsModelImpl implements LiveRequestMappingsModel {
  private static final String METHOD_NAME_SEPARATOR = "#";
  private final List<LiveRequestMapping> myMappings;
  private final Map<String, List<LiveRequestMapping>> myMethodToMappings;

  LiveRequestMappingsModelImpl(Collection<? extends LiveRequestMapping> mappings) {
    this.myMappings = new SmartList();
    this.myMethodToMappings = new HashMap();
    this.myMappings.addAll(mappings);
    this.myMappings.forEach(mapping -> {
      LiveHandlerMethod method = mapping.getMethod();
      if (method == null) {
        return;
      }
      List<LiveRequestMapping> methodMappings = this.myMethodToMappings.computeIfAbsent(method.getClassName() + "#" + method.getMethodName(), key -> {
        return new SmartList<>();
      });
      methodMappings.add(mapping);
    });
  }

  @Override

  public List<LiveRequestMapping> getRequestMappings() {
    return Collections.unmodifiableList(this.myMappings);
  }

  @Override

  public List<LiveRequestMapping> getRequestMappingsByMethod(PsiMethod psiMethod) {
    List<LiveRequestMapping> mappings;
    if (!psiMethod.isValid()) {
      return Collections.emptyList();
    }
    PsiClass psiClass = psiMethod.getContainingClass();
    if (psiClass == null) {
      return Collections.emptyList();
    }
    List<LiveRequestMapping> mappings2 = this.myMethodToMappings.get(psiClass.getQualifiedName() + "#" + psiMethod.getName());
    if (mappings2 == null) {
      if (LiveHandlerMethodImpl.isRouterFunctionBean(psiMethod))
        if ((mappings = this.myMethodToMappings.get(psiClass.getQualifiedName() + "#$$Lambda")) != null) {
          return mappings;
        }
      return Collections.emptyList();
    }
    return ContainerUtil.filter(mappings2, mapping -> {
      LiveHandlerMethod liveHandlerMethod = mapping.getMethod();
      return liveHandlerMethod != null && liveHandlerMethod.matches(psiMethod);
    });
  }
}
