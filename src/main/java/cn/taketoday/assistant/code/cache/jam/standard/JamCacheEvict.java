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

package cn.taketoday.assistant.code.cache.jam.standard;

import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemRegistrar;

import java.util.Collection;

import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.CacheableElement;
import cn.taketoday.assistant.code.cache.jam.JamBaseCacheableElement;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public class JamCacheEvict<T extends PsiMember & PsiNamedElement> extends JamBaseCacheableElement<T> implements CacheEvictMarker {
  public static final SemKey<JamCacheEvict<?>> CACHE_EVICT_JAM_KEY =
          CACHEABLE_BASE_JAM_KEY.subKey("CacheEvict");
  public static final JamAnnotationMeta CACHE_EVICT_ANNO_META =
          new JamAnnotationMeta(CacheableConstant.CACHE_EVICT)
                  .addAttribute(VALUE_ATTR_META)
                  .addAttribute(CACHE_NAMES_ATTR_META)
                  .addAttribute(CACHE_MANAGER_ATTR_META)
                  .addAttribute(CACHE_RESOLVER_ATTR_META)
                  .addAttribute(KEY_GENERATOR_ATTR_META);

  public JamCacheEvict(T t) {
    super(CacheableConstant.CACHE_EVICT, t);
  }

  public JamCacheEvict(PsiAnnotation annotation) {
    super(annotation);
  }

  public static void addElements(JamService service, GlobalSearchScope scope, Collection<CacheableElement> result) {
    result.addAll(service.getJamMethodElements(CACHE_EVICT_JAM_KEY, CacheableConstant.CACHE_EVICT, scope));
    result.addAll(service.getJamClassElements(CACHE_EVICT_JAM_KEY, CacheableConstant.CACHE_EVICT, scope));
  }

  public static void register(SemRegistrar registrar, PsiMethodPattern psiMethod) {
    ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHE_EVICT));
    ForMethod.META.register(registrar, psiMethod.withAnnotation(CacheableConstant.CACHE_EVICT));
  }

  public static class ForClass extends JamCacheEvict<PsiClass> {
    public static final SemKey<ForClass> JAM_KEY = CACHE_EVICT_JAM_KEY.subKey("CacheEvictPsiClass");
    public static final JamClassMeta<ForClass> META =
            new JamClassMeta<>(null, ForClass.class, JAM_KEY).addAnnotation(CACHE_EVICT_ANNO_META);
    public static final SemKey<JamMemberMeta<PsiClass, ForClass>> META_KEY =
            META.getMetaKey().subKey("CacheEvictPsiClass");

    public ForClass(PsiClass aClass) {
      super(aClass);
    }
  }

  public static class ForMethod extends JamCacheEvict<PsiMethod> {
    public static final SemKey<ForMethod> JAM_KEY = CACHE_EVICT_JAM_KEY.subKey("CacheEvictForMethod");
    public static final JamMethodMeta<ForMethod> META =
            new JamMethodMeta<>(null, ForMethod.class, JAM_KEY)
                    .addAnnotation(CACHE_EVICT_ANNO_META);

    public static final SemKey<JamMemberMeta<PsiMethod, ForMethod>>
            META_KEY = META.getMetaKey().subKey("CacheEvictForMethod");

    public ForMethod(PsiMethod psiMethod) {
      super(psiMethod);
    }

  }

}
