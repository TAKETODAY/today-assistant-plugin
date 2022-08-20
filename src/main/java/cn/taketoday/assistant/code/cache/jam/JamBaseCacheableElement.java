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
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiNamedElement;
import com.intellij.semantic.SemKey;
import com.intellij.spring.model.SpringBeanPointer;
import cn.taketoday.assistant.code.cache.CacheableConstant;
import com.intellij.spring.model.events.jam.SpringEventListener;
import com.intellij.spring.model.jam.converters.SpringBeanReferenceJamConverter;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class JamBaseCacheableElement<T extends PsiMember & PsiNamedElement> extends CacheableElement<T> {
  public static final String CACHE_NAMES_ATTR_NAME = "cacheNames";
  public static final SemKey<JamBaseCacheableElement> CACHEABLE_BASE_JAM_KEY = CACHEABLE_ROOT_JAM_KEY.subKey("SpringJamBaseCacheableElement");

  private static final JamStringAttributeMeta.Single<String> CONDITION_ATTR_META = JamAttributeMeta.singleString(SpringEventListener.CONDITION_ATTR_NAME);

  protected static final JamStringAttributeMeta.Collection<String> VALUE_ATTR_META = JamAttributeMeta.collectionString("value", new CacheableNameConverter());
  protected static final JamStringAttributeMeta.Collection<String> CACHE_NAMES_ATTR_META =
          JamAttributeMeta.collectionString(CACHE_NAMES_ATTR_NAME, new CacheableNameConverter());
  protected static final JamStringAttributeMeta.Single<SpringBeanPointer<?>> KEY_GENERATOR_ATTR_META = JamAttributeMeta.singleString("keyGenerator", new SpringBeanReferenceJamConverter(
          CacheableConstant.KEY_GENERATOR_CLASS));
  protected static final JamStringAttributeMeta.Single<SpringBeanPointer<?>> CACHE_MANAGER_ATTR_META = JamAttributeMeta.singleString("cacheManager",
          new SpringBeanReferenceJamConverter("cn.taketoday.cache.CacheManager"));
  protected static final JamStringAttributeMeta.Single<SpringBeanPointer<?>> CACHE_RESOLVER_ATTR_META = JamAttributeMeta.singleString("cacheResolver", new SpringBeanReferenceJamConverter(
          CacheableConstant.CACHE_RESOLVER_CLASS));

  public JamBaseCacheableElement(String annoName, T t) {
    super(t, annoName);
  }

  public JamBaseCacheableElement(PsiAnnotation psiAnnotation) {
    super(psiAnnotation);
  }

  public JamStringAttributeElement<String> getConditionAttributeElement() {
    return CONDITION_ATTR_META.getJam(getPsiAnnotationRef());
  }

  public List<JamStringAttributeElement<String>> getValueElement() {
    return VALUE_ATTR_META.getJam(getPsiAnnotationRef());
  }

  public List<JamStringAttributeElement<String>> getCacheNamesElement() {
    return CACHE_NAMES_ATTR_META.getJam(getPsiAnnotationRef());
  }

  public JamStringAttributeElement<SpringBeanPointer<?>> getCacheManagerElement() {
    return CACHE_MANAGER_ATTR_META.getJam(getPsiAnnotationRef());
  }

  public JamStringAttributeElement<SpringBeanPointer<?>> getCacheResolverElement() {
    return CACHE_RESOLVER_ATTR_META.getJam(getPsiAnnotationRef());
  }

  public JamStringAttributeElement<SpringBeanPointer<?>> getKeyGeneratorElement() {
    return KEY_GENERATOR_ATTR_META.getJam(getPsiAnnotationRef());
  }

  @Override
  public Set<String> getCacheNames() {
    Set<String> names = new LinkedHashSet<>();
    names.addAll(ContainerUtil.mapNotNull(getValueElement(), JamStringAttributeElement::getStringValue));
    names.addAll(ContainerUtil.mapNotNull(getCacheNamesElement(), JamStringAttributeElement::getStringValue));
    return names;
  }
}
