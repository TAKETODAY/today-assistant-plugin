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

package cn.taketoday.assistant.model.jam.utils;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.JamReferenceContributorKt;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.util.text.PlaceholderTextRanges;

import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.expressions.UInjectionHost;

import java.util.Arrays;

import cn.taketoday.assistant.model.values.PlaceholderUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public final class InfraResourceLocationsUtil {

  public static final InfraResourceLocationsUtil INSTANCE = new InfraResourceLocationsUtil();

  public void checkResourceLocation(ProblemsHolder holder, JamStringAttributeElement<?> jamStringAttributeElement) {
    String message;
    PsiAnnotationMemberValue value = jamStringAttributeElement.getPsiElement();

    if (value instanceof PsiLiteral psiLiteral) {
      for (PsiReference psiReference : psiLiteral.getReferences()) {
        PsiReference unwrapReference = JamReferenceContributorKt.unwrapReference(psiReference);
        if (!unwrapReference.isSoft() && (unwrapReference instanceof FileReference fileReference)) {
          ResolveResult[] multiResolve = fileReference.multiResolve(false);
          if (multiResolve.length == 0) {
            UInjectionHost uInjectionHost = UastContextKt.toUElement(psiLiteral, UInjectionHost.class);
            PsiElement psiElement = uInjectionHost != null ? uInjectionHost.getSourcePsi() : null;
            if (psiElement != null) {
              String pathValue = uInjectionHost.evaluateToString();
              if (pathValue != null && !PlaceholderTextRanges.getPlaceholderRanges(pathValue, PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX, PlaceholderUtils.DEFAULT_PLACEHOLDER_SUFFIX).isEmpty()) {
                return;
              }
              if (fileReference.isLast()) {
                message = message("model.file.error.message", fileReference.getText());
              }
              else {
                message = message("model.directory.error.message", fileReference.getText());
              }
              String message2 = message;
              TextRange rangeInElement = unwrapReference.getRangeInElement();
              LocalQuickFix[] quickFixes = fileReference.getQuickFixes();
              holder.registerProblem(psiElement, rangeInElement, message2, Arrays.copyOf(quickFixes, quickFixes.length));
            }
          }
        }
      }
    }
  }
}
