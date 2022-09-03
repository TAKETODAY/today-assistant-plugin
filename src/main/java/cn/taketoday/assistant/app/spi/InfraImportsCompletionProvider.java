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

package cn.taketoday.assistant.app.spi;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spi.psi.SPIClassProviderReferenceElement;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.core.StrategiesClassReferenceProvider;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

public final class InfraImportsCompletionProvider extends CompletionProvider<CompletionParameters> {

  protected void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    PsiElement psiElement = PsiTreeUtil.getParentOfType(parameters.getPosition(), SPIClassProviderReferenceElement.class);
    if (psiElement != null) {
      String text = psiElement.getText();
      int offset = parameters.getOffset();
      TextRange textRange = psiElement.getTextRange();
      String referenceText = text.substring(0, offset - textRange.getStartOffset());
      JavaClassReferenceProvider javaClassReferenceProvider = StrategiesClassReferenceProvider.CLASS_REFERENCE_PROVIDER;
      JavaClassReference[] references = new JavaClassReferenceSet(referenceText, psiElement, 0, true, javaClassReferenceProvider) {
        public boolean isAllowDollarInNames() {
          return true;
        }
      }.getAllReferences();
      if (references.length == 0) {
        return;
      }
      JavaClassReference reference = references[references.length - 1];
      String prefix = getPrefix(referenceText);
      for (Object variant : reference.getVariants()) {
        if (variant instanceof LookupItem) {
          ((LookupItem) variant).addLookupStrings(prefix + ((LookupItem) variant).getLookupString());
          result.addElement((LookupElement) variant);
        }
        else if (variant instanceof LookupElementBuilder) {
          result.addElement(prepareBuilder((LookupElementBuilder) variant, prefix));
        }
        else if (variant instanceof PsiNamedElement) {
          LookupElementBuilder withIcon = LookupElementBuilder.create((PsiNamedElement) variant).withIcon(((PsiNamedElement) variant).getIcon(0));
          result.addElement(prepareBuilder(withIcon, prefix));
        }
      }
      result.stopHere();
    }
  }

  private String getPrefix(String text) {
    int index = Math.max(StringsKt.lastIndexOf(text, '.', 0, false), StringsKt.lastIndexOf(text, '$', 0, false));
    if (index >= 0) {
      String substring = text.substring(0, index + 1);
      Intrinsics.checkNotNullExpressionValue(substring, "this as java.lang.String…ing(startIndex, endIndex)");
      return substring;
    }
    return "";
  }

  private LookupElementBuilder prepareBuilder(LookupElementBuilder builder, String prefix) {
    LookupElementBuilder withLookupString = builder.withLookupString(prefix + builder.getLookupString());
    Intrinsics.checkNotNullExpressionValue(withLookupString, "builder.withLookupString…${builder.lookupString}\")");
    return withLookupString;
  }
}
