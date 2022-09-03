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

package cn.taketoday.assistant.context.model;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.beans.stereotype.Component;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.jam.JamPsiClassInfraBean;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class AnnotationInfraModelImpl extends AnnotationInfraModel {
  private final Set<SmartPsiElementPointer<? extends PsiClass>> configClasses;

  public AnnotationInfraModelImpl(Set<? extends PsiClass> classes, Module module, @Nullable InfraFileSet fileSet) {
    super(module, fileSet);
    this.configClasses = classes.stream()
            .filter(o -> o != null && o.isValid())
            .map(cl -> SmartPointerManager.getInstance(module.getProject()).createSmartPsiElementPointer(cl))
            .collect(Collectors.toSet());
  }

  @Override
  public Set<CommonInfraModel> getRelatedModels(boolean checkActiveProfiles) {
    var localAnnotationModels = getLocalAnnotationModels(checkActiveProfiles);
    var simpleBeans = new LinkedHashSet<PsiClass>();
    var models = new LinkedHashSet<CommonInfraModel>(localAnnotationModels);
    for (SmartPsiElementPointer<? extends PsiClass> psiElementPointer : this.configClasses) {
      PsiClass configClass = psiElementPointer.getElement();
      if (configClass != null && configClass.isValid() && !isConfiguration(configClass)) {
        simpleBeans.add(configClass);
      }
    }
    if (!simpleBeans.isEmpty()) {
      models.add(new BeansInfraModel(getModule(), simpleBeans));
    }
    Set<PsiPackage> packages = getComponentScanPackages();
    if (!packages.isEmpty()) {
      models.add(new ComponentScanPackagesModel(NotNullLazyValue.lazy(() -> packages), getModule()));
    }
    models.addAll(getDependencies());
    return models;
  }

  private Set<PsiPackage> getComponentScanPackages() {
    InfraFileSet fileSet = getFileSet();
    if (fileSet instanceof ComponentScannedApplicationContext scannedCtx) {
      Module module = getModule();
      if (module != null) {
        Project project = module.getProject();
        Set<PsiPackage> packages = new LinkedHashSet<>();
        for (String pkg : scannedCtx.getComponentScanPackages()) {
          PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(pkg);
          ContainerUtil.addIfNotNull(packages, psiPackage);
        }
        return packages;
      }
    }
    return Collections.emptySet();
  }

  private Set<LocalAnnotationModel> getLocalAnnotationModels(boolean checkActiveProfiles) {
    JamPsiClassInfraBean springConfiguration;
    Set<String> activeProfiles = getActiveProfiles();
    Set<LocalAnnotationModel> models = new LinkedHashSet<>();
    for (SmartPsiElementPointer<? extends PsiClass> psiElementPointer : this.configClasses) {
      PsiClass configClass = psiElementPointer.getElement();
      if (configClass != null && configClass.isValid() && ((springConfiguration = ObjectUtils.chooseNotNull(getConfiguration(configClass),
              getComponent(configClass))) == null || !checkActiveProfiles || ProfileUtils.isInActiveProfiles(springConfiguration, activeProfiles))) {
        Set<String> profiles = (activeProfiles == null || !checkActiveProfiles) ? Collections.emptySet() : activeProfiles;
        LocalAnnotationModel model = LocalModelFactory.of().getOrCreateLocalAnnotationModel(configClass, getModule(), profiles);
        if (model != null) {
          models.add(model);
        }
      }
    }
    return models;
  }

  private static boolean isConfiguration(PsiClass configClass) {
    return configClass.isValid() && InfraUtils.isConfigurationOrMeta(configClass);
  }

  @Nullable
  private static Configuration getConfiguration(PsiClass configClass) {
    if (configClass.isValid()) {
      return JamService.getJamService(configClass.getProject()).getJamElement(Configuration.JAM_KEY, configClass);
    }
    return null;
  }

  @Nullable
  private static Component getComponent(PsiClass psiClass) {
    return Component.from(psiClass);
  }

}
