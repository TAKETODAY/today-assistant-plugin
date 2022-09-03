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

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;

public class ComponentScanFilter extends CommonModelElement.PsiBase implements JamElement {

  private static final JamClassAttributeMeta.Collection VALUE_ATTR_META = JamAttributeMeta.classCollection("value");
  private static final JamClassAttributeMeta.Collection CLASSES_ATTR_META = JamAttributeMeta.classCollection("classes");
  private static final JamEnumAttributeMeta.Single<FilterType> FILTER_TYPE_ATTR_META =
          JamAttributeMeta.singleEnum("type", FilterType.class);

  public static final JamAnnotationMeta ANNOTATION_META =
          new JamAnnotationMeta(AnnotationConstant.COMPONENT_SCAN_FILTER)
                  .addAttribute(VALUE_ATTR_META)
                  .addAttribute(CLASSES_ATTR_META)
                  .addAttribute(FILTER_TYPE_ATTR_META);

  private final PsiMember myMember;
  private final PsiElementRef<PsiAnnotation> myAnnotation;

  @SuppressWarnings("unused")
  public ComponentScanFilter(PsiMember member) {
    myMember = member;
    myAnnotation = ANNOTATION_META.getAnnotationRef(member);
  }

  @SuppressWarnings("unused")
  public ComponentScanFilter(PsiAnnotation annotation) {
    myAnnotation = PsiElementRef.real(annotation);
    myMember = PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true);
  }

  public static final JamClassMeta<ComponentScanFilter> META =
          new JamClassMeta<>(ComponentScanFilter.class).addAnnotation(ANNOTATION_META);

  public Set<PsiClass> getFilteredClasses() {
    Set<PsiClass> result = new LinkedHashSet<>();

    addClasses(result, VALUE_ATTR_META);
    addClasses(result, CLASSES_ATTR_META);

    return result;
  }

  private void addClasses(Set<PsiClass> result, JamClassAttributeMeta.Collection meta) {
    for (JamClassAttributeElement element : meta.getJam(myAnnotation)) {
      PsiClass psiClass = element.getValue();
      if (psiClass != null) {
        result.add(psiClass);
      }
    }
  }

  public FilterType getFilterType() {
    FilterType filterType = FILTER_TYPE_ATTR_META.getJam(myAnnotation).getValue();
    if (filterType != null) {
      return filterType;
    }
    return FilterType.ANNOTATION;
  }

  @Override
  public PsiMember getPsiElement() {
    return myMember;
  }
}
