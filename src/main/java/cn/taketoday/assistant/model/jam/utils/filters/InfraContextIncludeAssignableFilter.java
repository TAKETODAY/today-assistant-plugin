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
package cn.taketoday.assistant.model.jam.utils.filters;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.lang.Nullable;

public class InfraContextIncludeAssignableFilter extends InfraContextFilter.IncludeExpression {

  public InfraContextIncludeAssignableFilter(@Nullable String expression) {
    super(expression);
  }

  @Override
  public Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages) {
    Set<InfraStereotypeElement> components = new LinkedHashSet<>();

    String fqn = getExpression();

    if (!StringUtil.isEmptyOrSpaces(fqn)) {
      GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      PsiClass assignableClass = JavaPsiFacade.getInstance(module.getProject()).findClass(fqn, searchScope);

      addCustomComponents(packages, searchScope, components, assignableClass);
    }
    return components;
  }

  public static void addCustomComponents(Set<? extends PsiPackage> packages,
          GlobalSearchScope searchScope,
          Set<? super InfraStereotypeElement> components,
          @Nullable PsiClass assignableClass) {
    if (assignableClass == null)
      return;

    addCustomComponent(components, assignableClass);

    for (PsiPackage psiPackage : packages) {
      GlobalSearchScope pkgSearchScope = searchScope.intersectWith(PackageScope.packageScope(psiPackage, true));

      Collection<PsiClass> inheritors = ClassInheritorsSearch.search(assignableClass, pkgSearchScope, true).findAll();
      for (PsiClass psiClass : inheritors) {
        addCustomComponent(components, psiClass);
      }
    }
  }

  private static void addCustomComponent(Set<? super InfraStereotypeElement> components, PsiClass componentCandidateClass) {
    PsiModifierList modifierList = componentCandidateClass.getModifierList();

    if (componentCandidateClass.isInterface() ||
            componentCandidateClass.isAnnotationType() ||
            (modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT))) {
      return;
    }

    components.add(new CustomInfraComponent(componentCandidateClass));
  }
}
