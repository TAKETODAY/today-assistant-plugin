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
package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.model.jam.testContexts.converters.ApplicationContextReferenceConverter;
import cn.taketoday.lang.Nullable;

public class CustomContextConfiguration extends JamBaseElement<PsiClass> implements ContextConfiguration {

  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = CONTEXT_CONFIGURATION_JAM_ANNOTATION_KEY
          .subKey("CustomContextConfiguration");

  public static final SemKey<CustomContextConfiguration> JAM_KEY =
          ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY.subKey("CustomContextConfiguration");
  public static final SemKey<JamMemberMeta<PsiClass, CustomContextConfiguration>> META_KEY =
          ContextConfiguration.CONTEXT_CONFIGURATION_META_KEY.subKey("CustomContextConfiguration");

  protected final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  private final AnnotationChildLink myAnnotationChildLink;

  protected static final JamClassAttributeMeta.Collection CLASSES_ATTR_META = new JamClassAttributeMeta.Collection(CLASSES_ATTR_NAME);
  protected static final JamClassAttributeMeta.Collection VALUE_ATTR_META = new JamClassAttributeMeta.Collection(VALUE_ATTR_NAME);
  private final NullableLazyValue<InfraContextConfiguration> myDefiningMetaAnnotation =
          new NullableLazyValue<>() {
            @Nullable
            @Override
            protected InfraContextConfiguration compute() {
              final PsiAnnotation definingMetaAnnotation = AliasForUtils
                      .findDefiningMetaAnnotation(getPsiElement(), myAnnotationChildLink.getAnnotationQualifiedName(),
                              AnnotationConstant.CONTEXT_CONFIGURATION, true);
              if (definingMetaAnnotation != null) {
                final PsiClass annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true);
                if (annotationType != null) {
                  return InfraContextConfiguration.META.getJamElement(annotationType);
                }
              }
              return null;
            }
          };

  public CustomContextConfiguration(String anno, PsiClass psiClassAnchor) {
    super(PsiElementRef.real(psiClassAnchor));
    myAnnotationChildLink = new AnnotationChildLink(anno);
    myPsiAnnotation = myAnnotationChildLink.createChildRef(getPsiElement());
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override

  public Set<XmlFile> getLocations(PsiClass... contexts) {
    Set<XmlFile> files = new LinkedHashSet<>();
    for (String attrName : XML_FILES_ATTRS) {
      InfraAliasFor aliasFor = getAliasAttribute(attrName);
      if (aliasFor != null) {
        JamStringAttributeMeta.Collection<List<XmlFile>> collection =
                JamAttributeMeta.collectionString(aliasFor.getMethodName(), new ApplicationContextReferenceConverter());
        for (JamStringAttributeElement<List<XmlFile>> element : collection.getJam(myPsiAnnotation)) {
          List<XmlFile> values = element.getValue();
          if (values != null)
            files.addAll(values);
        }
        return files;
      }
    }

    final ContextConfiguration definingContextConfiguration = getDefiningContextConfiguration();
    if (definingContextConfiguration != null) {
      return definingContextConfiguration.getLocations(definingContextConfiguration.getPsiElement());
    }

    return Collections.emptySet();
  }

  @Override

  public List<PsiClass> getConfigurationClasses() {
    List<PsiClass> psiClasses = new SmartList<>();
    InfraAliasFor aliasFor = getAliasAttribute(CLASSES_ATTR_NAME);
    if (aliasFor != null) {
      JamClassAttributeMeta.Collection collection = new JamClassAttributeMeta.Collection(aliasFor.getMethodName());
      for (JamClassAttributeElement classAttributeElement : collection.getJam(myPsiAnnotation)) {
        ContainerUtil.addIfNotNull(psiClasses, classAttributeElement.getValue());
      }
      return psiClasses;
    }

    final ContextConfiguration definingContextConfiguration = getDefiningContextConfiguration();
    if (definingContextConfiguration != null) {
      return definingContextConfiguration.getConfigurationClasses();
    }

    return Collections.emptyList();
  }

  private ContextConfiguration getDefiningContextConfiguration() {
    return myDefiningMetaAnnotation.getValue();
  }

  @Override
  public boolean hasLocationsAttribute() {
    final InfraAliasFor aliasAttribute = getAliasAttribute(LOCATIONS_ATTR_NAME);
    if (aliasAttribute != null)
      return true;

    final ContextConfiguration definingContextConfiguration = getDefiningContextConfiguration();
    return definingContextConfiguration != null && definingContextConfiguration.hasLocationsAttribute();
  }

  @Override
  public boolean hasValueAttribute() {
    final InfraAliasFor aliasAttribute = getAliasAttribute(VALUE_ATTR_NAME);
    if (aliasAttribute != null)
      return true;

    final ContextConfiguration definingContextConfiguration = getDefiningContextConfiguration();
    return definingContextConfiguration != null && definingContextConfiguration.hasValueAttribute();
  }

  @Nullable
  @Override
  public PsiClass getLoaderClass() {
    InfraAliasFor aliasFor = getAliasAttribute(LOADER_ATTR_NAME);
    if (aliasFor != null) {
      return JamAttributeMeta.singleClass(aliasFor.getMethodName()).getJam(myPsiAnnotation).getValue();
    }
    return null;
  }

  private InfraAliasFor getAliasAttribute(String attrName) {
    return AliasForUtils.findAliasFor(getPsiElement(),
            myAnnotationChildLink.getAnnotationQualifiedName(),
            AnnotationConstant.CONTEXT_CONFIGURATION,
            attrName);
  }

  @Override
  public List<JamStringAttributeElement<List<XmlFile>>> getLocationElements() {
    return ContainerUtil.concat(
            getLocations(getAliasAttribute(VALUE_ATTR_NAME)),
            getLocations(getAliasAttribute(LOCATIONS_ATTR_NAME))
    );
  }

  private List<JamStringAttributeElement<List<XmlFile>>> getLocations(@Nullable InfraAliasFor aliasFor) {
    if (aliasFor == null)
      return Collections.emptyList();

    JamStringAttributeMeta.Collection<List<XmlFile>> collection =
            new JamStringAttributeMeta.Collection<>(aliasFor.getMethodName(), new ApplicationContextReferenceConverter());

    return collection.getJam(myPsiAnnotation);
  }
}
