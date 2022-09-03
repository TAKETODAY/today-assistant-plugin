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
package cn.taketoday.assistant.model.jam.testContexts.converters;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Function;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.facet.InfraSchemaVersion;
import cn.taketoday.assistant.model.converters.InfraImportResourceConverter;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class ApplicationContextReferenceConverter extends ResourcePathReferenceConverter<XmlFile> {

  @Override
  protected Function<ResolveResult, XmlFile> getMapper() {
    return InfraImportResourceConverter.XML_FILE_MAPPER;
  }

  public static List<XmlFile> getApplicationContexts(@Nullable String path, PsiElement context, Module... modules) {
    return path == null ? Collections.emptyList() :
           InfraImportResourceConverter.getFiles(getReferencesOnFakeElement(context, path, modules));
  }

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<List<XmlFile>> jamAttribute,
          PsiLanguageInjectionHost injectionHost) {
    // tweak default references with Spring beans file template

    String s = ElementManipulators.getValueText(injectionHost);

    InfraResourcesBuilder resourcesBuilder =
            InfraResourcesBuilder.create(injectionHost, s)
                    .fromRoot(s.startsWith("/"))
                    .soft(false)
                    .newFileTemplateName(InfraSchemaVersion.INFRA_SCHEMA.getTemplateName());

    return ResourcesUtil.of().getReferences(resourcesBuilder);
  }
}
