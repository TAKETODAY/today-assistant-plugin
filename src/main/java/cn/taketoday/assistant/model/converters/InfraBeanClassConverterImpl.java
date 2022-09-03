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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.values.PlaceholderUtils;

public class InfraBeanClassConverterImpl extends InfraBeanClassConverter {

  public PsiClass fromString(String s, ConvertContext context) {
    DomElement invocationElement = context.getInvocationElement();
    if ((invocationElement instanceof GenericDomValue domValue) && PlaceholderUtils.getInstance().isRawTextPlaceholder(domValue)) {
      return DomJavaUtil.findClass("java.lang.Object", context.getInvocationElement());
    }
    return super.fromString(s, context);
  }

  public PsiReference[] createReferences(GenericDomValue<PsiClass> value, PsiElement element, ConvertContext context) {
    if (PlaceholderUtils.getInstance().isRawTextPlaceholder(value)) {
      return PsiReference.EMPTY_ARRAY;
    }
    return super.createReferences(value, element, context);
  }

  protected JavaClassReferenceProvider createClassReferenceProvider(
          GenericDomValue<PsiClass> genericDomValue, ConvertContext context, ExtendClass extendClass) {
    JavaClassReferenceProvider provider = super.createClassReferenceProvider(genericDomValue, context, extendClass);
    provider.setOption(JavaClassReferenceProvider.ALLOW_DOLLAR_NAMES, Boolean.TRUE);
    return provider;
  }
}
