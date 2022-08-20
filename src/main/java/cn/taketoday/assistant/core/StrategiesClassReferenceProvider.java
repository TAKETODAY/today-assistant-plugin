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

package cn.taketoday.assistant.core;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
public final class StrategiesClassReferenceProvider extends PsiReferenceProvider {
  public static final String VALUE_DELIMITERS = ",\\ ";
  public static final StrategiesClassReferenceProvider INSTANCE = new StrategiesClassReferenceProvider();
  public static final JavaClassReferenceProvider CLASS_REFERENCE_PROVIDER = new JavaClassReferenceProvider();

  static {
    CLASS_REFERENCE_PROVIDER.setSoft(true);
    CLASS_REFERENCE_PROVIDER.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
  }

  private StrategiesClassReferenceProvider() { }

  @Override
  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    String text = element.getText();
    Ref<PsiReference[]> allReferences = Ref.create(PsiReference.EMPTY_ARRAY);
    (new DelimitedListProcessor(VALUE_DELIMITERS) {
      @Override
      protected void processToken(int start, int end, boolean delimitersOnly) {
        var classReferenceSet = new JavaClassReferenceSet(
                text.substring(start, end), element, start, true,
                StrategiesClassReferenceProvider.CLASS_REFERENCE_PROVIDER) {

          @Override
          public boolean isAllowDollarInNames() {
            return true;
          }
        };
        allReferences.set(ArrayUtil.mergeArrays(allReferences.get(), classReferenceSet.getAllReferences()));
      }
    }).processText(text);

    return allReferences.get();
  }

}
