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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;

public class MVCViewConverter implements CustomReferenceConverter<PsiElement> {
  private static final InfraMVCViewReferenceProvider PROVIDER = new InfraMVCViewReferenceProvider(true);

  public PsiReference[] createReferences(GenericDomValue<PsiElement> psiElementGenericDomValue, PsiElement element, ConvertContext context) {
    PsiReference[] references = PROVIDER.getReferencesByElement(element, new ProcessingContext());
    return references.length == 0 ? PsiReference.EMPTY_ARRAY : new PsiReference[] { new PsiMultiReference(references, element) {
      public boolean isSoft() {
        return true;
      }
    } };
  }
}
