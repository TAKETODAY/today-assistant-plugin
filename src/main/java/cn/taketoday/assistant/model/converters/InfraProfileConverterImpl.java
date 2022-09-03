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

import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;

import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraProfileConverterImpl extends InfraProfileConverter {
  public InfraProfileConverterImpl() {
    super(InfraUtils.INFRA_DELIMITERS);
  }

  public String convertString(@Nullable String s, ConvertContext convertContext) {
    return s;
  }

  public String toString(@Nullable String s) {
    return s;
  }

  protected Object[] getReferenceVariants(ConvertContext convertContext, GenericDomValue<? extends List<String>> listGenericDomValue) {
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  public PsiElement resolveReference(@Nullable String s, ConvertContext convertContext) {
    return convertContext.getReferenceXmlElement();
  }

  protected String getUnresolvedMessage(String s) {
    return InfraBundle.message("profile.unresolved.message");
  }
}
