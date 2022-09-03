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

package cn.taketoday.assistant.model.converters;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.impl.GenericDomValueReference;

import java.util.Collection;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.lang.Nullable;

public class InfraCompletionContributor extends CompletionContributor {

  public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet outer) {
    if (parameters.getCompletionType() != CompletionType.SMART) {
      return;
    }
    ApplicationManager.getApplication().runReadAction(() -> {
      GenericDomValueReference<?> reference = getReference(parameters);

      if (reference != null) {
        if (reference.getConverter() instanceof InfraBeanResolveConverter converter) {
          Collection<BeanPointer<?>> variants = converter.getVariants(reference.getConvertContext());
          for (BeanPointer<?> variant : variants) {
            LookupElement element = InfraConverterUtil.createCompletionVariant(variant);
            if (element != null) {
              outer.addElement(element);
            }
          }
        }
      }

    });
  }

  @Nullable
  private static GenericDomValueReference<?> getReference(CompletionParameters parameters) {
    if (!(parameters.getPosition() instanceof XmlToken)) {
      return null;
    }
    for (PsiReference psiReference : getReferences(parameters)) {
      if (psiReference instanceof GenericDomValueReference<?> domValueReference) {
        Converter<?> converter = domValueReference.getConverter();
        if (converter instanceof InfraBeanResolveConverter) {
          return domValueReference;
        }
      }
    }
    return null;
  }

  private static PsiReference[] getReferences(CompletionParameters parameters) {
    PsiElement psiElement = parameters.getPosition().getParent();
    return psiElement instanceof XmlText ? psiElement.getParent().getReferences() : psiElement.getReferences();
  }
}
