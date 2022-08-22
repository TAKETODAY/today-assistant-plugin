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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemService;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.SpringBundle;
import com.intellij.spring.SpringManager;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.gutter.groups.SpringGutterIconBuilder;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.TodayLibraryUtil;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.code.cache.jam.CachingGroup;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheConfig;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheEvict;
import cn.taketoday.assistant.code.cache.jam.standard.JamCachePut;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheable;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 21:04
 */
public class CacheableAnnotator extends RelatedItemLineMarkerProvider {

  private static final NotNullFunction<CacheableElement<?>, Collection<? extends PsiElement>> CACHEABLE_CONVERTOR
          = cacheableElement -> ContainerUtil.createMaybeSingletonList(cacheableElement.getAnnotation());

  private static DefaultPsiElementCellRenderer getCacheableListCellRenderer() {
    return new DefaultPsiElementCellRenderer() {
      @Override
      public String getElementText(PsiElement element) {
        PsiMember psiMember = PsiTreeUtil.getParentOfType(element, PsiMember.class);
        return super.getElementText(psiMember == null ? element : psiMember);
      }

      @Override
      protected Icon getIcon(PsiElement element) {
        PsiMember psiMember = PsiTreeUtil.getParentOfType(element, PsiMember.class);
        return super.getIcon(psiMember == null ? element : psiMember);
      }

      @Override
      public String getContainerText(PsiElement element, String name) {
        PsiMember psiMember = PsiTreeUtil.getParentOfType(element, PsiMember.class);
        return super.getContainerText(psiMember == null ? element : psiMember, name);
      }
    };
  }

  @Override
  public String getId() {
    return "CacheableAnnotator";
  }

  @Override
  public String getName() {
    return SpringBundle.message("spring.core.cacheable.annotator.name");
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return SpringApiIcons.ShowCacheable;
  }

  @Override
  public void collectNavigationMarkers(
          List<? extends PsiElement> elements, Collection<? super RelatedItemLineMarkerInfo<?>> result, boolean forNavigation) {

    PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement != null
            && CommonUtils.hasFacets(psiElement.getProject())
            && TodayLibraryUtil.hasLibrary(psiElement.getProject())) {
      super.collectNavigationMarkers(elements, result, forNavigation);
    }
  }

  @Override
  protected void collectNavigationMarkers(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    UElement elementToProcess = UAnnotationUtils.getIdentifierAnnotationOwner(psiElement);
    if (elementToProcess instanceof UMethod || elementToProcess instanceof UClass) {
      JamService service = JamService.getJamService(psiElement.getProject());
      if (elementToProcess instanceof UMethod) {
        PsiMethod psiMethod = (PsiMethod) elementToProcess.getJavaPsi();
        if (psiMethod == null) {
          return;
        }

        if (!psiMethod.hasModifierProperty("public")
                || psiMethod.hasModifierProperty("static")
                || psiMethod.isConstructor()) {
          return;
        }

        annotateCachingGroup(psiElement,
                service.getJamElement(psiMethod, CachingGroup.ForMethod.META), result);

      }

      if (elementToProcess instanceof UClass) {
        PsiClass psiClass = (PsiClass) elementToProcess.getJavaPsi();
        if (psiClass != null) {

          annotateCachingGroup(psiElement,
                  service.getJamElement(psiClass, CachingGroup.ForClass.META), result);
        }
      }

      PsiElement sourcePsi = elementToProcess.getSourcePsi();
      if (sourcePsi != null) {
        Module module = ModuleUtilCore.findModuleForPsiElement(sourcePsi);
        if (module != null) {
          List<CacheableElement> cacheableElements = SemService.getSemService(psiElement.getProject())
                  .getSemElements(CacheableElement.CACHEABLE_ROOT_JAM_KEY, elementToProcess.getJavaPsi());
          if (!cacheableElements.isEmpty() && !hasCustomCacheResolver(module)) {

            for (CacheableElement<?> cacheableElement : cacheableElements) {
              doAnnotateCacheable(psiElement, result, cacheableElement);
            }

          }
        }
      }
    }
  }

  private static void annotateCachingGroup(PsiElement identifierToReport,
          @Nullable CachingGroup<? extends PsiMember> cachingGroups,
          Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    if (cachingGroups != null) {
      for (JamCacheable<?> cacheable : cachingGroups.getCacheables()) {
        doAnnotateCacheable(identifierToReport, result, cacheable);
      }

      for (JamCachePut<?> put : cachingGroups.getCachePuts()) {
        doAnnotateCacheable(identifierToReport, result, put);
      }

      for (JamCacheEvict<?> evict : cachingGroups.getCacheEvict()) {
        doAnnotateCacheable(identifierToReport, result, evict);
      }
    }

  }

  private static void doAnnotateCacheable(PsiElement elementToAnnotate,
          Collection<? super RelatedItemLineMarkerInfo<?>> result,
          CacheableElement<?> cacheableElement) {
    PsiElement psiAnnotationIdentifier = UAnnotationUtils.getNameElement(UastContextKt.toUElement(cacheableElement.getAnnotation()));
    if (psiAnnotationIdentifier == elementToAnnotate) {
      Set<CacheableElement<?>> cacheableElements = findCacheableWithTheSameName(cacheableElement);
      if (!cacheableElements.isEmpty()) {
        var builder = SpringGutterIconBuilder.createBuilder(SpringApiIcons.Gutter.ShowCacheable, CACHEABLE_CONVERTOR, null);
        builder.setTargets(cacheableElements)
                .setCellRenderer(CacheableAnnotator::getCacheableListCellRenderer)
                .setPopupTitle(SpringBundle.message("spring.cacheable.element.choose.title"))
                .setTooltipText(SpringBundle.message("spring.cacheable.element.tooltip.text"));
        result.add(builder.createSpringRelatedMergeableLineMarkerInfo(psiAnnotationIdentifier));
      }
    }

  }

  private static boolean hasCustomCacheResolver(Module module) {
    PsiClass cacheResolver = CommonUtils.findLibraryClass(module, CacheableConstant.CACHE_RESOLVER_CLASS);
    if (cacheResolver == null) {
      return true;
    }
    else {
      SpringModel combinedModel = SpringManager.getInstance(module.getProject()).getCombinedModel(module);
      return SpringModelSearchers.doesBeanExist(combinedModel, cacheResolver);
    }
  }

  public static Set<CacheableElement<?>> findCacheableWithTheSameName(
          CacheableElement<?> cacheableElement) {

    var ret = new HashSet<CacheableElement<?>>();

    PsiElement psiElement = cacheableElement.getAnnotation();
    Set<String> cacheNames = cacheableElement.getCacheNames();
    if (cacheNames.isEmpty()) {
      cacheNames = getDefaultCacheNames(psiElement);
    }

    if (!cacheNames.isEmpty()) {
      Set<CacheableElement> allCacheable = findAllCacheable(psiElement);
      for (CacheableElement<?> element : allCacheable) {
        if (!element.equals(cacheableElement)) {
          Set<String> names = element.getCacheNames();
          if (names.isEmpty()) {
            names = getDefaultCacheNames(element.getAnnotation());
          }
          if (hasSameNames(names, cacheNames)) {
            ret.add(element);
          }
        }
      }
    }

    return ret;
  }

  private static Set<String> getDefaultCacheNames(@Nullable PsiElement psiElement) {
    if (psiElement != null && psiElement.isValid()) {
      PsiClass aClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
      if (aClass != null) {

        SemService service = SemService.getSemService(aClass.getProject());
        JamCacheConfig cacheConfig = service.getSemElement(JamCacheConfig.CACHE_CONFIG_JAM_KEY, aClass);
        if (cacheConfig != null) {
          return cacheConfig.getCacheNames();
        }
      }
    }
    return Collections.emptySet();
  }

  private static boolean hasSameNames(Set<String> names1, Set<String> names2) {
    for (String name1 : names1) {
      for (String name2 : names2) {
        if (name1.equals(name2)) {
          return true;
        }
      }
    }
    return false;
  }

  public static Set<CacheableElement> findAllCacheable(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) {
      return Collections.emptySet();
    }
    else {
      JamService service = JamService.getJamService(module.getProject());
      GlobalSearchScope scope = psiElement.getResolveScope();
      var result = new LinkedHashSet<CacheableElement>();

      JamCachePut.addElements(service, scope, result);
      JamCacheable.addElements(service, scope, result);
      JamCacheEvict.addElements(service, scope, result);
      JamCacheConfig.addElements(service, scope, result);
      CachingGroup.addElements(service, scope, result);

      return result;
    }
  }

}

