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

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.code.cache.CacheableConstant;
import cn.taketoday.assistant.code.cache.jam.JamBaseCacheableElement;

public class JamCachePut<T extends PsiMember & PsiNamedElement> extends JamBaseCacheableElement<T> implements CachePutMarker {
  public static final SemKey<JamCachePut> CACHE_PUT_JAM_KEY = CACHEABLE_BASE_JAM_KEY.subKey("SpringJamCachePut");
  public static final JamAnnotationMeta CACHE_PUT_ANNO_META = new JamAnnotationMeta(CacheableConstant.CACHE_PUT);

  private static void $$$reportNull$$$0(int i) {
    throw new IllegalArgumentException(
            String.format("Argument for parameter '%s' of %s.%s must not be null", "annotation", "com/intellij/spring/model/cacheable/jam/standard/SpringJamCachePut", "<init>"));
  }

  static {
    CACHE_PUT_ANNO_META.addAttribute(VALUE_ATTR_META).addAttribute(CACHE_NAMES_ATTR_META).addAttribute(CACHE_MANAGER_ATTR_META).addAttribute(CACHE_RESOLVER_ATTR_META)
            .addAttribute(KEY_GENERATOR_ATTR_META);
  }

  public JamCachePut(T t) {
    super(CacheableConstant.CACHE_PUT, t);
  }

  public JamCachePut(PsiAnnotation annotation) {
    super(annotation);
    if (annotation == null) {
      $$$reportNull$$$0(0);
    }
  }

  public static class ForMethod extends JamCachePut<PsiMethod> {
    public static final SemKey<ForMethod> JAM_KEY = CACHE_PUT_JAM_KEY.subKey("SpringJamCachePutForMethod");
    public static final JamMethodMeta<ForMethod> META = new JamMethodMeta<>(null,
            ForMethod.class, JAM_KEY).addAnnotation(CACHE_PUT_ANNO_META);
    public static final SemKey<JamMemberMeta<PsiMethod, ForMethod>> META_KEY = META.getMetaKey().subKey("SpringJamCachePutForMethod");

    public ForMethod(PsiMethod psiMethod) {
      super(psiMethod);
    }
  }

  public static class ForClass extends JamCachePut<PsiClass> {
    public static final SemKey<ForClass> JAM_KEY = CACHE_PUT_JAM_KEY.subKey("SpringJamCachePutForClass");
    public static final JamClassMeta<ForClass> META = new JamClassMeta<>(null,
            ForClass.class, JAM_KEY).addAnnotation(CACHE_PUT_ANNO_META);
    public static final SemKey<JamMemberMeta<PsiClass, ForClass>> META_KEY = META.getMetaKey().subKey("SpringJamCachePutForClass");
    private PsiElementRef<PsiAnnotation> myRef;

    public ForClass(PsiClass aClass) {
      super(aClass);
      this.myRef = null;
    }

    public ForClass(PsiAnnotation annotation) {
      super(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true));
      this.myRef = null;
      this.myRef = PsiElementRef.real(annotation);
    }

    @Override
    public PsiElementRef<PsiAnnotation> getPsiAnnotationRef() {
      return this.myRef == null ? super.getPsiAnnotationRef() : this.myRef;
    }
  }

}
