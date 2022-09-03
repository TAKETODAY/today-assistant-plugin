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

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.lang.Nullable;

public class InfraContextIncludeAnnotationFilter extends InfraContextFilter.IncludeExpression {

  public InfraContextIncludeAnnotationFilter(@Nullable String expression) {
    super(expression);
  }

  @Override
  public Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages) {
    final String annotation = getExpression();
    if (!StringUtil.isEmptyOrSpaces(annotation)) {
      GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      final PsiClass annotationClass = JavaPsiFacade.getInstance(module.getProject()).findClass(annotation, searchScope);

      return getAnnotatedStereotypes(annotationClass, searchScope, packages);
    }
    return Collections.emptySet();
  }

  public static Set<InfraStereotypeElement> getAnnotatedStereotypes(@Nullable PsiClass annotationClass,
          GlobalSearchScope searchScope,
          Set<PsiPackage> packages) {
    if (annotationClass == null || !annotationClass.isAnnotationType())
      return Collections.emptySet();

    final Set<InfraStereotypeElement> components = new LinkedHashSet<>();
    final Set<PsiClass> annotatedClasses = getAnnotatedClasses(annotationClass, packages, searchScope);
    for (PsiClass annotatedClass : annotatedClasses) {
      JamPsiMemberInfraBean bean = JamService.getJamService(annotatedClass.getProject())
              .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, annotatedClass);
      if (bean instanceof InfraStereotypeElement) {
        components.add((InfraStereotypeElement) bean);
      }
      else {
        components.add(new CustomInfraComponent(annotationClass.getQualifiedName(), annotatedClass));
      }
    }

    return components;
  }

  private static Set<PsiClass> getAnnotatedClasses(PsiClass annotationClass,
          Set<PsiPackage> packages,
          GlobalSearchScope searchScope) {
    final Set<PsiClass> annotatedClasses = new LinkedHashSet<>();
    Processor<PsiClass> processor = new CommonProcessors.CollectProcessor<>(annotatedClasses);

    for (PsiPackage psiPackage : packages) {
      final GlobalSearchScope scope = searchScope.intersectWith(PackageScope.packageScope(psiPackage, true));
      AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).forEach(processor);
    }
    return annotatedClasses;
  }
}
