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
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
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
public class Cacheable<T extends PsiMember & PsiNamedElement> extends JamBaseCacheableElement<T> implements CacheableMarker {
  public static final SemKey<Cacheable> CACHEABLE_JAM_KEY = CACHEABLE_BASE_JAM_KEY.subKey("Cacheable");
  public static final JamAnnotationMeta CACHEABLE_ANNO_META = new JamAnnotationMeta(CacheableConstant.CACHEABLE)
          .addAttribute(VALUE_ATTR_META)
          .addAttribute(CACHE_NAMES_ATTR_META)
          .addAttribute(CACHE_MANAGER_ATTR_META)
          .addAttribute(CACHE_RESOLVER_ATTR_META)
          .addAttribute(KEY_GENERATOR_ATTR_META);

  public Cacheable(T t) {
    super(CacheableConstant.CACHEABLE, t);
  }

  public Cacheable(PsiAnnotation annotation) {
    super(annotation);
  }

  public static void register(SemRegistrar registrar, PsiMethodPattern psiMethod) {
    Cacheable.ForClass.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(CacheableConstant.CACHEABLE));
    Cacheable.ForMethod.META.register(registrar, psiMethod.withAnnotation(CacheableConstant.CACHEABLE));
  }

  public static void addElements(JamService service, GlobalSearchScope scope, Collection<CacheableElement> result) {
    result.addAll(service.getJamMethodElements(Cacheable.CACHEABLE_JAM_KEY, CacheableConstant.CACHEABLE, scope));
    result.addAll(service.getJamClassElements(CACHEABLE_JAM_KEY, CacheableConstant.CACHEABLE, scope));
  }

  public static class ForMethod extends Cacheable<PsiMethod> {
    public static final SemKey<ForMethod> JAM_KEY = CACHEABLE_JAM_KEY.subKey("JamCacheable.ForMethod");
    public static final JamMethodMeta<ForMethod> META =
            new JamMethodMeta<>(null, ForMethod.class, JAM_KEY)
                    .addAnnotation(CACHEABLE_ANNO_META);
    public static final SemKey<JamMemberMeta<PsiMethod, ForMethod>> META_KEY = META.getMetaKey().subKey("JamCacheableForMethod");

    public ForMethod(PsiMethod psiMethod) {
      super(psiMethod);
    }
  }

  public static class ForClass extends Cacheable<PsiClass> {
    public static final SemKey<ForClass> JAM_KEY = CACHEABLE_JAM_KEY.subKey("JamCacheable.ForClass");
    public static final JamClassMeta<ForClass> META =
            new JamClassMeta<>(null, ForClass.class, JAM_KEY)
                    .addAnnotation(CACHEABLE_ANNO_META);
    public static final SemKey<JamMemberMeta<PsiClass, ForClass>> META_KEY = META.getMetaKey().subKey("JamCacheablePsiClass");
    private final PsiElementRef<PsiAnnotation> myRef;

    public ForClass(PsiClass aClass) {
      super(aClass);
      this.myRef = null;
      System.out.println("ForClass aClass," + aClass);
    }

    public ForClass(PsiAnnotation annotation) {
      super(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true));
      this.myRef = PsiElementRef.real(annotation);
      System.out.println("ForClass annotation," + annotation);
    }

    @Override
    public PsiElementRef<PsiAnnotation> getPsiAnnotationRef() {
      return this.myRef == null ? super.getPsiAnnotationRef() : this.myRef;
    }

  }

}
