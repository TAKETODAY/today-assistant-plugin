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

import com.intellij.microservices.jvm.config.hints.HintReferenceProviderBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.app.spi.InfraSpiClassesListJamConverter;
import cn.taketoday.assistant.core.StrategiesClassesListJamConverter;

class InfraSpiClassesProvider extends HintReferenceProviderBase {
  private final String myKey;
  private final String myImportKey;

  InfraSpiClassesProvider(String key, String importKey) {
    this.myKey = key;
    this.myImportKey = importKey;
  }

  protected PsiReference createReference(PsiElement element, TextRange textRange, ProcessingContext context) {
    String elementText = element.getText();
    TextRange trimmedRange = getTrimmedRange(textRange, elementText);
    String trimmedText = trimmedRange.substring(elementText);
    if (this.myImportKey != null) {
      return new InfraSpiClassesListJamConverter.InfraSpiClassReference(this.myKey, this.myImportKey, element, trimmedRange, trimmedText);
    }
    return new StrategiesClassesListJamConverter.ClassReference(this.myKey, element, trimmedRange, trimmedText);
  }

  private static TextRange getTrimmedRange(TextRange textRange, String elementText) {
    String text = textRange.substring(elementText);
    int offset = 0;
    while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) {
      offset++;
    }
    int length = text.length();
    if (offset != length) {
      while (length > 0 && Character.isWhitespace(text.charAt(length - 1))) {
        length--;
      }
    }
    return TextRange.from(textRange.getStartOffset() + offset, length - offset);
  }
}
