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

package cn.taketoday.assistant.web.scope;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.scope.BeanScope;
import cn.taketoday.assistant.model.scope.InfraCustomBeanScope;
import cn.taketoday.assistant.util.InfraUtils;

final class InfraWebCustomBeanScope extends InfraCustomBeanScope {

  private static final String MVC_DISPATCHER_SERVLET = "cn.taketoday.web.servlet.DispatcherServlet";

  @Override
  public String getScopeClassName() {
    return MVC_DISPATCHER_SERVLET;
  }

  @Override
  public boolean process(List<BeanScope> scopes, Set<InfraModel> models, PsiClass scopeClass, PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module != null) {
      if (InfraUtils.findLibraryClass(module, MVC_DISPATCHER_SERVLET) != null) {
        addWebScopes(scopes);
      }
    }
    return true;
  }

  private static void addWebScopes(List<BeanScope> scopes) {
    scopes.add(new BeanScope("request"));
    scopes.add(new BeanScope("session"));
  }
}
