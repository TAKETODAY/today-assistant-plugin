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

import com.intellij.codeInsight.ExpectedTypeInfo;
import com.intellij.codeInsight.ExpectedTypesProvider;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.ProcessingContext;

import cn.taketoday.lang.Nullable;

public class InfraBeanNamesReferenceProvider extends PsiReferenceProvider {

  public static final String[] METHODS = {
          "containsBean", "getBean", "isSingleton", "getType", "getAliases"
  };

  @Override
  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    if (element instanceof final PsiLiteral literalExpression) {
      if (literalExpression.getValue() instanceof String) {
        return new PsiReference[] {
                new InfraBeanReference(literalExpression,
                        ElementManipulators.getValueTextRange(element),
                        determineRequiredClass(element),
                        false)
        };
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }

  /**
   * Tries to determine required bean class by analyzing surrounding expression.
   *
   * @param element Current element.
   * @return Expected bean class or {@code null} if not determinable.
   */
  @Nullable
  public static PsiClass determineRequiredClass(PsiElement element) {
    PsiExpression expression = PsiTreeUtil.getParentOfType(element, PsiExpression.class);
    if (expression == null) {
      return null;
    }

    ExpectedTypeInfo[] types = ExpectedTypesProvider.getExpectedTypes(expression, true);
    if (types.length != 1) {
      return null;
    }

    PsiType type = types[0].getType();
    return PsiTypesUtil.getPsiClass(type);
  }
}
