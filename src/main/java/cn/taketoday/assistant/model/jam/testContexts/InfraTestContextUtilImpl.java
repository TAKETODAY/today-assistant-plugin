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

package cn.taketoday.assistant.model.jam.testContexts;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.CommonProcessors;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.lang.Nullable;

public class InfraTestContextUtilImpl extends InfraTestContextUtil {
  public static final String CONTEXT_CONFIGURATION_SUFFIX = "ContextConfiguration";

  @Override
  public boolean isTestContextConfigurationClass(PsiClass psiClass) {
    if (psiClass.isAnnotationType() || psiClass.isEnum()) {
      return false;
    }
    PsiFile containingFile = psiClass.getContainingFile().getOriginalFile();
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(psiClass.getProject()).getFileIndex();
    if (!projectFileIndex.isInTestSourceContent(virtualFile)) {
      return false;
    }
    CommonProcessors.FindProcessor<PsiClass> findFirstProcessor = new CommonProcessors.FindProcessor<>() {
      public boolean accept(PsiClass psiClass2) {
        return InfraTestContextUtilImpl.isSpringTestContextConfiguration(psiClass2);
      }
    };
    InheritanceUtil.processSupers(psiClass, true, findFirstProcessor);
    return findFirstProcessor.isFound();
  }

  public static boolean isSpringTestContextConfiguration(PsiClass psiClass) {
    JamService service = JamService.getJamService(psiClass.getProject());
    return service.getJamElement(
            ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, psiClass) != null
            || service.getJamElement(psiClass, InfraContextHierarchy.META) != null;
  }

  @Override

  public CommonInfraModel getTestingModel(PsiClass testClass) {
    Module module = ModuleUtilCore.findModuleForPsiElement(testClass);
    return module == null ? InfraModel.UNKNOWN : new InfraTestingModel(testClass, module);
  }

  @Override
  public void discoverConfigFiles(ContextConfiguration contextConfiguration,
          Set<XmlFile> appContexts, Set<PsiClass> configurationContexts, PsiClass... contexts) {
    List<PsiClass> configurationClasses = contextConfiguration.getConfigurationClasses();
    if (!contextConfiguration.hasLocationsAttribute() && !contextConfiguration.hasValueAttribute() && configurationClasses.isEmpty()) {
      XmlFile xmlFile = getDefaultLocation(contextConfiguration);
      if (xmlFile != null && InfraDomUtils.isInfraXml(xmlFile)) {
        appContexts.add(xmlFile);
        return;
      }
      return;
    }
    for (XmlFile xmlFile2 : contextConfiguration.getLocations(contexts)) {
      if (InfraDomUtils.isInfraXml(xmlFile2)) {
        appContexts.add(xmlFile2);
      }
    }
    configurationContexts.addAll(configurationClasses);
  }

  @Nullable
  public static XmlFile getDefaultLocation(ContextConfiguration contextConfiguration) {
    if (!isAnnotationConfigLoader(contextConfiguration)) {
      PsiClass psiClass = contextConfiguration.getPsiElement();
      String defaultAppContextName = getDefaultAppContextName(contextConfiguration);
      PsiDirectory containingDirectory = psiClass.getContainingFile().getContainingDirectory();
      if (containingDirectory != null) {
        PsiFile findFile = containingDirectory.findFile(defaultAppContextName);
        if (findFile instanceof XmlFile xmlFile) {
          return xmlFile;
        }
        PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(containingDirectory);
        if (psiPackage != null) {
          for (PsiDirectory psiDirectory : psiPackage.getDirectories()) {
            PsiFile findFile2 = psiDirectory.findFile(defaultAppContextName);
            if (findFile2 instanceof XmlFile xmlFile) {
              return xmlFile;
            }
          }
          return null;
        }
        return null;
      }
      return null;
    }
    return null;
  }

  public static String getDefaultAppContextName(ContextConfiguration contextConfiguration) {
    return contextConfiguration.getPsiElement().getName() + "-context.xml";
  }

  public static boolean isGenericXmlContextLoader(ContextConfiguration contextConfiguration) {
    return isConfigClassLoader(contextConfiguration, "cn.taketoday.test.context.support.GenericXmlContextLoader");
  }

  public static boolean isAnnotationConfigLoader(ContextConfiguration contextConfiguration) {
    return isConfigClassLoader(contextConfiguration, "cn.taketoday.test.context.support.AnnotationConfigContextLoader");
  }

  private static boolean isConfigClassLoader(ContextConfiguration contextConfiguration, String className) {
    PsiClass psiClass = contextConfiguration.getLoaderClass();
    if (psiClass == null) {
      return false;
    }
    return className.equals(psiClass.getQualifiedName()) || InheritanceUtil.isInheritor(psiClass, className);
  }

}
