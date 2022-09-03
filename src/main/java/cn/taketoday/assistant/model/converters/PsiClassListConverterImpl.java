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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.PsiClassConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.lang.Nullable;

public class PsiClassListConverterImpl extends PsiClassListConverter {

  public Collection<PsiClass> fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return Collections.emptyList();
    }
    else if (context.getXmlElement() == null) {
      return Collections.emptyList();
    }
    else {
      PsiReference[] psiReferences = createReferences((GenericDomValue) context.getInvocationElement(), context.getXmlElement(), context);
      Collection<PsiClass> list = new LinkedHashSet<>();
      for (PsiReference psiReference : psiReferences) {
        PsiElement resolve = psiReference.resolve();
        if (resolve instanceof PsiClass psiClass) {
          list.add(psiClass);
        }
      }
      return list;
    }
  }

  public String toString(@Nullable Collection<PsiClass> psiPackages, ConvertContext context) {
    return "";
  }

  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    String text = genericDomValue.getStringValue();
    if (text == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    int startInElement = ElementManipulators.getOffsetInElement(element);
    ArrayList<PsiReference> list = new ArrayList<>();
    JavaClassReferenceProvider referenceProvider = PsiClassConverter.createJavaClassReferenceProvider(
            genericDomValue, genericDomValue.getAnnotation(ExtendClass.class),
            new JavaClassReferenceProvider() {
              public GlobalSearchScope getScope(Project project) {
                return context.getSearchScope();
              }
            });
    new DelimitedListProcessor(",; \n\t") {
      protected void processToken(int start, int end, boolean delimitersOnly) {
        String substring = text.substring(start, end);
        PsiReference[] references = referenceProvider.getReferencesByString(substring, element, startInElement + start);
        list.addAll(Arrays.asList(references));
      }
    }.processText(text);
    return list.toArray(PsiReference.EMPTY_ARRAY);
  }
}
