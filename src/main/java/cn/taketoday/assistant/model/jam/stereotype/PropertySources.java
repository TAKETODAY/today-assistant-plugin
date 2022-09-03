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
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;

public class PropertySources extends JamBaseElement<PsiClass> {
  public static final JamAnnotationAttributeMeta.Collection<JamPropertySource> VALUE_ATTRIBUTE =
          JamAttributeMeta.annoCollection("value", JamPropertySource.ANNO_META, JamPropertySource.class);

  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.PROPERTY_SOURCES).addAttribute(
          VALUE_ATTRIBUTE);

  public static final JamClassMeta<PropertySources> META = new JamClassMeta<>(PropertySources.class).addAnnotation(ANNO_META);

  public PropertySources(PsiClass psiClass) {
    super(PsiElementRef.real(psiClass));
  }

  public PropertySources(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true))));
  }

  public List<JamPropertySource> getPropertySources() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNO_META, VALUE_ATTRIBUTE);
  }
}
