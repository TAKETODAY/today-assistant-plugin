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

package cn.taketoday.assistant.code.cache.highlighting;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.code.cache.jam.CachingGroup;
import cn.taketoday.assistant.code.cache.jam.JamBaseCacheableElement;
import cn.taketoday.assistant.model.highlighting.jam.BeanPointerResolveInspection;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheableComponentsInspection extends BeanPointerResolveInspection {

  @Override
  public ProblemDescriptor[] checkMethod(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {

    if (isPlainJavaFileInInfraModule(method)) {
      PsiAnnotation annotation;
      ProblemsHolder holder = new ProblemsHolder(manager, method.getContainingFile(), isOnTheFly);

      for (JamBaseCacheableElement cacheable : JamBaseCacheableElement.getElements(method)) {
        if (!method.getModifierList().hasModifierProperty("public")
                && (annotation = cacheable.getAnnotation()) != null) {
          holder.registerProblem(annotation, InfraBundle.message("cacheable.annotations.should.be.defined.on.public.methods"),
                  ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
        checkBeansResolve(cacheable, holder);
      }
      checkCachingGroups(holder, CachingGroup.forMethod(method));
      return holder.getResultsArray();
    }
    return null;
  }

  @Override
  public ProblemDescriptor[] checkClass(PsiClass aClass, InspectionManager manager, boolean isOnTheFly) {
    if (isPlainJavaFileInInfraModule(aClass)) {
      ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
      for (JamBaseCacheableElement cacheable : JamBaseCacheableElement.getElements(aClass)) {
        checkBeansResolve(cacheable, holder);
      }
      checkCachingGroups(holder, CachingGroup.forClass(aClass));
      return holder.getResultsArray();
    }
    return null;
  }

  public void checkCachingGroups(ProblemsHolder holder, @Nullable CachingGroup<? extends PsiMember> cachingGroups) {
    if (cachingGroups != null) {
      for (JamBaseCacheableElement element : cachingGroups.getCacheables()) {
        checkBeansResolve(element, holder);
      }
      for (JamBaseCacheableElement element2 : cachingGroups.getCachePuts()) {
        checkBeansResolve(element2, holder);
      }
      for (JamBaseCacheableElement element3 : cachingGroups.getCacheEvict()) {
        checkBeansResolve(element3, holder);
      }
    }
  }

  private static void checkBeansResolve(JamBaseCacheableElement cacheable, ProblemsHolder holder) {
    checkBeanPointerResolve(holder, cacheable.getCacheManagerElement());
    checkBeanPointerResolve(holder, cacheable.getCacheResolverElement());
    checkBeanPointerResolve(holder, cacheable.getKeyGeneratorElement());
  }
}
