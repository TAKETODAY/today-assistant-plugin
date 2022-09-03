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
package cn.taketoday.assistant.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.model.custom.CustomModuleComponentsDiscoverer;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public abstract class InfraImplicitBeansProviderBase extends CustomModuleComponentsDiscoverer {

  protected abstract Collection<CommonInfraBean> getImplicitBeans(Module module);

  @Override
  public final Collection<CommonInfraBean> getCustomComponents(@Nullable Module module) {
    return getImplicitBeans(module);
  }

  @Nullable
  protected PsiClass findClassInDependenciesAndLibraries(Module module, String className) {
    if (module.isDisposed()) {
      return null;
    }

    JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
    return facade.findClass(className,
            GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests()));
  }

  protected boolean includeTests() {
    return false;
  }

  protected void addImplicitBean(Collection<CommonInfraBean> implicitBeans,
          Module module, String className, String beanName) {
    doAddImplicitBean(implicitBeans, module, className, beanName, false);
  }

  /**
   * Adds implicit bean with given name if <em>library</em> class exists.
   *
   * @param implicitBeans List of implicit beans.
   * @param module Module.
   * @param className FQN of library class.
   * @param beanName Name of implicit bean.
   */
  protected void addImplicitLibraryBean(Collection<CommonInfraBean> implicitBeans,
          Module module,
          String className,
          String beanName) {
    doAddImplicitBean(implicitBeans, module, className, beanName, true);
  }

  private void doAddImplicitBean(Collection<CommonInfraBean> implicitBeans,
          Module module,
          String className,
          String beanName,
          boolean isLibraryClass) {
    PsiClass psiClass = isLibraryClass ? InfraUtils.findLibraryClass(module, className) :
                              findClassInDependenciesAndLibraries(module, className);
    if (psiClass != null) {
      implicitBeans.add(new InfraImplicitBean(getProviderName(), psiClass, beanName));
    }
  }

  @Override
  public Object[] getDependencies(Module module) {
    return InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies();
  }
}
