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
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemService;
import com.intellij.spring.SpringBundle;

import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCachePut;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCacheable;
import cn.taketoday.assistant.code.cache.jam.standard.JamCachePut;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheable;
import cn.taketoday.assistant.util.CommonUtils;

import com.intellij.spring.model.highlighting.jam.SpringUastInspectionBase;
import com.intellij.util.SmartList;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheableAndCachePutInspection extends SpringUastInspectionBase {

  public CacheableAndCachePutInspection() {
    super(UMethod.class);
  }

  public ProblemDescriptor[] checkMethod(UMethod umethod, InspectionManager manager, boolean isOnTheFly) {
    if (CommonUtils.isInInfraEnabledModule(umethod)) {
      PsiMethod method = umethod.getJavaPsi();
      PsiElement sourcePsi = umethod.getSourcePsi();
      if (sourcePsi == null) {
        return null;
      }
      List<CacheableElement> cacheableElements = getCacheableElements(method);
      if (cacheableElements.size() > 0) {
        List<CacheableElement> cachePutElements = getCachePutElements(method);
        if (cachePutElements.size() > 0) {
          ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
          registerProblems(cacheableElements, holder);
          registerProblems(cachePutElements, holder);
          return holder.getResultsArray();
        }
        return null;
      }
      return null;
    }
    return null;
  }

  private static void registerProblems(List<CacheableElement> cacheableElements, ProblemsHolder holder) {
    for (CacheableElement element : cacheableElements) {
      PsiElement annotation = UAnnotationKt.getNamePsiElement(UastContextKt.toUElement(element.getAnnotation(), UAnnotation.class));
      if (annotation != null) {
        holder.registerProblem(annotation, SpringBundle.message("cacheable.and.cache.put.on.the.same.method", new Object[0]), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix[0]);
      }
    }
  }

  private static List<CacheableElement> getCacheableElements(PsiMethod method) {
    SmartList smartList = new SmartList();
    smartList.addAll(SemService.getSemService(method.getProject()).getSemElements(JamCacheable.CACHEABLE_JAM_KEY, method));
    smartList.addAll(SemService.getSemService(method.getProject()).getSemElements(CustomCacheable.CUSTOM_CACHEABLE_JAM_KEY, method));
    return smartList;
  }

  private static List<CacheableElement> getCachePutElements(PsiMethod method) {
    SmartList smartList = new SmartList();
    smartList.addAll(SemService.getSemService(method.getProject()).getSemElements(JamCachePut.CACHE_PUT_JAM_KEY, method));
    smartList.addAll(SemService.getSemService(method.getProject()).getSemElements(CustomCachePut.CUSTOM_CACHE_PUT_JAM_KEY, method));
    return smartList;
  }
}
