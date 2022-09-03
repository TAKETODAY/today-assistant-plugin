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

package cn.taketoday.assistant.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.model.BeanPsiTarget;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.lang.Nullable;

public class InfraBeansScopeEnlarger extends UseScopeEnlarger {

  @Nullable
  public SearchScope getAdditionalUseScope(PsiElement element) {
    if (element instanceof PomTargetPsiElement pomTargetPsiElement) {
      PomTarget target = pomTargetPsiElement.getTarget();
      if (target instanceof BeanPsiTarget beanPsiTarget) {
        CommonInfraBean infraBean = beanPsiTarget.getInfraBean();
        Module module = infraBean.getModule();
        if (module != null) {
          SearchScope scope = null;
          Set<Module> dependencies = new HashSet<>();
          ModuleUtilCore.getDependencies(module, dependencies);
          for (Module dependencyModule : dependencies) {
            SearchScope moduleWithDependenciesAndLibrariesScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(dependencyModule);
            if (scope == null) {
              scope = moduleWithDependenciesAndLibrariesScope;
            }
            else {
              scope = scope.intersectWith(moduleWithDependenciesAndLibrariesScope);
            }
          }
          return scope;
        }
      }
    }

    return null;
  }
}
