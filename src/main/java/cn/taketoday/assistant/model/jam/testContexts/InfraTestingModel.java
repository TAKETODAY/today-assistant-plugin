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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemService;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.XmlInfraModel;
import cn.taketoday.assistant.impl.InfraCombinedModelFactory;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.custom.CustomModuleComponentsDiscoverer;
import cn.taketoday.assistant.model.jam.testContexts.profiles.InfraActiveProfile;

class InfraTestingModel extends InfraModel {

  private final PsiClass testClass;
  private Set<String> activeProfiles;

  public InfraTestingModel(PsiClass testClass, Module module) {
    super(module);
    this.testClass = testClass;
  }

  @Override
  public Set<String> getActiveProfiles() {
    if (this.activeProfiles == null) {
      this.activeProfiles = discoverTestContextActiveProfiles();
    }
    return this.activeProfiles;
  }

  @Override
  public Set<CommonInfraModel> getRelatedModels(boolean checkActiveProfiles) {
    Set<ContextConfiguration> testConfigurations = new LinkedHashSet<>();
    Set<XmlFile> appContexts = new LinkedHashSet<>();
    Set<PsiClass> configurationContexts = new LinkedHashSet<>();
    InfraTestContextUtil testContextUtil = InfraTestContextUtil.of();
    InheritanceUtil.processSupers(this.testClass, true, psiClass -> {
      ProgressManager.checkCanceled();
      if ("java.lang.Object".equals(psiClass.getQualifiedName())) {
        return true;
      }
      testConfigurations.addAll(getConfigurations(psiClass));
      return true;
    });
    for (ContextConfiguration contextConfiguration : testConfigurations) {
      testContextUtil.discoverConfigFiles(contextConfiguration, appContexts, configurationContexts, contextConfiguration.getPsiElement(), this.testClass);
    }
    Set<String> activeProfiles = getActiveProfiles();
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    Module module = getModule();
    if (!appContexts.isEmpty()) {
      XmlInfraModel xmlModel = InfraCombinedModelFactory.createXmlModel(appContexts, module, activeProfiles);
      ContainerUtil.addIfNotNull(models, xmlModel);
    }
    Set<String> annotationActiveProfiles = new LinkedHashSet<>(activeProfiles);
    annotationActiveProfiles.add(InfraProfile.DEFAULT_TEST_PROFILE_NAME);
    for (PsiClass context : configurationContexts) {
      ContainerUtil.addIfNotNull(models, LocalModelFactory.of().getOrCreateLocalAnnotationModel(context, getModule(), annotationActiveProfiles));
    }
    if (!module.isDisposed()) {
      ContainerUtil.addIfNotNull(models, CustomModuleComponentsDiscoverer.getCustomBeansModel(module));
    }
    for (ContextConfiguration configuration : testConfigurations) {
      for (TestingImplicitContextsProvider provider : TestingImplicitContextsProvider.EP_NAME.getExtensionList()) {
        models.addAll(provider.getModels(getModule(), configuration, annotationActiveProfiles));
      }
    }
    return models;
  }

  private static Set<ContextConfiguration> getConfigurations(PsiClass psiClass) {
    Set<ContextConfiguration> configurations = new LinkedHashSet<>(2);
    configurations.addAll(SemService.getSemService(psiClass.getProject()).getSemElements(ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, psiClass));
    var hierarchy = JamService.getJamService(psiClass.getProject()).getJamElement(
            psiClass, InfraContextHierarchy.META);
    if (hierarchy != null) {
      configurations.addAll(hierarchy.getContextConfigurations());
    }
    return configurations;
  }

  private Set<String> discoverTestContextActiveProfiles() {
    Set<String> activeProfiles = new LinkedHashSet<>();
    InheritanceUtil.processSupers(this.testClass, true, psiClass -> {
      InfraActiveProfile profiles;
      if (!"java.lang.Object".equals(psiClass.getQualifiedName()) && (profiles = InfraContextConfiguration.getActiveProfiles(psiClass)) != null) {
        activeProfiles.addAll(profiles.getActiveProfiles());
        return true;
      }
      return true;
    });
    return activeProfiles;
  }
}
