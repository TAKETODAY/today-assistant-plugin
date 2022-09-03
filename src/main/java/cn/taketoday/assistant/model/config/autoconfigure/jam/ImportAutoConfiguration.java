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

package cn.taketoday.assistant.model.config.autoconfigure.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.IMPORT_AUTO_CONFIGURATION;

public class ImportAutoConfiguration extends JamBaseElement<PsiClass> {
  private static final JamClassAttributeMeta.Collection VALUE =
          new JamClassAttributeMeta.Collection("value");
  private static final JamClassAttributeMeta.Collection CLASSES =
          new JamClassAttributeMeta.Collection("classes");
  private static final JamClassAttributeMeta.Collection EXCLUDE =
          new JamClassAttributeMeta.Collection("exclude");

  static final JamAnnotationArchetype ARCHETYPE =
          new JamAnnotationArchetype()
                  .addAttribute(VALUE)
                  .addAttribute(CLASSES)
                  .addAttribute(EXCLUDE);

  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(IMPORT_AUTO_CONFIGURATION, ARCHETYPE);

  public static final SemKey<ImportAutoConfiguration> JAM_KEY =
          JamService.JAM_ELEMENT_KEY.subKey("ImportAutoConfigSemKey");

  public static final JamClassMeta<ImportAutoConfiguration> META =
          new JamClassMeta<>(null, ImportAutoConfiguration.class, JAM_KEY)
                  .addAnnotation(ANNOTATION_META);

  public static final SemKey<JamMemberMeta<PsiClass, ImportAutoConfiguration>> META_KEY =
          JamService.MEMBER_META_KEY.subKey("ImportAutoConfigurationMeta");

  private final String myAnno;
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;
  private final NullableLazyValue<ImportAutoConfiguration> myDefiningMetaAnnotation =
          new NullableLazyValue<>() {
            @Nullable
            @Override
            protected ImportAutoConfiguration compute() {
              PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(
                      getPsiElement(), getAnnotationFqn(), IMPORT_AUTO_CONFIGURATION);
              if (definingMetaAnnotation != null) {
                PsiClass annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true);
                if (annotationType != null) {
                  return META.getJamElement(annotationType);
                }
              }
              return null;
            }
          };

  private static final Map<String, JamAnnotationMeta> ourJamAnnotationMetaMap =
          ConcurrentFactoryMap.createMap(key -> new JamAnnotationMeta(key, ARCHETYPE));

  @SuppressWarnings("unused")
  public ImportAutoConfiguration(PsiClass psiClass) {
    this(IMPORT_AUTO_CONFIGURATION, psiClass);
  }

  public ImportAutoConfiguration(@Nullable String anno, PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
    myAnno = anno;
    JamAnnotationMeta annotationMeta = ourJamAnnotationMetaMap.get(anno);
    myAnnotationRef = annotationMeta.getAnnotationRef(psiClass);
  }

  public static Function<Module, Collection<String>> getAnnotations() {
    return SemContributorUtil.getCustomMetaAnnotations(IMPORT_AUTO_CONFIGURATION);
  }

  public String getAnnotationFqn() {
    return myAnno;
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myAnnotationRef.getPsiElement();
  }

  @Nullable
  public String getSourceFqn() {
    if (IMPORT_AUTO_CONFIGURATION.equals(myAnno)) {
      return getPsiElement().getQualifiedName();
    }
    ImportAutoConfiguration definingMetaAnnotation = myDefiningMetaAnnotation.getValue();
    if (definingMetaAnnotation != null) {
      return definingMetaAnnotation.getPsiElement().getQualifiedName();
    }
    return null;
  }

  public List<PsiClass> getExcludes() {
    return ContainerUtil.mapNotNull(EXCLUDE.getJam(myAnnotationRef), JamClassAttributeElement::getValue);
  }

  public List<PsiClass> getClasses() {
    if (IMPORT_AUTO_CONFIGURATION.equals(myAnno)) {
      List<PsiClass> classes = ContainerUtil.mapNotNull(VALUE.getJam(myAnnotationRef), JamClassAttributeElement::getValue);
      if (!classes.isEmpty()) {
        return classes;
      }
      return ContainerUtil.mapNotNull(CLASSES.getJam(myAnnotationRef), JamClassAttributeElement::getValue);
    }
    ImportAutoConfiguration definingMetaAnnotation = myDefiningMetaAnnotation.getValue();
    if (definingMetaAnnotation != null) {
      return definingMetaAnnotation.getClasses();
    }
    return Collections.emptyList();
  }
}
