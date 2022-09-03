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
package cn.taketoday.assistant.model.jam.testContexts.propertySources;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.model.jam.converters.PropertiesFileConverter;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.lang.Nullable;

public class CustomTestPropertySource implements PropertySource {

  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = JamService.ANNO_META_KEY.subKey("CustomTestPropertySource");
  public static final SemKey<CustomTestPropertySource> JAM_KEY =
          PropertySource.PROPERTY_SOURCE_JAM_KEY.subKey("CustomTestPropertySource");
  public static final SemKey<JamMemberMeta<PsiClass, CustomTestPropertySource>> META_KEY = PROPERTY_SOURCE_META_KEY.subKey("CustomTestPropertySource");

  String[] LOCATIONS_FILES_ATTRS = { "locations", "value" };

  protected final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final AnnotationChildLink myAnnotationChildLink;
  private final PsiAnchor myPsiClassAnchor;

  private final NullableLazyValue<TestPropertySource> myDefiningMetaAnnotation =
          new NullableLazyValue<>() {
            @Nullable
            @Override
            protected TestPropertySource compute() {
              PsiAnnotation definingMetaAnnotation = AliasForUtils
                      .findDefiningMetaAnnotation(getPsiElement(), myAnnotationChildLink.getAnnotationQualifiedName(),
                              TestPropertySource.ANNO_META.getAnnoName());
              if (definingMetaAnnotation != null) {
                PsiClass annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true);
                if (annotationType != null) {
                  return TestPropertySource.META.getJamElement(annotationType);
                }
              }
              return null;
            }
          };

  public CustomTestPropertySource(String anno, PsiClass psiClassAnchor) {
    myAnnotationChildLink = new AnnotationChildLink(anno);
    myPsiClassAnchor = PsiAnchor.create(psiClassAnchor);
    myPsiAnnotation = myAnnotationChildLink.createChildRef(getPsiElement());
  }

  @Override
  public PsiClass getPsiElement() {
    return (PsiClass) myPsiClassAnchor.retrieve();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override
  public List<JamStringAttributeElement<Set<PropertiesFile>>> getLocationElements() {
    List<JamStringAttributeElement<Set<PropertiesFile>>> result = new SmartList<>();

    JamConverter<Set<PropertiesFile>> referenceConverter = new PropertiesFileConverter();
    for (String attrName : LOCATIONS_FILES_ATTRS) {
      InfraAliasFor aliasFor = getAliasAttribute(attrName);
      if (aliasFor != null) {
        JamStringAttributeMeta.Collection<Set<PropertiesFile>> element =
                JamAttributeMeta.collectionString(aliasFor.getMethodName(), referenceConverter);
        result.addAll(element.getJam(myPsiAnnotation));
      }
    }

    return result;
  }

  @Override
  public boolean isIgnoreResourceNotFound() {
    InfraAliasFor ignoreResourceAttr = getAliasAttribute(IGNORE_RESOURCE_NOT_FOUND_ATTR_NAME);
    if (ignoreResourceAttr != null) {
      JamBooleanAttributeMeta attributeMeta = JamAttributeMeta.singleBoolean(IGNORE_RESOURCE_NOT_FOUND_ATTR_NAME, false);
      return attributeMeta.getJam(myPsiAnnotation).getValue();
    }

    return false;
  }

  @Override
  public Set<PropertiesFile> getPropertiesFiles() {
    Set<PropertiesFile> files = new LinkedHashSet<>();

    JamConverter<Set<PropertiesFile>> referenceConverter = new PropertiesFileConverter();
    for (String attrName : LOCATIONS_FILES_ATTRS) {
      InfraAliasFor aliasFor = getAliasAttribute(attrName);
      if (aliasFor != null) {
        JamStringAttributeMeta.Collection<Set<PropertiesFile>> element = JamAttributeMeta.collectionString(aliasFor.getMethodName(), referenceConverter);
        for (JamStringAttributeElement<Set<PropertiesFile>> attributeElement : element.getJam(myPsiAnnotation)) {
          Set<PropertiesFile> values = attributeElement.getValue();
          if (values != null)
            files.addAll(values);
        }
        return files;
      }
    }

    TestPropertySource testPropertySource = getDefiningTestPropertySource();
    if (testPropertySource != null) {
      return testPropertySource.getPropertiesFiles();
    }
    return Collections.emptySet();
  }

  private TestPropertySource getDefiningTestPropertySource() {
    return myDefiningMetaAnnotation.getValue();
  }

  private InfraAliasFor getAliasAttribute(String attrName) {
    return AliasForUtils.findAliasFor(getPsiElement(),
            myAnnotationChildLink.getAnnotationQualifiedName(),
            TestPropertySource.ANNO_META.getAnnoName(),
            attrName);
  }

  @Override
  public boolean isPsiValid() {
    return getPsiElement().isValid();
  }
}