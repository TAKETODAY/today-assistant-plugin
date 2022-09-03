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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;

/**
 * @author Yann C&eacute;bron
 */
public abstract class InfraImportResourceConverter extends Converter<Set<PsiFile>> implements CustomReferenceConverter {

  public static final NullableFunction<ResolveResult, XmlFile> XML_FILE_MAPPER = result -> {
    PsiElement psiElement = result.getElement();
    return psiElement instanceof XmlFile ? (XmlFile) psiElement : null;
  };

  public static List<XmlFile> getFiles(PsiReference[] references) {
    final Collection<XmlFile> items = ResourcesUtil.of().getResourceItems(references, InfraDomUtils.XML_CONDITION);
    return new ArrayList<>(items);
  }
}
