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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Collection;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.util.InfraUtils.filterInnerClassBeans;
import static cn.taketoday.assistant.util.InfraUtils.isDefinedAsCollectionElement;

/**
 * Reference to Spring Bean by name, optionally limited to required base-class.
 */
public class InfraBeanResourceReference extends InfraBeanReference {

  public InfraBeanResourceReference(final PsiElement element,
          TextRange range,
          @Nullable final PsiClass requiredClass,
          boolean isFactoryBeanRef) {
    super(element, range, requiredClass, isFactoryBeanRef);
  }

  @Override
  public PsiElement resolve() {
    final String beanName = getValue();

    final CommonInfraModel springModel = getInfraModel();

    BeanPointer<?> pointer = InfraModelSearchers.findBean(springModel, beanName);
    return pointer == null || !pointer.isValid() || isDefinedAsCollectionElement(pointer) ?
           resolveResourceByFqn(beanName, springModel, getElement().getProject()) : pointer.getPsiElement();
  }

  @Override
  protected Collection<BeanPointer<?>> getBeanPointers(CommonInfraModel model) {
    return filterInnerClassBeans(super.getBeanPointers(model));
  }

  @Override
  public boolean isSoft() {
    return true;
  }

  @Nullable
  public static PsiClass resolveResourceByFqn(String beanName, CommonInfraModel model, Project project) {
    Module module = model.getModule();
    GlobalSearchScope scope =
            module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(project);
    return JavaPsiFacade.getInstance(project).findClass(beanName.replace("$", "."), scope);
  }
}
