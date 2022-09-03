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
package cn.taketoday.assistant.model.jam.converters;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;

import java.util.Collection;

import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.lang.Nullable;

public class PackageJamConverter extends JamConverter<Collection<PsiPackage>> {

  @Override
  public Collection<PsiPackage> fromString(@Nullable String s, JamStringAttributeElement<Collection<PsiPackage>> context) {
    if (StringUtil.isEmptyOrSpaces(s) || PlaceholderUtils.getInstance().isDefaultPlaceholder(s))
      return null;

    PsiLanguageInjectionHost psiLiteral = context.getLanguageInjectionHost();
    if (psiLiteral == null) {
      return calculatePackagesOnFakeElement(s, context.getPsiElement());
    }

    return createReferenceSet(psiLiteral, s).resolvePackage();
  }

  @Nullable
  private Collection<PsiPackage> calculatePackagesOnFakeElement(String s,
          @Nullable PsiElement context) {
    if (context == null)
      return null;
    final PsiExpression psiExpression =
            JavaPsiFacade.getElementFactory(context.getProject()).createExpressionFromText("\"" + s + "\"", context);
    if (psiExpression instanceof PsiLanguageInjectionHost) {
      return createReferenceSet((PsiLanguageInjectionHost) psiExpression, s).resolvePackage();
    }
    return null;
  }

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<Collection<PsiPackage>> context,
          PsiLanguageInjectionHost injectionHost) {
    final String value = context.getStringValue();
    if (value == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    return createReferenceSet(injectionHost, value).getPsiReferences();
  }

  private PackageReferenceSet createReferenceSet(PsiLanguageInjectionHost psiLiteral, String value) {
    return new InfraAntPatternPackageReferenceSet(value, psiLiteral, ElementManipulators.getOffsetInElement(psiLiteral),
            InfraReferenceUtils.getResolveScope(psiLiteral));
  }
}
