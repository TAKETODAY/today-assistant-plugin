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

import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class PackageListConverter extends Converter<Collection<PsiPackage>> implements CustomReferenceConverter {

  @Override

  public Collection<PsiPackage> fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return Collections.emptyList();
    }
    if (context.getXmlElement() == null) {
      return Collections.emptyList();
    }

    if (InfraConverterUtil.containsPatternReferences(s)) {
      PsiReference[] references = createReferences((GenericDomValue) context.getInvocationElement(), context.getXmlElement(), context);
      return InfraConverterUtil.getPsiPackages(references);
    }

    return InfraConverterUtil.getPackages(s, getDelimiters(), context.getPsiManager().getProject());
  }

  @Override
  public String toString(@Nullable Collection<PsiPackage> psiPackages, ConvertContext context) {
    return null;
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    String text = genericDomValue.getStringValue();
    if (text == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    GlobalSearchScope scope = context.getSearchScope();
    return InfraReferenceUtils.getPsiPackagesReferences(element, text, ElementManipulators.getOffsetInElement(element), getDelimiters(),
            scope != null ? scope : GlobalSearchScope.allScope(element.getProject()));
  }

  protected String getDelimiters() {
    return ",; \n\t";
  }
}
