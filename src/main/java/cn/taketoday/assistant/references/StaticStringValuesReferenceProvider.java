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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;

import java.util.Arrays;

/**
 * @author Yann C&eacute;bron
 */
public class StaticStringValuesReferenceProvider extends PsiReferenceProvider {

  private final boolean allowOtherValues;
  private final String[] values;

  /**
   * Creates a reference provider with the given values for autocompletion.
   * Other values will *not* be highlighted as errors.
   *
   * @param values Autocompletion values.
   */
  public StaticStringValuesReferenceProvider(String... values) {
    this(true, values);
  }

  /**
   * Creates a reference provider with the given values for autocompletion and optional error highlighting.
   *
   * @param allowOtherValues Set to false to enable error highlighting.
   * @param values Autocompletion values.
   */
  public StaticStringValuesReferenceProvider(boolean allowOtherValues, String... values) {
    this.allowOtherValues = allowOtherValues;
    Arrays.sort(values); // make sure Arrays.binarySearch() works later on..
    this.values = values;
  }

  @Override
  public PsiReference[] getReferencesByElement(PsiElement element,
          ProcessingContext context) {
    return new PsiReference[] { new PsiReferenceBase<>(element) {
      @Override
      public PsiElement resolve() {
        String myValue = getValue();
        if (allowOtherValues) {
          return myElement;
        }

        return Arrays.binarySearch(values, myValue) > -1 ? myElement : null;
      }

      @Override
      public Object[] getVariants() {
        return values;
      }
    } };
  }
}