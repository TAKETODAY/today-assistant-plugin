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
package cn.taketoday.assistant.dom.converters;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

/**
 * @author Yann C&eacute;bron
 */
public final class CharsetConverter extends ResolvingConverter.StringConverter {
  private final NotNullLazyValue<Set<String>> charSets = NotNullLazyValue.atomicLazy(() -> {
    return ContainerUtil.map2Set(CharsetToolkit.getAvailableCharsets(), Charset::name);
  });

  @Override
  public String fromString(final String s, final ConvertContext convertContext) {
    if (StringUtil.isEmpty(s)) {
      return null;
    }

    return charSets.getValue().contains(s) ? s : null;
  }

  @Override
  public LookupElement createLookupElement(String s) {
    return LookupElementBuilder.create(s).withCaseSensitivity(false);
  }

  @Override
  public Collection<String> getVariants(final ConvertContext convertContext) {
    return charSets.getValue();
  }

  @Override
  public String getErrorMessage(@Nullable String s, ConvertContext context) {
    return InfraBundle.message("cannot.resolve.charset", s);
  }
}