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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import org.jetbrains.uast.UAnnotation;

import cn.taketoday.lang.Nullable;

public final class InfraUastQualifierReferenceKt {
  public static PsiClass findAnnotationClass$default(UAnnotation uAnnotation, PsiElement psiElement, int i, Object obj) {
    if ((i & 2) != 0) {
      psiElement = uAnnotation.getSourcePsi();
    }
    return findAnnotationClass(uAnnotation, psiElement);
  }

  @Nullable
  public static PsiClass findAnnotationClass(UAnnotation uAnnotation, @Nullable PsiElement context) {
    String annotationFQN = uAnnotation.getQualifiedName();
    if (annotationFQN == null || context == null) {
      return null;
    }
    return JavaPsiFacade.getInstance(context.getProject()).findClass(annotationFQN, context.getResolveScope());
  }
}
