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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

import cn.taketoday.assistant.profiles.InfraProfilesFactory;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraProfilesDomConverterImpl extends InfraProfilesDomConverter {

  public String toString(List<String> strings, ConvertContext context) {
    return StringUtil.join(strings, ",");
  }

  public List<String> fromString(String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    return InfraUtils.tokenize(s);
  }

  public PsiReference[] createReferences(GenericDomValue<List<String>> genericDomValue, PsiElement element, ConvertContext context) {
    Module module = context.getModule();
    if (module == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    String roles = genericDomValue.getStringValue();
    return InfraProfilesFactory.of().getProfilesReferences(module, element, roles, 0, InfraUtils.INFRA_DELIMITERS, true);
  }
}
