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
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemService;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.model.highlighting.jam.SpringUastInspectionBase;
import com.intellij.spring.model.utils.SpringCommonUtils;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.code.cache.jam.CachingGroup;
import cn.taketoday.assistant.code.cache.jam.JamBaseCacheableElement;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCacheConfig;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheConfig;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheNamesInspection extends SpringUastInspectionBase {

  public CacheNamesInspection() {
    super(UClass.class, UMethod.class);
  }

  public ProblemDescriptor[] checkMethod(UMethod umethod, InspectionManager manager, boolean isOnTheFly) {
    if (SpringCommonUtils.isInSpringEnabledModule(umethod)) {
      PsiMethod method = umethod.getJavaPsi();
      PsiElement sourcePsi = umethod.getSourcePsi();
      if (sourcePsi == null) {
        return null;
      }
      ProblemsHolder holder = new ProblemsHolder(manager, sourcePsi.getContainingFile(), isOnTheFly);
      checkCacheNames(method.getContainingClass(), holder, getJamCacheableElements(method, CachingGroup.ForMethod.META));
      return holder.getResultsArray();
    }
    return null;
  }

  private static List<CacheableElement> getJamCacheableElements(PsiElement psiElement, JamMemberMeta<?, ? extends CachingGroup<?>> groupMeta) {
    List<CacheableElement> elements = new ArrayList<>(SemService.getSemService(psiElement.getProject()).getSemElements(CacheableElement.CACHEABLE_ROOT_JAM_KEY, psiElement));
    CachingGroup<?> group = JamService.getJamService(psiElement.getProject())
            .getJamElement(psiElement, groupMeta);
    if (group != null) {
      elements.addAll(group.getCacheables());
      elements.addAll(group.getCacheEvict());
      elements.addAll(group.getCachePuts());
    }
    return elements;
  }

  private static boolean hasDefaultCacheNamesOrCustomCacheResolver(@Nullable PsiClass aClass) {
    if (aClass != null) {
      SemService service = SemService.getSemService(aClass.getProject());
      JamCacheConfig cacheConfig = service.getSemElement(JamCacheConfig.CACHE_CONFIG_JAM_KEY, aClass);
      if (cacheConfig != null) {
        return !cacheConfig.getCacheNamesElement().isEmpty() || cacheConfig.getCacheResolverElement().getValue() != null;
      }
      CustomCacheConfig customCacheConfig = service.getSemElement(CustomCacheConfig.JAM_KEY, aClass);
      return customCacheConfig != null && !customCacheConfig.getCacheNames().isEmpty();
    }
    return false;
  }

  private static void checkCacheNames(@Nullable PsiClass containingClass, ProblemsHolder holder, List<CacheableElement> elements) {
    PsiElement annotation;
    if (hasDefaultCacheNamesOrCustomCacheResolver(containingClass)) {
      return;
    }
    for (CacheableElement<?> cacheable : elements) {
      if (!(cacheable instanceof JamCacheConfig) && !containsNonEmptyNames(cacheable.getCacheNames()) && !hasCustomCacheResolver(cacheable) && (annotation = UAnnotationKt.getNamePsiElement(
              UastContextKt.toUElement(cacheable.getAnnotation(), UAnnotation.class))) != null) {
        holder.registerProblem(annotation, SpringBundle.message("cacheable.no.cache.could.be.resolved.for.cache.operation", new Object[0]), ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new LocalQuickFix[0]);
      }
    }
  }

  private static boolean containsNonEmptyNames(Set<String> names) {
    if (names.isEmpty()) {
      return false;
    }
    for (String name : names) {
      if (StringUtil.isNotEmpty(name)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasCustomCacheResolver(CacheableElement cacheable) {
    return (cacheable instanceof JamBaseCacheableElement) && ((JamBaseCacheableElement) cacheable).getCacheResolverElement().getValue() != null;
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    if (SpringCommonUtils.isInSpringEnabledModule(uClass)) {
      PsiClass aClass = uClass.getJavaPsi();
      PsiElement sourcePsi = uClass.getSourcePsi();
      if (sourcePsi == null) {
        return null;
      }
      ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
      checkCacheNames(aClass, holder, getJamCacheableElements(aClass, CachingGroup.ForClass.META));
      return holder.getResultsArray();
    }
    return null;
  }
}
