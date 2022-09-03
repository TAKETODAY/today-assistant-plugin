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

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.semantic.SemKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public abstract class CacheableElement<T extends PsiMember & PsiNamedElement> implements JamElement {
  public static final SemKey<CacheableElement> CACHEABLE_ROOT_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("JamCacheableElement");
  private final JamAnnotationMeta myMeta;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotationRef;
  private static final Map<String, JamAnnotationMeta> annotationMetaMap = new HashMap<>();

  public CacheableElement(T psiMember, String annoName) {
    this.myMeta = getMeta(annoName);
    this.myPsiAnnotationRef = myMeta.getAnnotationRef(psiMember);
  }

  public CacheableElement(PsiAnnotation annotation) {
    String qualifiedName = annotation.getQualifiedName();
    this.myMeta = getMeta(qualifiedName);
    this.myPsiAnnotationRef = PsiElementRef.real(annotation);
  }

  private static synchronized JamAnnotationMeta getMeta(String anno) {
    JamAnnotationMeta meta = annotationMetaMap.get(anno);
    if (meta == null) {
      meta = new JamAnnotationMeta(anno);
      annotationMetaMap.put(anno, meta);
    }
    return meta;
  }

  public JamAnnotationMeta getAnnotationMeta() {
    return this.myMeta;
  }

  @Nullable
  public final PsiAnnotation getAnnotation() {
    return getPsiAnnotationRef().getPsiElement();
  }

  public PsiElementRef<PsiAnnotation> getPsiAnnotationRef() {
    return this.myPsiAnnotationRef;
  }

  public abstract Set<String> getCacheNames();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CacheableElement<?> element = (CacheableElement) o;
    return Objects.equals(this.myMeta, element.myMeta) && Objects.equals(this.myPsiAnnotationRef, element.myPsiAnnotationRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.myMeta, this.myPsiAnnotationRef);
  }
}
