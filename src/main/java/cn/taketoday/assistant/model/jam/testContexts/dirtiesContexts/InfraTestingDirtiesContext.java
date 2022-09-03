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
package cn.taketoday.assistant.model.jam.testContexts.dirtiesContexts;

import com.intellij.jam.JamElement;
import com.intellij.jam.JamEnumAttributeElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

public class InfraTestingDirtiesContext implements JamElement {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = JamService.ANNO_META_KEY.subKey("InfraTestingDirtiesContext");
  public static final SemKey<InfraTestingDirtiesContext> JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("InfraTestingDirtiesContext");

  public static final JamClassMeta<InfraTestingDirtiesContext> CLASS_META =
          new JamClassMeta<>(null, InfraTestingDirtiesContext.class, JAM_KEY);
  public static final JamMethodMeta<InfraTestingDirtiesContext> METHOD_META =
          new JamMethodMeta<>(null, InfraTestingDirtiesContext.class, JAM_KEY);

  public static final JamAttributeMeta<JamEnumAttributeElement<MethodMode>> METHOD_MODE_ATTR_META =
          JamAttributeMeta.singleEnum("methodMode", MethodMode.class);
  public static final JamAttributeMeta<JamEnumAttributeElement<ClassMode>> CLASS_MODE_ATTR_META =
          JamAttributeMeta.singleEnum("classMode", ClassMode.class);
  public static final JamAttributeMeta<JamEnumAttributeElement<HierarchyMode>> HIERARCHY_MODE_ATTR_META =
          JamAttributeMeta.singleEnum("hierarchyMode", HierarchyMode.class);

  public static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype()
          .addAttribute(METHOD_MODE_ATTR_META)
          .addAttribute(CLASS_MODE_ATTR_META)
          .addAttribute(HIERARCHY_MODE_ATTR_META);

  public static final JamAnnotationMeta ANNO_META =
          new JamAnnotationMeta(AnnotationConstant.DIRTIES_CONTEXT, ARCHETYPE, JAM_ANNO_META_KEY);

  static {
    CLASS_META.addAnnotation(ANNO_META);
    METHOD_META.addAnnotation(ANNO_META);
  }

  private final PsiAnchor myPsiMemberAnchor;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  @SuppressWarnings("unused")
  public InfraTestingDirtiesContext(PsiMember psiMember) {
    myPsiMemberAnchor = PsiAnchor.create(psiMember);
    myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  @SuppressWarnings("unused")
  public InfraTestingDirtiesContext(PsiAnnotation annotation) {
    PsiClass psiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true);
    myPsiMemberAnchor = psiClass != null ? PsiAnchor.create(psiClass) : null;
    myPsiAnnotation = PsiElementRef.real(annotation);
  }

  public ClassMode getClassMode() {
    final ClassMode classMode = getClassModeElement().getValue();
    if (classMode != null) {
      return classMode;
    }
    return ClassMode.AFTER_CLASS;
  }

  public MethodMode getMethodMode() {
    final MethodMode methodMode = getMethodModeElement().getValue();
    if (methodMode != null) {
      return methodMode;
    }
    return MethodMode.AFTER_METHOD;
  }

  public HierarchyMode getHierarchyMode() {
    final HierarchyMode methodMode = getHierarchyModeElement().getValue();
    if (methodMode != null) {
      return methodMode;
    }
    return HierarchyMode.EXHAUSTIVE;
  }

  public JamEnumAttributeElement<ClassMode> getClassModeElement() {
    return CLASS_MODE_ATTR_META.getJam(myPsiAnnotation);
  }

  public JamEnumAttributeElement<MethodMode> getMethodModeElement() {
    return METHOD_MODE_ATTR_META.getJam(myPsiAnnotation);
  }

  public JamEnumAttributeElement<HierarchyMode> getHierarchyModeElement() {
    return HIERARCHY_MODE_ATTR_META.getJam(myPsiAnnotation);
  }

  public @Nullable PsiMember getPsiElement() {
    return myPsiMemberAnchor != null ? (PsiMember) myPsiMemberAnchor.retrieveOrThrow() : null;
  }

  public @Nullable PsiAnnotation getAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    InfraTestingDirtiesContext context = (InfraTestingDirtiesContext) o;
    return Objects.equals(myPsiMemberAnchor, context.myPsiMemberAnchor) &&
            Objects.equals(myPsiAnnotation, context.myPsiAnnotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myPsiMemberAnchor, myPsiAnnotation);
  }
}
