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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta.Collection;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.code.cache.CacheableConstant;

import java.util.List;

import cn.taketoday.assistant.code.cache.jam.standard.JamCacheEvict;
import cn.taketoday.assistant.code.cache.jam.standard.JamCachePut;
import cn.taketoday.assistant.code.cache.jam.standard.JamCacheable;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public class CachingGroup<T extends PsiMember & PsiNamedElement> extends JamBaseElement<T> {
  public static final SemKey<CachingGroup> CACHING_GROUP_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("SpringJamCachingGroup");
  private static final Collection<JamCachePut> PUT_ATTRIBUTE = JamAttributeMeta.annoCollection("put", JamCachePut.CACHE_PUT_ANNO_META, JamCachePut.class);
  private static final Collection<JamCacheEvict> EVICT_ATTRIBUTE = JamAttributeMeta.annoCollection("evict", JamCacheEvict.CACHE_EVICT_ANNO_META, JamCacheEvict.class);
  private static final Collection<JamCacheable> CACHEABLE_ATTRIBUTE = JamAttributeMeta.annoCollection("cacheable", JamCacheable.CACHEABLE_ANNO_META, JamCacheable.class);
  protected static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(CacheableConstant.CACHING)
          .addAttribute(CACHEABLE_ATTRIBUTE)
          .addAttribute(PUT_ATTRIBUTE)
          .addAttribute(EVICT_ATTRIBUTE);

  public CachingGroup(T psiMember) {
    super(PsiElementRef.real(psiMember));
  }

  @Nullable
  public final PsiAnnotation getAnnotation() {
    return getPsiAnnotationRef().getPsiElement();
  }

  protected PsiElementRef<PsiAnnotation> getPsiAnnotationRef() {
    return ANNOTATION_META.getAnnotationRef(getPsiElement());
  }

  public List<JamCacheable> getCacheables() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNOTATION_META, CACHEABLE_ATTRIBUTE);
  }

  public List<JamCachePut> getCachePuts() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNOTATION_META, PUT_ATTRIBUTE);
  }

  public List<JamCacheEvict> getCacheEvict() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNOTATION_META, EVICT_ATTRIBUTE);
  }

  public static class ForClass extends CachingGroup<PsiClass> {
    public static final SemKey<ForClass> CACHING_GROUP_FOR_CLASS_JAM_KEY =
            CACHING_GROUP_JAM_KEY.subKey("SpringCachingGroupsForClass");
    public static final JamClassMeta<ForClass> META =
            new JamClassMeta<>(null, ForClass.class, CACHING_GROUP_FOR_CLASS_JAM_KEY)
                    .addAnnotation(ANNOTATION_META);

    public ForClass(PsiClass aClass) {
      super(aClass);
    }
  }

  public static class ForMethod extends CachingGroup<PsiMethod> {
    public static final SemKey<ForMethod> CACHING_GROUP_FOR_METHOD_JAM_KEY = CACHING_GROUP_JAM_KEY.subKey("SpringCachingGroupsForMethod");
    public static final JamMethodMeta<ForMethod> META = new JamMethodMeta<>(null, ForMethod.class,
            CACHING_GROUP_FOR_METHOD_JAM_KEY).addAnnotation(ANNOTATION_META);

    public ForMethod(PsiMethod psiMethod) {
      super(psiMethod);
    }
  }
}
