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

import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.hints.HintReferenceProviderBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;

import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

class RegExReferenceProvider extends HintReferenceProviderBase {
  private final Pattern myPattern;

  RegExReferenceProvider(String regExPattern) {
    this.myPattern = Pattern.compile(regExPattern);
  }

  protected PsiReference createReference(PsiElement element, TextRange textRange, ProcessingContext context) {
    return new HintReferenceBase(element, textRange) {

      @Nullable
      protected PsiElement doResolve() {
        if (RegExReferenceProvider.this.myPattern.matcher(getValue()).matches()) {
          return getElement();
        }
        return null;
      }

      public String getUnresolvedMessagePattern() {
        String str = "Value ''{0}'' does not match pattern ''" + RegExReferenceProvider.this.myPattern + "''";
        return str;
      }
    };
  }
}
