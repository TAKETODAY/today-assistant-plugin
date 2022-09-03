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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

public class CorsMappingAllowedMethodsConverter extends DelimitedListConverter<RequestMethod> {
  private static final String ANY_METHOD = "*";
  private static final EnumSet<RequestMethod> DEFAULT_REQUEST_METHODS = EnumSet.of(RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST);

  public CorsMappingAllowedMethodsConverter() {
    super(",");
  }

  @Nullable
  public RequestMethod convertString(@Nullable String string, ConvertContext context) {
    RequestMethod[] values;
    String value = StringUtil.trim(string);
    if (ANY_METHOD.equals(value)) {
      return RequestMethod.GET;
    }
    for (RequestMethod method : RequestMethod.values()) {
      if (method.name().equals(value)) {
        return method;
      }
    }
    return null;
  }

  @Nullable
  public String toString(@Nullable RequestMethod method) {
    if (method != null) {
      return method.name();
    }
    return null;
  }

  protected Object[] getReferenceVariants(ConvertContext context, GenericDomValue<? extends List<RequestMethod>> genericDomValue) {
    return getVariants();
  }

  public static Object[] getVariants() {
    List<LookupElement> variants = new ArrayList<>(RequestMethod.values().length);
    for (RequestMethod method : RequestMethod.values()) {
      boolean isEnabledByDefault = DEFAULT_REQUEST_METHODS.contains(method);
      variants.add(LookupElementBuilder.create(method.name()).withBoldness(isEnabledByDefault));
    }
    variants.add(LookupElementBuilder.create(ANY_METHOD).withTailText(" (all)", true));
    return ArrayUtil.toObjectArray(variants);
  }

  @Nullable
  public PsiElement resolveReference(@Nullable RequestMethod method, ConvertContext context) {
    if (method != null) {
      return context.getXmlElement();
    }
    return null;
  }

  protected String getUnresolvedMessage(String value) {
    return InfraAppBundle.message("cors.mapping.method.unresolved.message", value);
  }
}
