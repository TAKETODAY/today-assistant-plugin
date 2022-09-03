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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.lang.Nullable;

public class InfraContextIncludeRegexFilter extends InfraContextFilter.IncludeExpression {

  public InfraContextIncludeRegexFilter(@Nullable String expression) {
    super(expression);
  }

  @Override

  public Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages) {
    Set<InfraStereotypeElement> components = new LinkedHashSet<>();
    String regexp = getExpression();
    if (!StringUtil.isEmptyOrSpaces(regexp)) {
      try {
        Pattern pattern = Pattern.compile(regexp);
        GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
        for (PsiClass annotatedClass : findClassesByMask(searchScope, pattern, packages)) {
          components.add(new CustomInfraComponent(annotatedClass));
        }
      }
      catch (PatternSyntaxException e) {
      }
    }
    return components;
  }

  private static Set<PsiClass> findClassesByMask(GlobalSearchScope searchScope, Pattern pattern, Set<PsiPackage> packages) {
    Set<PsiClass> classes = new LinkedHashSet<>();
    for (PsiPackage psiPackage : packages) {
      findClassesByMask(searchScope, pattern, classes, psiPackage);
    }
    return classes;
  }

  private static void findClassesByMask(GlobalSearchScope searchScope, Pattern pattern, Set<PsiClass> classes, PsiPackage psiPackage) {
    for (PsiClass psiClass : psiPackage.getClasses(searchScope)) {
      String fqn = psiClass.getQualifiedName();
      if (fqn != null && pattern.matcher(fqn).matches()) {
        classes.add(psiClass);
      }
    }
    for (PsiPackage subPackage : psiPackage.getSubPackages(searchScope)) {
      findClassesByMask(searchScope, pattern, classes, subPackage);
    }
  }
}
