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

package cn.taketoday.assistant.app.run.maven;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.idea.maven.dom.MavenDomProjectProcessorUtils;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.application.config.InfraReplacementTokenResolver;
import cn.taketoday.lang.Nullable;
import icons.OpenapiIcons;

final class InfraMavenReplacementTokenResolver extends InfraReplacementTokenResolver {

  @Override
  public List<PsiElement> resolve(PsiReference reference) {
    MavenDomProjectModel mavenDomProjectModel = getMavenDomProjectModel(reference);
    if (mavenDomProjectModel == null) {
      return Collections.emptyList();
    }
    XmlTag pomXmlProperty = MavenDomProjectProcessorUtils.searchProperty(reference.getCanonicalText(),
            mavenDomProjectModel, reference.getElement().getProject());
    return ContainerUtil.createMaybeSingletonList(pomXmlProperty);
  }

  @Override
  public List<LookupElement> getVariants(PsiReference reference) {
    MavenDomProjectModel mavenDomProjectModel = getMavenDomProjectModel(reference);
    if (mavenDomProjectModel == null) {
      return Collections.emptyList();
    }
    Set<XmlTag> propertyTags = MavenDomProjectProcessorUtils.collectProperties(mavenDomProjectModel, reference.getElement().getProject());
    return ContainerUtil.map2List(propertyTags, tag -> {
      return LookupElementBuilder.create(tag).withIcon(OpenapiIcons.RepositoryLibraryLogo);
    });
  }

  @Nullable
  private static MavenDomProjectModel getMavenDomProjectModel(PsiReference reference) {
    MavenProject mavenProject = MavenDomUtil.findContainingProject(reference.getElement());
    if (mavenProject == null) {
      return null;
    }
    Project project = reference.getElement().getProject();
    return MavenDomUtil.getMavenDomProjectModel(project, mavenProject.getFile());
  }
}
