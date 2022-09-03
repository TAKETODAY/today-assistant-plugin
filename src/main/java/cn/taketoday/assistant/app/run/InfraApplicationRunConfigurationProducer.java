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

package cn.taketoday.assistant.app.run;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.lang.Nullable;

final class InfraApplicationRunConfigurationProducer
        extends JavaRunConfigurationProducerBase<InfraApplicationRunConfiguration> {

  public ConfigurationFactory getConfigurationFactory() {
    return InfraApplicationConfigurationType.of().getConfigurationFactories()[0];
  }

  public boolean setupConfigurationFromContext(InfraApplicationRunConfiguration configuration, ConfigurationContext context,
          Ref<PsiElement> sourceElement) {
    Pair<PsiClass, PsiElement> classWithAnchor;
    Module module = context.getModule();
    if (InfraLibraryUtil.hasFrameworkLibrary(module) && (classWithAnchor = findInfraApplication(sourceElement.get())) != null) {
      if (classWithAnchor.second != null) {
        sourceElement.set(classWithAnchor.second);
      }
      configuration.setInfraMainClass(JavaExecutionUtil.getRuntimeQualifiedName(classWithAnchor.first));
      configuration.setGeneratedName();
      setupConfigurationModule(context, configuration);
      return true;
    }
    return false;
  }

  public boolean isConfigurationFromContext(InfraApplicationRunConfiguration configuration, ConfigurationContext context) {
    Pair<PsiClass, PsiElement> contextClassWithAnchor;
    Module configurationModule = configuration.getConfigurationModule().getModule();
    if (Comparing.equal(context.getModule(), configurationModule) && (contextClassWithAnchor = findInfraApplication(context.getPsiLocation())) != null) {
      String configFqn = configuration.getInfraMainClass();
      return Comparing.strEqual(configFqn, contextClassWithAnchor.first.getQualifiedName());
    }
    return false;
  }

  public boolean shouldReplace(ConfigurationFromContext self, ConfigurationFromContext other) {
    RunConfiguration configuration = other.getConfiguration();
    if ((configuration instanceof ModuleBasedConfiguration moduleBasedConfiguration) && (configuration instanceof CommonJavaRunConfigurationParameters)) {
      RunConfigurationModule runConfigurationModule = moduleBasedConfiguration.getConfigurationModule();
      return runConfigurationModule instanceof JavaRunConfigurationModule;
    }
    return false;
  }

  @Nullable
  private static Pair<PsiClass, PsiElement> findInfraApplication(@Nullable PsiElement psiElement) {
    UFile file;
    PsiClass psiClass;
    UFile file2;
    PsiClass psiClass2;
    if (psiElement == null) {
      return null;
    }
    UMethod uMethod = UastContextKt.getUastParentOfType(psiElement, UMethod.class);
    if (uMethod != null) {
      PsiClass mainClass = UastUtils.getMainMethodClass(uMethod);
      if (mainClass != null && (file2 = UastContextKt.getUastParentOfType(psiElement, UFile.class)) != null && (psiClass2 = findInfraApplication(mainClass, file2)) != null) {
        return Pair.create(psiClass2, null);
      }
      return null;
    }
    UFile file3 = UastContextKt.toUElement(psiElement, UFile.class);
    if (file3 != null) {
      for (UClass uClass : file3.getClasses()) {
        if (UastUtils.findMainInClass(uClass) != null) {
          PsiClass psiClass3 = findInfraApplication(uClass.getJavaPsi(), file3);
          if (psiClass3 != null) {
            return Pair.create(psiClass3, UElementKt.getSourcePsiElement(uClass));
          }
          return null;
        }
      }
      return null;
    }
    PsiClass mainClass2 = ApplicationConfigurationType.getMainClass(psiElement);
    if (mainClass2 != null && (file = UastContextKt.getUastParentOfType(psiElement, UFile.class)) != null && (psiClass = findInfraApplication(mainClass2, file)) != null) {
      return Pair.pair(psiClass, mainClass2);
    }
    return null;
  }

  @Nullable
  private static PsiClass findInfraApplication(PsiClass mainClass, UFile file) {
    if (InfraApplicationService.of().isInfraApplication(mainClass)) {
      return mainClass;
    }
    if (mainClass.getContainingClass() != null) {
      return null;
    }
    for (UClass uClass : file.getClasses()) {
      PsiClass psiClass = findInfraApplication(mainClass, uClass);
      if (psiClass != null) {
        return psiClass;
      }
    }
    return null;
  }

  @Nullable
  private static PsiClass findInfraApplication(PsiClass mainClass, UClass uClass) {
    PsiClass psiClass = uClass.getJavaPsi();
    if (InfraApplicationService.of().isInfraApplication(psiClass)
            && mainClass.equals(InfraApplicationService.of().findMainClassCandidate(psiClass))) {
      return psiClass;
    }
    if (psiClass.getContainingClass() == null || psiClass.hasModifierProperty("static")) {
      for (UClass innerClass : uClass.getInnerClasses()) {
        PsiClass psiClass2 = findInfraApplication(mainClass, innerClass);
        if (psiClass2 != null) {
          return psiClass2;
        }
      }
      return null;
    }
    return null;
  }
}
