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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;

public class InfraCoreCantBeStaticExtension implements Condition<PsiElement> {
  private static final List<String> NON_STATIC_METHOD_ANNOTATIONS = List.of(AnnotationConstant.BEAN);

  public boolean value(PsiElement element) {
    if (element instanceof PsiMethod) {
      return AnnotationUtil.isAnnotated((PsiMethod) element, NON_STATIC_METHOD_ANNOTATIONS, 0);
    }
    return false;
  }
}
