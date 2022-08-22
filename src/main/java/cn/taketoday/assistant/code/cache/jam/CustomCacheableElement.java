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

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemService;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AliasForElement;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public abstract class CustomCacheableElement<T extends PsiMember & PsiNamedElement> extends CacheableElement<T> {
  public static final SemKey<CustomCacheableElement> CUSTOM_ROOT_JAM_KEY = CACHEABLE_ROOT_JAM_KEY.subKey("SpringJamCustomCacheableElement");
  private final String[] CACHE_NAMES_ATTRIBUTES;
  private final NullableLazyValue<JamBaseCacheableElement> myDefiningMetaElement;

  protected abstract String getDefiningAnnotation();

  public CustomCacheableElement(String annoName, T t) {
    super(t, annoName);
    this.CACHE_NAMES_ATTRIBUTES = new String[] { "value", JamBaseCacheableElement.CACHE_NAMES_ATTR_NAME };
    this.myDefiningMetaElement = new NullableLazyValue<>() {
      @Nullable
      public JamBaseCacheableElement<?> compute() {
        PsiClass annotationType;
        PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(CustomCacheableElement.this.getAnnotation(),
                CustomCacheableElement.this.getAnnotationMeta().getAnnoName(), CustomCacheableElement.this.getDefiningAnnotation());
        if (definingMetaAnnotation != null && (annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true)) != null) {
          return SemService.getSemService(annotationType.getProject()).getSemElement(JamBaseCacheableElement.CACHEABLE_BASE_JAM_KEY, annotationType);
        }
        return null;
      }
    };
  }

  @Override
  public Set<String> getCacheNames() {
    Set<String> names = new LinkedHashSet<>();
    for (String attrName : this.CACHE_NAMES_ATTRIBUTES) {
      AliasForElement aliasFor = getAliasAttribute(attrName);
      if (aliasFor != null) {
        var collection = JamAttributeMeta.collectionString(aliasFor.getMethodName());
        for (JamStringAttributeElement<String> stringAttributeElement : collection.getJam(getPsiAnnotationRef())) {
          ContainerUtil.addIfNotNull(names, stringAttributeElement.getValue());
        }
        return names;
      }
    }
    JamBaseCacheableElement baseCacheableElement = myDefiningMetaElement.getValue();
    if (baseCacheableElement != null) {
      return (Set<String>) baseCacheableElement.getCacheNames();
    }
    return Collections.emptySet();
  }

  private AliasForElement getAliasAttribute(String attrName) {
    PsiAnnotation annotation = getAnnotation();
    if (annotation == null) {
      return null;
    }
    return AliasForUtils.findAliasFor(annotation, getAnnotationMeta().getAnnoName(), getDefiningAnnotation(), attrName);
  }
}
