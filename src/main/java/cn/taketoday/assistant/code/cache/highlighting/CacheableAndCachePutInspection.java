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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemService;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.code.cache.jam.standard.CachePut;
import cn.taketoday.assistant.code.cache.jam.standard.Cacheable;
import cn.taketoday.assistant.util.CommonUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheableAndCachePutInspection extends AbstractInfraLocalInspection {

  public CacheableAndCachePutInspection() {
    super(UMethod.class);
  }

  @Override
  public ProblemDescriptor[] checkMethod(UMethod umethod, InspectionManager manager, boolean isOnTheFly) {
    if (CommonUtils.isInInfraEnabledModule(umethod)) {
      PsiMethod method = umethod.getJavaPsi();
      PsiElement sourcePsi = umethod.getSourcePsi();
      if (sourcePsi != null) {
        List<CacheableElement<?>> cacheableElements = getElements(method, Cacheable.CACHEABLE_JAM_KEY);
        if (!cacheableElements.isEmpty()) {
          List<CacheableElement<?>> cachePutElements = getElements(method, CachePut.CACHE_PUT_JAM_KEY);
          if (!cachePutElements.isEmpty()) {
            ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
            registerProblems(cacheableElements, holder);
            registerProblems(cachePutElements, holder);
            return holder.getResultsArray();
          }
        }
      }
    }
    return null;
  }

  private static void registerProblems(List<CacheableElement<?>> cacheableElements, ProblemsHolder holder) {
    for (CacheableElement<?> element : cacheableElements) {
      PsiElement annotation = UAnnotationKt.getNamePsiElement(UastContextKt.toUElement(element.getAnnotation(), UAnnotation.class));
      if (annotation != null) {
        holder.registerProblem(annotation, InfraBundle.message("cacheable.and.cache.put.on.the.same.method"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      }
    }
  }

  private static <T extends CacheableElement<?>> List<T> getElements(PsiMethod method, SemKey semKey) {
    return SemService.getSemService(method.getProject())
            .getSemElements(semKey, method);
  }

}
