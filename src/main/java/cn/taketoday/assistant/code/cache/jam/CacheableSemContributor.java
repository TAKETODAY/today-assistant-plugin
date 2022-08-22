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

package cn.taketoday.assistant.code.cache.jam;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;
import com.intellij.spring.model.jam.SpringSemContributorUtil;
import com.intellij.util.Function;

import java.util.Collection;

import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCacheConfig;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCacheEvict;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCachePut;
import cn.taketoday.assistant.code.cache.jam.custom.CustomCacheable;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheConfig;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheEvict;
import cn.taketoday.assistant.code.cache.jam.standard.JamCachePut;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
final class CacheableSemContributor extends SemContributor {
  private final PsiClassPattern nonAnnoClass = PsiJavaPatterns.psiClass().nonAnnotationType();
  private final PsiMethodPattern psiMethod = PsiJavaPatterns.psiMethod().constructor(false);

  @Override
  public void registerSemProviders(SemRegistrar registrar, Project project) {
    SemService semService = SemService.getSemService(project);
    registerCacheable(registrar, semService);
    registerCacheEvict(registrar, semService);
    registerCachePut(registrar, semService);
    registerCacheConfig(registrar, semService);
    CachingGroup.ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHING));
    CachingGroup.ForMethod.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(CacheableConstant.CACHING));
  }

  private void registerCachePut(SemRegistrar registrar, SemService semService) {
    JamCachePut.ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHE_PUT));
    JamCachePut.ForMethod.META.register(registrar, this.psiMethod.withAnnotation(CacheableConstant.CACHE_PUT));
    Function<Module, Collection<String>> customMetaAnnotations = SpringSemContributorUtil.getCustomMetaAnnotations(CacheableConstant.CACHE_PUT);
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.nonAnnoClass, CustomCachePut.ForClass.META_KEY, CustomCachePut.ForClass.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCachePut.ForClass.JAM_KEY, CustomCachePut.ForClass.class, customMetaAnnotations, pair -> {
              return new CustomCachePut.ForClass(pair.first, pair.second);
            }, null));
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.psiMethod, CustomCachePut.ForMethod.META_KEY, CustomCachePut.ForMethod.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCachePut.ForMethod.JAM_KEY, CustomCachePut.ForMethod.class, customMetaAnnotations, pair2 -> {
              return new CustomCachePut.ForMethod(pair2.first, pair2.second);
            }, null));
  }

  private void registerCacheEvict(SemRegistrar registrar, SemService semService) {
    JamCacheEvict.ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHE_EVICT));
    JamCacheEvict.ForMethod.META.register(registrar, this.psiMethod.withAnnotation(CacheableConstant.CACHE_EVICT));
    Function<Module, Collection<String>> customMetaAnnotations = SpringSemContributorUtil.getCustomMetaAnnotations(CacheableConstant.CACHE_EVICT);
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.nonAnnoClass, cn.taketoday.assistant.code.cache.jam.custom.CustomCacheEvict.ForClass.META_KEY, cn.taketoday.assistant.code.cache.jam.custom.CustomCacheEvict.ForClass.JAM_KEY,
            SpringSemContributorUtil.createFunction(cn.taketoday.assistant.code.cache.jam.custom.CustomCacheEvict.ForClass.JAM_KEY, CustomCacheEvict.ForClass.class, customMetaAnnotations, pair -> {
              return new CustomCacheEvict.ForClass(pair.first, pair.second);
            }, null));
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.psiMethod, CustomCacheEvict.ForMethod.META_KEY, CustomCacheEvict.ForMethod.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCacheEvict.ForMethod.JAM_KEY, CustomCacheEvict.ForMethod.class, customMetaAnnotations, pair2 -> {
              return new CustomCacheEvict.ForMethod(pair2.first, pair2.second);
            }, null));
  }

  private void registerCacheable(SemRegistrar registrar, SemService semService) {
    JamCacheable.ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHEABLE));
    JamCacheable.ForMethod.META.register(registrar, this.psiMethod.withAnnotation(CacheableConstant.CACHEABLE));
    Function<Module, Collection<String>> customMetaAnnotations = SpringSemContributorUtil.getCustomMetaAnnotations(CacheableConstant.CACHEABLE);
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.nonAnnoClass, CustomCacheable.ForClass.META_KEY, CustomCacheable.ForClass.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCacheable.ForClass.JAM_KEY, CustomCacheable.ForClass.class, customMetaAnnotations, pair -> {
              return new CustomCacheable.ForClass(pair.first, pair.second);
            }, null));
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.psiMethod, CustomCacheable.ForMethod.META_KEY, CustomCacheable.ForMethod.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCacheable.ForMethod.JAM_KEY, CustomCacheable.ForMethod.class, customMetaAnnotations, pair2 -> {
              return new CustomCacheable.ForMethod(pair2.first, pair2.second);
            }, null));
  }

  private void registerCacheConfig(SemRegistrar registrar, SemService semService) {
    JamCacheConfig.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHE_CONFIG));
    var customMetaAnnotations = SpringSemContributorUtil.getCustomMetaAnnotations(CacheableConstant.CACHE_CONFIG);
    SpringSemContributorUtil.registerMetaComponents(semService, registrar, this.nonAnnoClass, CustomCacheConfig.META_KEY, CustomCacheConfig.JAM_KEY,
            SpringSemContributorUtil.createFunction(CustomCacheConfig.JAM_KEY,
                    CustomCacheConfig.class, customMetaAnnotations, pair -> new CustomCacheConfig(pair.first, pair.second), null));
  }
}
