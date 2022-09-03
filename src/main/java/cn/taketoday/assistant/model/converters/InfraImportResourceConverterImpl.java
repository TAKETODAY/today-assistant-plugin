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

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraSchemaVersion;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.values.converters.resources.InfraResourceTypeProvider;
import cn.taketoday.assistant.model.xml.beans.InfraImport;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraImportResourceConverterImpl extends InfraImportResourceConverter implements InfraResourceTypeProvider {
  @Override
  public Set<PsiFile> fromString(@Nullable String s, ConvertContext context) {
    if (s == null) {
      return null;
    }
    GenericAttributeValue<PsiFile> element = (GenericAttributeValue<PsiFile>) context.getInvocationElement();
    if (DomUtil.hasXml(element) && !s.contains(PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX)) {
      PsiReference[] references = createReferences(element, context.getFile(), context);
      List<XmlFile> files = getFiles(references);
      if (files.isEmpty()) {
        return null;
      }
      return new LinkedHashSet<>(files);
    }
    return Collections.emptySet();
  }

  @Override
  public String toString(@Nullable Set<PsiFile> psiFile, ConvertContext context) {
    return null;
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue genericDomValue, PsiElement element, ConvertContext context) {
    String s = genericDomValue.getStringValue();
    if (s == null || hasTrailingSpace(s) || element == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    else if (s.contains(PlaceholderUtils.DEFAULT_PLACEHOLDER_PREFIX)) {
      return PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(genericDomValue);
    }
    else {
      Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) {
        return PsiReference.EMPTY_ARRAY;
      }
      InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, s, 1)
              .fromRoot(s.startsWith("/"))
              .filter(InfraDomUtils.XML_CONDITION)
              .modules(JamCommonUtil.getAllDependentModules(module))
              .newFileTemplateName(InfraSchemaVersion.INFRA_SCHEMA.getTemplateName());

      return ResourcesUtil.of().getReferences(builder);
    }
  }

  @Override
  @Nullable
  public Condition<PsiFileSystemItem> getResourceFilter(GenericDomValue genericDomValue) {
    if (genericDomValue.getParent() instanceof InfraImport) {
      return InfraDomUtils.XML_CONDITION;
    }
    return null;
  }

  @Nullable
  public String getErrorMessage(@Nullable String s, ConvertContext context) {
    if (s != null && hasTrailingSpace(s)) {
      return message("import.resource.error.message.trailing.space", s);
    }
    return super.getErrorMessage(s, context);
  }

  private static boolean hasTrailingSpace(String s) {
    return !s.contains("IntellijIdeaRulezzz ") && StringUtil.trimTrailing(s).length() != s.length();
  }
}
