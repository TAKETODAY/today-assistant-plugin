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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.converters.PropertiesFileConverter;
import cn.taketoday.lang.Nullable;

public class JamPropertySource extends JamBaseElement<PsiClass> implements PropertySource {

  public static final SemKey<JamPropertySource> REPEATABLE_ANNO_JAM_KEY =
          PropertySource.PROPERTY_SOURCE_JAM_KEY.subKey("SpringJamPropertySource");

  private static final JamStringAttributeMeta.Collection<Set<PropertiesFile>> VALUE_ATTR_META =
          JamAttributeMeta.collectionString("value", new PropertiesFileConverter());

  private static final JamBooleanAttributeMeta IGNORE_RESOURCE_NOT_FOUND_ATTR_META =
          JamAttributeMeta.singleBoolean(IGNORE_RESOURCE_NOT_FOUND_ATTR_NAME, false);

  static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.PROPERTY_SOURCE);

  public static final JamClassMeta<JamPropertySource> META
          = new JamClassMeta<>(null, JamPropertySource.class, REPEATABLE_ANNO_JAM_KEY);

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
    ANNO_META.addAttribute(IGNORE_RESOURCE_NOT_FOUND_ATTR_META);
  }

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public JamPropertySource(PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  public JamPropertySource(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true))));
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNO_META.getAnnotation(getPsiElement());
  }

  @Override
  public List<JamStringAttributeElement<Set<PropertiesFile>>> getLocationElements() {
    return VALUE_ATTR_META.getJam(myPsiAnnotation);
  }

  @Override
  public boolean isIgnoreResourceNotFound() {
    return IGNORE_RESOURCE_NOT_FOUND_ATTR_META.getJam(myPsiAnnotation).getValue();
  }

  @Override
  public Set<PropertiesFile> getPropertiesFiles() {
    Set<PropertiesFile> propertiesFiles = new LinkedHashSet<>();

    List<JamStringAttributeElement<Set<PropertiesFile>>> attributeValues = VALUE_ATTR_META.getJam(myPsiAnnotation);

    for (JamStringAttributeElement<Set<PropertiesFile>> attributeElement : attributeValues) {
      Set<PropertiesFile> value = attributeElement.getValue();
      if (value != null)
        propertiesFiles.addAll(value);
    }

    return propertiesFiles;
  }

  @Override
  public boolean isPsiValid() {
    return isValid();
  }
}
