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

package cn.taketoday.assistant.app.run.gradle;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.StringSearcher;

import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;

import java.io.File;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.app.application.config.InfraReplacementTokenResolver;
import cn.taketoday.lang.Nullable;
import icons.GradleIcons;

final class InfraGradleReplacementTokenResolver extends InfraReplacementTokenResolver {
  private static GradleExtensionsSettings.GradleExtensionsData myTestData;
  private static PsiFile myBuildFile;

  @Override
  public List<PsiElement> resolve(PsiReference reference) {
    GradleExtensionsSettings.GradleExtensionsData extensionsData = getExtensionsData(reference);
    if (extensionsData == null) {
      return Collections.emptyList();
    }
    String canonicalText = reference.getCanonicalText();
    GradleExtensionsSettings.GradleProp property = extensionsData.findProperty(canonicalText);
    if (property == null) {
      return Collections.emptyList();
    }
    PsiFile buildFile = resolveBuildFile(reference);
    if (buildFile == null) {
      return Collections.emptyList();
    }
    return resolveCandidates(buildFile, canonicalText);
  }

  @Override
  public List<LookupElement> getVariants(PsiReference reference) {
    GradleExtensionsSettings.GradleExtensionsData extensionsData = getExtensionsData(reference);
    if (extensionsData == null) {
      return Collections.emptyList();
    }
    PsiFile buildFile = resolveBuildFile(reference);
    if (buildFile == null) {
      return Collections.emptyList();
    }
    PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(reference.getElement().getProject());
    return ContainerUtil.map2List(extensionsData.properties.values(), prop -> {
      LookupElementBuilder builder;
      List<PsiElement> candidates = resolveCandidates(buildFile, prop.name);
      if (candidates.size() != 1) {
        builder = LookupElementBuilder.create(prop.name);
      }
      else {
        builder = LookupElementBuilder.create(candidates.get(0), prop.name);
      }
      PsiClassType type = psiElementFactory.createTypeByFQClassName(prop.getTypeFqn());
      return builder.withIcon(GradleIcons.Gradle).withTypeText(type.getPresentableText());
    });
  }

  private static List<PsiElement> resolveCandidates(PsiFile buildFile, String name) {
    SmartList smartList = new SmartList();
    StringSearcher searcher = new StringSearcher(name, true, true);
    searcher.processOccurrences(buildFile.getText(), occurrence -> {
      PsiElement at = buildFile.findElementAt(occurrence);
      ContainerUtil.addIfNotNull(smartList, at);
      return true;
    });
    return smartList;
  }

  @Nullable
  private static PsiFile resolveBuildFile(PsiReference reference) {
    ExternalProject externalProject;
    File buildFile;
    VirtualFile virtualFile;
    if (myBuildFile != null && isTestMode()) {
      return myBuildFile;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(reference.getElement());
    String gradleProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (module == null || gradleProjectPath == null || (externalProject = ExternalProjectDataCache.getInstance(module.getProject())
            .getRootExternalProject(gradleProjectPath)) == null || (buildFile = externalProject.getBuildFile()) == null || (virtualFile = LocalFileSystem.getInstance()
            .findFileByIoFile(buildFile)) == null) {
      return null;
    }
    return PsiManager.getInstance(module.getProject()).findFile(virtualFile);
  }

  @Nullable
  private static GradleExtensionsSettings.GradleExtensionsData getExtensionsData(PsiReference reference) {
    if (myTestData != null && isTestMode()) {
      return myTestData;
    }
    Project project = reference.getElement().getProject();
    Module module = ModuleUtilCore.findModuleForPsiElement(reference.getElement());
    return GradleExtensionsSettings.getInstance(project).getExtensionsFor(module);
  }

  private static boolean isTestMode() {
    return ApplicationManager.getApplication().isUnitTestMode();
  }

  @TestOnly
  public static void setMyTestData(GradleExtensionsSettings.GradleExtensionsData myTestData2) {
    myTestData = myTestData2;
  }

  @TestOnly
  public static void setBuildFile(PsiFile buildFile) {
    myBuildFile = buildFile;
  }
}
