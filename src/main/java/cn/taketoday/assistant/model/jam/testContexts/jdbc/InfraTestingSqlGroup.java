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
package cn.taketoday.assistant.model.jam.testContexts.jdbc;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

public class InfraTestingSqlGroup extends JamBaseElement<PsiMember> implements TestingSqlGroup {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = JAM_ANNOTATION_KEY.subKey("InfraTestingSqlGroup");
  private static final SemKey<InfraTestingSqlGroup> JAM_KEY = TestingSqlGroup.JAM_KEY.subKey("InfraTestingSqlGroup");

  public static final JamClassMeta<InfraTestingSqlGroup> CLASS_META = new JamClassMeta<>(null, InfraTestingSqlGroup.class, JAM_KEY);
  public static final JamMethodMeta<InfraTestingSqlGroup> METHOD_META = new JamMethodMeta<>(null, InfraTestingSqlGroup.class, JAM_KEY);

  public static final JamAnnotationAttributeMeta.Collection<InfraTestingSql> VALUE_ATTR_META =
          JamAttributeMeta.annoCollection(VALUE_ATTR_NAME, InfraTestingSql.ANNO_META, InfraTestingSql.class);

  public static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype()
          .addAttribute(VALUE_ATTR_META);

  public static final JamAnnotationMeta ANNO_META =
          new JamAnnotationMeta(AnnotationConstant.TEST_SQL_GROUP, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    CLASS_META.addAnnotation(ANNO_META);
    METHOD_META.addAnnotation(ANNO_META);
  }

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  @SuppressWarnings("unused")
  public InfraTestingSqlGroup(PsiMember psiMember) {
    super(PsiElementRef.real(psiMember));
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  @SuppressWarnings("unused")
  public InfraTestingSqlGroup(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true))));
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override
  public List<InfraTestingSql> getSqlAnnotations() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNO_META, VALUE_ATTR_META);
  }
}
