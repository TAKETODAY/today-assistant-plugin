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

package cn.taketoday.assistant.model.config;

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.LocalModelProducer;
import cn.taketoday.assistant.beans.stereotype.ComponentScan;
import cn.taketoday.assistant.context.model.InfraComponentScanModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.config.autoconfigure.jam.InfraApplication;
import cn.taketoday.assistant.model.config.properties.ConfigurationPropertiesScan;
import cn.taketoday.assistant.model.config.properties.CustomConfigurationPropertiesScan;
import cn.taketoday.assistant.model.jam.stereotype.CustomComponentScan;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/31 01:56
 */
public class InfraApplicationLocalModel extends LocalAnnotationModel {

  @Override
  public List<InfraBeansPackagesScan> getPackagesScans() {
    List<InfraBeansPackagesScan> scans = super.getPackagesScans();
    return ContainerUtil.filter(scans, (scan) -> {
      return !isDefaultApplicationToSkip(scan, scans);
    });
  }

  @Override
  protected Set<CommonInfraModel> getPackageScanModels(LocalAnnotationModel localModel) {
    var models = new LinkedHashSet<CommonInfraModel>();
    Module module = localModel.getModule();
    List<InfraBeansPackagesScan> scans = localModel.getPackagesScans();

    for (InfraBeansPackagesScan scan : scans) {
      if (!isDefaultApplicationToSkip(scan, scans)) {
        models.add(new InfraComponentScanModel<>(module, scan, localModel.getActiveProfiles()));
      }
    }

    return models;
  }

  private static boolean isDefaultApplicationToSkip(
          InfraBeansPackagesScan scan, List<? extends InfraBeansPackagesScan> scans) {

    PsiElement element;
    PsiFile psiFile;
    if (scan instanceof CustomComponentScan) {
      element = scan.getIdentifyingPsiElement();
      if (isInfraApplication(element)) {
        psiFile = element.getContainingFile();
        return scans.stream().filter((packageScan) -> {
          return (packageScan instanceof CustomComponentScan
                  || packageScan instanceof ComponentScan)
                  && psiFile.equals(packageScan.getContainingFile());
        }).count() > 1L;
      }
    }

    if (scan instanceof CustomConfigurationPropertiesScan) {
      element = scan.getIdentifyingPsiElement();
      if (isInfraApplication(element)) {
        psiFile = element.getContainingFile();
        return scans.stream().filter(packageScan -> {
          return (packageScan instanceof CustomConfigurationPropertiesScan
                  || packageScan instanceof ConfigurationPropertiesScan)
                  && psiFile.equals(packageScan.getContainingFile());
        }).count() > 1L;
      }
    }

    return false;
  }

  private static boolean isInfraApplication(PsiElement element) {
    return element instanceof PsiAnnotation
            && InfraConfigConstant.INFRA_APPLICATION.equals(((PsiAnnotation) element).getQualifiedName());
  }

  public InfraApplicationLocalModel(PsiClass aClass, Module module, Set<String> activeProfiles) {
    super(aClass, module, activeProfiles);
  }

  public static class InfraApplicationLocalModelProducer implements LocalModelProducer {

    @Override
    public LocalAnnotationModel create(PsiClass aClass, Module module, Set<String> activeProfiles) {
      return JamService.getJamService(module.getProject())
                     .getJamElement(InfraApplication.META.getJamKey(), aClass) != null
             ? new InfraApplicationLocalModel(aClass, module, activeProfiles)
             : null;
    }
  }
}
