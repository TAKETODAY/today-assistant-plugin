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

package cn.taketoday.assistant.references;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.uast.UastSmartPointer;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UNamedExpression;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.model.InfraInheritableQualifier;
import cn.taketoday.assistant.model.QualifierAttribute;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class UInfraQualifier extends CommonModelElement.PsiBase implements InfraInheritableQualifier {
  private final UastSmartPointer<UAnnotation> annotationAnchor;

  public UInfraQualifier(UAnnotation uAnnotation) {
    this.annotationAnchor = new UastSmartPointer<>(uAnnotation, UAnnotation.class);
  }

  public PsiElement getPsiElement() {
    UDeclaration uDeclaration;
    UElement uElement = this.annotationAnchor.getElement();
    if (uElement != null) {
      uDeclaration = UastUtils.getParentOfType(uElement, UDeclaration.class, true);
    }
    else {
      uDeclaration = null;
    }
    return uDeclaration.getSourcePsi();
  }

  @Override
  @Nullable
  public PsiClass getQualifierType() {
    UAnnotation it = this.annotationAnchor.getElement();
    if (it != null) {
      return InfraUastQualifierReferenceKt.findAnnotationClass$default(it, null, 2, null);
    }
    return null;
  }

  @Override
  @Nullable
  public String getQualifierValue() {
    UAnnotation element = this.annotationAnchor.getElement();
    if (element != null) {
      UExpression findAttributeValue = element.findAttributeValue(null);
      if (findAttributeValue != null) {
        return UastUtils.evaluateString(findAttributeValue);
      }
    }
    return null;
  }

  @Override
  public List<QualifierAttribute> getQualifierAttributes() {
    UAnnotation element = this.annotationAnchor.getElement();
    List<UNamedExpression> attributeValues = element != null ? element.getAttributeValues() : null;
    if (attributeValues == null) {
      attributeValues = CollectionsKt.emptyList();
    }
    ArrayList<QualifierAttribute> destination$iv$iv = new ArrayList<>();
    for (UNamedExpression it : attributeValues) {
      QualifierAttribute qualifierAttribute = (Intrinsics.areEqual(it.getName(), "value") || it.getName() == null) ? null : new QualifierAttribute() {
        @Override
        @Nullable
        public String getAttributeKey() {
          return it.getName();
        }

        @Override
        @Nullable
        public Object getAttributeValue() {
          return it.getExpression().evaluate();
        }
      };
      if (qualifierAttribute != null) {
        destination$iv$iv.add(qualifierAttribute);
      }
    }
    return destination$iv$iv;
  }
}
