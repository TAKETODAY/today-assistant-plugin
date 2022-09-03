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

package cn.taketoday.assistant.model.config.jam;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;

import cn.taketoday.lang.Nullable;

public class StringLiteralPsiClassConverter extends JamConverter<PsiClass> {
  private static final JavaClassReferenceProvider JAVA_CLASS_REFERENCE_PROVIDER = new JavaClassReferenceProvider();

  static {
    JAVA_CLASS_REFERENCE_PROVIDER.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
  }

  @Nullable
  public PsiClass fromString(@Nullable String s, JamStringAttributeElement<PsiClass> context) {
    PsiAnnotationMemberValue psiElement;
    if (!StringUtil.isEmptyOrSpaces(s) && (psiElement = context.getPsiElement()) != null) {
      return JavaPsiFacade.getInstance(psiElement.getProject()).findClass(s, psiElement.getResolveScope());
    }
    return null;
  }

  public PsiReference[] createReferences(JamStringAttributeElement<PsiClass> context, PsiLanguageInjectionHost injectionHost) {
    String stringValue = context.getStringValue();
    if (stringValue == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    JavaClassReferenceSet set = new JavaClassReferenceSet(stringValue, injectionHost, ElementManipulators.getOffsetInElement(injectionHost), false, JAVA_CLASS_REFERENCE_PROVIDER) {
      public boolean isAllowDollarInNames() {
        return true;
      }
    };
    return set.getAllReferences();
  }
}
