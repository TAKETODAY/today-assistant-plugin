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

package cn.taketoday.assistant.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.context.model.AnnotationInfraModelImpl;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.XmlInfraModel;
import cn.taketoday.assistant.context.model.XmlInfraModelImpl;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraCombinedModelFactory {

  @Nullable
  public static InfraModel createModel(@Nullable InfraFileSet set, Module module) {
    Set<InfraModel> mixed = new LinkedHashSet<>(2);
    List<XmlFile> xmlConfigs = getXmlConfigs(set, module.getProject());
    Set<PsiClass> codeConfigurations = getCodeConfigs(set, module.getProject());
    if (!xmlConfigs.isEmpty()) {
      InfraModel xmlModel = createXmlModel(xmlConfigs, module, set);
      if (codeConfigurations.isEmpty()) {
        return xmlModel;
      }
      ContainerUtil.addIfNotNull(mixed, xmlModel);
    }
    if (!codeConfigurations.isEmpty()) {
      var infraModel = new AnnotationInfraModelImpl(codeConfigurations, module, set);
      if (xmlConfigs.isEmpty()) {
        return infraModel;
      }
      mixed.add(infraModel);
    }
    return new CombinedInfraModelImpl(mixed, module, set);
  }

  private static List<XmlFile> getXmlConfigs(@Nullable InfraFileSet set, Project project) {
    if (set == null) {
      return Collections.emptyList();
    }
    List<XmlFile> xmlConfigs = new ArrayList<>();
    PsiManager psiManager = PsiManager.getInstance(project);
    for (VirtualFilePointer pointer : set.getXmlFiles()) {
      VirtualFile file = pointer.getFile();
      if (file != null) {
        PsiFile findFile = psiManager.findFile(file);
        if (findFile instanceof XmlFile xmlFile) {
          xmlConfigs.add(xmlFile);
        }
      }
    }
    return xmlConfigs;
  }

  private static Set<PsiClass> getCodeConfigs(@Nullable InfraFileSet set, Project project) {
    if (set == null) {
      return Collections.emptySet();
    }
    Set<PsiClass> configurations = new LinkedHashSet<>();
    PsiManager psiManager = PsiManager.getInstance(project);
    for (VirtualFilePointer pointer : set.getCodeConfigurationFiles()) {
      VirtualFile file = pointer.getFile();
      if (file != null) {
        PsiFile findFile = psiManager.findFile(file);
        if (findFile instanceof PsiClassOwner psiClassOwner) {
          configurations.addAll(getConfigurationPsiClasses(psiClassOwner));
        }
      }
    }
    return configurations;
  }

  private static void addConfigurations(Set<PsiClass> configurations, PsiClass psiClass) {
    if (configurations.contains(psiClass)) {
      return;
    }
    configurations.add(psiClass);
    for (PsiClass innerClass : psiClass.getInnerClasses()) {
      if (InfraUtils.isConfigurationOrMeta(innerClass)) {
        addConfigurations(configurations, innerClass);
      }
    }
  }

  @Nullable
  public static XmlInfraModel createXmlModel(Collection<XmlFile> configs, Module module, Set<String> activeProfiles) {
    return createXmlModel(configs, module, null, () -> {
      return activeProfiles;
    });
  }

  @Nullable
  private static XmlInfraModel createXmlModel(Collection<XmlFile> configs, Module module, @Nullable InfraFileSet set) {
    return createXmlModel(configs, module, set, () -> {
      if (set == null) {
        return null;
      }
      return set.getActiveProfiles();
    });
  }

  @Nullable
  private static XmlInfraModel createXmlModel(Collection<XmlFile> configs, Module module, @Nullable InfraFileSet set,
          final NullableFactory<Set<String>> activeProfilesFactory) {
    Set<XmlFile> files = new LinkedHashSet<>(configs.size());
    for (XmlFile psiFile : configs) {
      if (InfraDomUtils.isInfraXml(psiFile)) {
        files.add(psiFile);
      }
    }
    if (files.isEmpty()) {
      return null;
    }
    return new XmlInfraModelImpl(files, module, set) {
      @Override
      public Set<String> getActiveProfiles() {
        return activeProfilesFactory.create();
      }
    };
  }

  @Nullable
  public static InfraModel createSingleModel(PsiFile psiFile, Module module) {
    PsiFile originalFile = psiFile.getOriginalFile();
    if (originalFile instanceof XmlFile xmlFile) {
      return createSingleXmlModel(xmlFile, module);
    }
    if (originalFile instanceof PsiClassOwner) {
      return createSingleAnnotationModel((PsiClassOwner) originalFile, module);
    }
    return null;
  }

  @Nullable
  private static InfraModel createSingleXmlModel(XmlFile psiFile, Module module) {
    if (!module.isDisposed() && InfraDomUtils.isInfraXml(psiFile)) {
      return new XmlInfraModelImpl(Collections.singleton(psiFile), module, null);
    }
    return null;
  }

  @Nullable
  private static InfraModel createSingleAnnotationModel(PsiClassOwner psiClassOwner, Module module) {
    Set<PsiClass> configurations = getConfigurationPsiClasses(psiClassOwner);
    if (configurations.isEmpty()) {
      return null;
    }
    return new AnnotationInfraModelImpl(configurations, module, null);
  }

  private static Set<PsiClass> getConfigurationPsiClasses(PsiClassOwner psiClassOwner) {
    Set<PsiClass> configurations = new LinkedHashSet<>();
    PsiClass[] classes = psiClassOwner.getClasses();
    if (classes.length == 1) {
      addConfigurations(configurations, classes[0]);
    }
    else {
      for (PsiClass psiClass : classes) {
        if (InfraUtils.isConfigurationOrMeta(psiClass)) {
          addConfigurations(configurations, psiClass);
        }
      }
    }
    return configurations;
  }
}
