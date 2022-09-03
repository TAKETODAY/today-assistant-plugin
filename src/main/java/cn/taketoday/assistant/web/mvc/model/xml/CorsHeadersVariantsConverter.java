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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ImmutableList;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;
import com.intellij.xml.util.HtmlUtil;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Nullable;

public class CorsHeadersVariantsConverter extends DelimitedListConverter<String> {
  private static final ImmutableList<String> KNOWN_HEADERS = ContainerUtil.immutableList(HtmlUtil.RFC2616_HEADERS);

  public CorsHeadersVariantsConverter() {
    super(", ");
  }

  @Nullable
  public String convertString(@Nullable String string, ConvertContext context) {
    return string;
  }

  @Override
  @Nullable
  public String toString(@Nullable String s) {
    return s;
  }

  protected Object[] getReferenceVariants(ConvertContext context, GenericDomValue<? extends List<String>> genericDomValue) {
    return getVariants(false);
  }

  public static Object[] getVariants(boolean includeAll) {
    List<LookupElement> variants = new ArrayList<>(KNOWN_HEADERS.size());
    for (String header : KNOWN_HEADERS) {
      variants.add(LookupElementBuilder.create(header));
    }
    if (includeAll) {
      variants.add(LookupElementBuilder.create("*").withTailText(" (all)", true));
    }
    return ArrayUtil.toObjectArray(variants);
  }

  @Override
  @Nullable
  public PsiElement resolveReference(@Nullable String s, ConvertContext context) {
    return context.getXmlElement();
  }

  protected String getUnresolvedMessage(String value) {
    return null;
  }

  public static class WithAnyVariant extends CorsHeadersVariantsConverter {

    @Override
    protected Object[] getReferenceVariants(ConvertContext context, GenericDomValue<? extends List<String>> genericDomValue) {
      return getVariants(true);
    }
  }
}
