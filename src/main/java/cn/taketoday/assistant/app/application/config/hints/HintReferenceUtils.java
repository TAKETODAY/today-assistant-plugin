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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PropertyUtilBase;

import cn.taketoday.lang.Nullable;

final class HintReferenceUtils {

  HintReferenceUtils() {
  }

  @Nullable
  static PsiAnnotation findAnnotationOnConfigPropertyField(MetaConfigKey configKey, String annotationFqn) {
    if (configKey.getDeclarationResolveResult() != MetaConfigKey.DeclarationResolveResult.PROPERTY) {
      return null;
    }
    PsiElement navigationElement = configKey.getDeclaration().getNavigationElement();
    if (!(navigationElement instanceof PsiMethod propertyMethod)) {
      return null;
    }
    String propertyName = PropertyUtilBase.getPropertyName(propertyMethod, true);
    PsiClass propertyClass = propertyMethod.getContainingClass();
    PsiField field = propertyClass.findFieldByName(propertyName, true);
    if (field != null) {
      return AnnotationUtil.findAnnotation(field, true, annotationFqn);
    }
    return null;
  }
}
