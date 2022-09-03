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

package cn.taketoday.assistant;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * for cn.taketoday.core.annotation.AliasFor
 */
public class InfraAliasFor extends JamBaseElement<PsiMethod> {
  public static final SemKey<InfraAliasFor> SEM_KEY = SemKey.createKey("TodayAliasFor");
  private static final JamClassAttributeMeta.Single ALIAS_FOR_CLASS_ATTR_META = JamAttributeMeta.singleClass("annotation");
  private static final JamStringAttributeMeta.Single<PsiMethod> ALIAS_FOR_ATTR_META = JamAttributeMeta.singleString("attribute", new AliasForAttributePsiMethodJamConverter());
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta("cn.taketoday.core.annotation.AliasFor")
          .addAttribute(ALIAS_FOR_CLASS_ATTR_META)
          .addAttribute(ALIAS_FOR_ATTR_META);

  public static final JamMethodMeta<InfraAliasFor> METHOD_META = new JamMethodMeta<>(null, InfraAliasFor.class, SEM_KEY)
          .addAnnotation(ANNO_META);

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public InfraAliasFor(PsiMethod psiMethod) {
    super(PsiElementRef.real(psiMethod));
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiMethod);
  }

  public InfraAliasFor(PsiAnnotation annotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(annotation, PsiMethod.class, true))));
    this.myPsiAnnotation = PsiElementRef.real(annotation);
  }

  public String getMethodName() {
    return getPsiElement().getName();
  }

  @Nullable
  private static PsiClass getAliasForAnnotationClass(PsiMethod method) {
    PsiClass forAnnotationClass = ANNO_META.getAttribute(method, ALIAS_FOR_CLASS_ATTR_META).getValue();
    return forAnnotationClass == null ? method.getContainingClass() : forAnnotationClass;
  }

  @Nullable
  public PsiClass getAnnotationClass() {
    return getAliasForAnnotationClass(this.getPsiElement());
  }

  public String getAttributeName() {
    JamStringAttributeElement<PsiMethod> attribute = ALIAS_FOR_ATTR_META.getJam(this.myPsiAnnotation);
    PsiAnnotationMemberValue psiElement = attribute.getPsiElement();
    return psiElement instanceof PsiLiteral ? attribute.getStringValue() : getMethodName();
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNO_META.getAnnotation(this.getPsiElement());
  }

  private static class AliasForAttributePsiMethodJamConverter extends JamSimpleReferenceConverter<PsiMethod> {

    public PsiMethod fromString(@Nullable String s, JamStringAttributeElement<PsiMethod> context) {
      PsiMethod[] var3 = getAliasForMethods(context);
      for (PsiMethod psiMethod : var3) {
        if (psiMethod.getName().equals(s)) {
          return psiMethod;
        }
      }

      return null;
    }

    private static PsiMethod[] getAliasForMethods(JamStringAttributeElement<PsiMethod> context) {

      PsiMethod psiMethod = getIdentifyingMethod(context);
      if (psiMethod != null) {
        PsiClass aliasForAnnotationClass = InfraAliasFor.getAliasForAnnotationClass(psiMethod);
        if (aliasForAnnotationClass != null) {
          return aliasForAnnotationClass.getAllMethods();
        }
      }

      return PsiMethod.EMPTY_ARRAY;
    }

    private static PsiMethod getIdentifyingMethod(JamStringAttributeElement<PsiMethod> context) {

      PsiAnnotationMemberValue contextPsiElement = context.getPsiElement();
      return contextPsiElement != null ? PsiTreeUtil.getParentOfType(contextPsiElement, PsiMethod.class) : null;
    }

    public Collection<PsiMethod> getVariants(JamStringAttributeElement<PsiMethod> context) {
      List<PsiMethod> methods = new ArrayList<>();
      PsiMethod identifyingMethod = getIdentifyingMethod(context);
      if (identifyingMethod != null) {
        PsiType returnType = identifyingMethod.getReturnType();
        PsiMethod[] var5 = getAliasForMethods(context);

        for (PsiMethod method : var5) {
          PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && containingClass.isAnnotationType()) {
            PsiType methodReturnType = method.getReturnType();
            if (returnType == null || methodReturnType == null || returnType.isAssignableFrom(methodReturnType)) {
              methods.add(method);
            }
          }
        }
      }

      return methods;
    }
  }
}
