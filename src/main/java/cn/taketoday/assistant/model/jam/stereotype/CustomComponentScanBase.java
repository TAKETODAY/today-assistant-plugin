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
package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.model.jam.converters.PackageJamConverter;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;
import cn.taketoday.lang.Nullable;

public abstract class CustomComponentScanBase extends AbstractComponentScan {
  private final JamAnnotationMeta myMeta;
  private final AnnotationChildLink myAnnotationChildLink;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  private static final Map<String, JamAnnotationMeta> annotationMetaMap = new HashMap<>();

  protected CustomComponentScanBase(String anno, PsiClass psiClassAnchor) {
    super(psiClassAnchor);
    myAnnotationChildLink = new AnnotationChildLink(anno);
    myPsiAnnotation = myAnnotationChildLink.createChildRef(psiClassAnchor);
    myMeta = getMeta(anno);
  }

  private static synchronized JamAnnotationMeta getMeta(String anno) {
    JamAnnotationMeta meta = annotationMetaMap.get(anno);
    if (meta == null) {
      meta = new JamAnnotationMeta(anno);
      annotationMetaMap.put(anno, meta);
    }
    return meta;
  }

  @Override
  public JamAnnotationMeta getAnnotationMeta() {
    return myMeta;
  }

  @Override
  public PsiElementRef<PsiAnnotation> getAnnotationRef() {
    return myPsiAnnotation;
  }

  @Override
  public List<JamStringAttributeMeta.Collection<Collection<PsiPackage>>> getPackageJamAttributes() {
    List<JamStringAttributeMeta.Collection<Collection<PsiPackage>>> list = new SmartList<>();

    ContainerUtil.addIfNotNull(list, getPackagesAttrMeta(BASE_PACKAGES_ATTR_NAME));
    ContainerUtil.addIfNotNull(list, getPackagesAttrMeta(VALUE_ATTR_NAME));

    return list;
  }

  @Override
  @Nullable
  protected JamClassAttributeMeta.Collection getBasePackageClassMeta() {
    InfraAliasFor aliasFor = getAliasAttribute(BASE_PACKAGE_CLASSES_ATTR_NAME);
    if (aliasFor != null) {
      return JamAttributeMeta.classCollection(aliasFor.getMethodName());
    }
    return null;
  }

  @Nullable
  private JamStringAttributeMeta.Collection<Collection<PsiPackage>> getPackagesAttrMeta(String name) {
    InfraAliasFor aliasFor = getAliasAttribute(name);
    if (aliasFor != null) {
      return new JamStringAttributeMeta.Collection<>(aliasFor.getMethodName(), new PackageJamConverter());
    }
    return null;
  }

  @Override
  public boolean useDefaultFilters() {
    return true;
  }

  @Override
  public Set<InfraContextFilter.Exclude> getExcludeContextFilters() {
    return Collections.emptySet(); // todo
  }

  @Override
  public Set<InfraContextFilter.Include> getIncludeContextFilters() {
    return Collections.emptySet();  //todo
  }

  protected InfraAliasFor getAliasAttribute(String attrName) {
    return AliasForUtils.findAliasFor(getPsiElement(), getAnnotationQualifiedName(),
            AnnotationConstant.COMPONENT_SCAN, attrName);
  }

  protected String getAnnotationQualifiedName() {
    return myAnnotationChildLink.getAnnotationQualifiedName();
  }
}
