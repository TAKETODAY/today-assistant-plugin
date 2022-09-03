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
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.app.spi.InfraSpiClassesListJamConverter;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant.ENABLE_AUTO_CONFIGURATION;

public class EnableAutoConfiguration extends JamBaseElement<PsiClass> {

  private static final JamClassAttributeMeta.Collection EXCLUDE =
          new JamClassAttributeMeta.Collection("exclude");

  private static final JamStringAttributeMeta.Collection<PsiClass> EXCLUDE_NAME =
          JamAttributeMeta.collectionString("excludeName",
                  new InfraSpiClassesListJamConverter(ENABLE_AUTO_CONFIGURATION,
                          InfraConfigConstant.AUTO_CONFIGURATION));

  static final JamAnnotationArchetype EXCLUDE_ARCHETYPE =
          new JamAnnotationArchetype().addAttribute(EXCLUDE).addAttribute(EXCLUDE_NAME);

  private static final JamAnnotationMeta ANNOTATION_META =
          new JamAnnotationMeta(ENABLE_AUTO_CONFIGURATION, EXCLUDE_ARCHETYPE);

  public static final SemKey<EnableAutoConfiguration> JAM_KEY =
          JamService.JAM_ELEMENT_KEY.subKey("EnableAutoConfigSemKey");

  public static final JamClassMeta<EnableAutoConfiguration> META =
          new JamClassMeta<>(null, EnableAutoConfiguration.class, JAM_KEY)
                  .addAnnotation(ANNOTATION_META);

  public static final SemKey<JamMemberMeta<PsiClass, EnableAutoConfiguration>> META_KEY =
          JamService.MEMBER_META_KEY.subKey("EnableAutoConfigurationMeta");

  private final String myAnno;
  private final PsiElementRef<PsiAnnotation> myAnnotationRef;

  private static final Map<String, JamAnnotationMeta> ourJamAnnotationMetaMap =
          ConcurrentFactoryMap.createMap(key -> new JamAnnotationMeta(key, EXCLUDE_ARCHETYPE));

  @SuppressWarnings("unused")
  public EnableAutoConfiguration(PsiClass psiClass) {
    this(ENABLE_AUTO_CONFIGURATION, psiClass);
  }

  public EnableAutoConfiguration(@Nullable String anno, PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
    myAnno = anno;
    JamAnnotationMeta annotationMeta = ourJamAnnotationMetaMap.get(anno);
    myAnnotationRef = annotationMeta.getAnnotationRef(psiClass);
  }

  public static Function<Module, Collection<String>> getAnnotations() {
    return SemContributorUtil.getCustomMetaAnnotations(ENABLE_AUTO_CONFIGURATION);
  }

  public String getAnnotationFqn() {
    return myAnno;
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myAnnotationRef.getPsiElement();
  }

  public List<PsiClass> getExcludes() {
    ArrayList<PsiClass> allExcludes = new ArrayList<>();

    allExcludes.addAll(ContainerUtil.mapNotNull(EXCLUDE.getJam(myAnnotationRef), JamClassAttributeElement::getValue));
    allExcludes.addAll(ContainerUtil.mapNotNull(EXCLUDE_NAME.getJam(myAnnotationRef), JamStringAttributeElement::getValue));
    return allExcludes;
  }

  @Nullable
  public static EnableAutoConfiguration find(PsiElement element) {
    return JamService.getJamService(element.getProject()).getJamElement(element, META);
  }

}
