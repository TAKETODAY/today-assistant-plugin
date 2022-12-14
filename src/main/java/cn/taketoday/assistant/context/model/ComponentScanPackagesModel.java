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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.NotNullFunction;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;

import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ImplicitlyRegisteredBeansProvider;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.lang.Nullable;

public class ComponentScanPackagesModel extends CacheableCommonInfraModel {

  private final Module module;
  private volatile Collection<BeanPointer<?>> scannedBeans;
  private final NotNullLazyValue<? extends Set<PsiPackage>> packages;
  private final Map<InfraQualifier, List<BeanPointer<?>>> localBeansByQualifier;

  private static boolean ourAllowDefaultPackageForTests;

  public ComponentScanPackagesModel(NotNullLazyValue<? extends Set<PsiPackage>> packages, Module module) {
    this.module = module;
    this.packages = packages;
    this.localBeansByQualifier = ConcurrentFactoryMap.createMap(key -> findLocalBeansByQualifier(this, key));
  }

  @Override
  public Collection<BeanPointer<?>> getLocalBeans() {
    if (this.scannedBeans == null) {
      Collection<BeanPointer<?>> calculateLocalBeans = calculateLocalBeans();
      this.scannedBeans = calculateLocalBeans;
      return calculateLocalBeans;
    }
    return this.scannedBeans;
  }

  public final Collection<BeanPointer<?>> calculateLocalBeans() {
    Collection<? extends BeanPointer<?>> pointers = calculateScannedBeans();
    Set<CommonInfraBean> javaBeans = new LinkedHashSet<>();
    for (BeanPointer<?> pointer : pointers) {
      Object pointerBean = pointer.getBean();
      if (pointerBean instanceof InfraStereotypeElement stereotypeElement) {
        PsiClass psiClass = stereotypeElement.getPsiElement();
        if (JamService.getJamService(psiClass.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass) == null) {
          for (InfraJavaBean javaBean : stereotypeElement.getBeans()) {
            javaBeans.add(javaBean);
            if (javaBean instanceof ImplicitlyRegisteredBeansProvider provider) {
              javaBeans.addAll(provider.getImplicitlyRegistered());
            }
          }
        }
      }
    }
    LinkedHashSet<BeanPointer<?>> beans = new LinkedHashSet<>();
    beans.addAll(pointers);
    beans.addAll(InfraBeanService.of().mapBeans(javaBeans));
    return beans;
  }

  protected Collection<BeanPointer<?>> calculateScannedBeans() {
    return getScannedComponents(packages.getValue(), module, getActiveProfiles());
  }

  public static Collection<BeanPointer<?>> getScannedComponents(Set<PsiPackage> packages, Module module, @Nullable Set<String> profiles) {
    return getScannedComponents(packages, module, profiles, true, Collections.emptySet(), Collections.emptySet());
  }

  public static Collection<Configuration> getScannedConfigurations(InfraBeansPackagesScan scan, Module module, @Nullable Set<String> profiles) {
    List<CommonInfraBean> components = getScannedComponents(dom -> InfraJamModel.from(module).getConfigurations(dom), scan.getPsiPackages(), module, profiles, scan.useDefaultFilters(),
            scan.getExcludeContextFilters(), scan.getIncludeContextFilters());
    Set<Configuration> configurations = new LinkedHashSet<>();
    for (CommonInfraBean component : components) {
      if (component instanceof Configuration) {
        configurations.add((Configuration) component);
      }
    }
    return configurations;
  }

  public static Collection<BeanPointer<?>> getScannedComponents(
          Set<PsiPackage> packages, Module module,
          @Nullable Set<String> profiles, boolean useDefaultFilters,
          Set<InfraContextFilter.Exclude> excludeContextFilters,
          Set<InfraContextFilter.Include> includeContextFilters) {
    Collection<CommonInfraBean> components = getScannedComponents(
            scope -> InfraJamModel.from(module).getStereotypeComponents(scope),
            packages, module, profiles, useDefaultFilters, excludeContextFilters, includeContextFilters);
    return InfraBeanService.of().mapBeans(components);
  }

  public static <T extends InfraStereotypeElement> List<CommonInfraBean> getScannedComponents(
          NotNullFunction<? super GlobalSearchScope, ? extends List<T>> components,
          Set<PsiPackage> packages,
          Module module,
          @Nullable Set<String> profiles, boolean useDefaultFilters, Set<InfraContextFilter.Exclude> excludeContextFilters,
          Set<InfraContextFilter.Include> includeContextFilters) {
    if (module.isDisposed() || packages.isEmpty()) {
      return new SmartList<>();
    }
    GlobalSearchScope[] scopes = packages.stream()
            .peek(psiPackage -> {
              if (psiPackage.getQualifiedName().isEmpty() && ApplicationManager.getApplication().isUnitTestMode() && !ourAllowDefaultPackageForTests) {
                throw new IllegalArgumentException("Do not use component-scan with <default> package in tests");
              }
            })
            .map(psiPackage2 -> PackageScope.packageScope(psiPackage2, true))
            .toArray(GlobalSearchScope[]::new);

    GlobalSearchScope allPackagesUnionScope = GlobalSearchScope.union(scopes);
    GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    GlobalSearchScope effectiveSearchScope = moduleScope.intersectWith(allPackagesUnionScope);
    List<T> allPointers = components.fun(effectiveSearchScope);
    Set<CommonInfraBean> filteredBeans = InfraJamService.of()
            .filterComponentScannedStereotypes(module, allPointers, packages, useDefaultFilters, excludeContextFilters, includeContextFilters);
    return ProfileUtils.filterBeansInActiveProfiles(filteredBeans, profiles);
  }

  @TestOnly
  public static void setAllowDefaultPackageForTests(boolean value) {
    ourAllowDefaultPackageForTests = value;
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  protected Collection<Object> getCachingProcessorsDependencies() {
    return Collections.singleton(PsiModificationTracker.MODIFICATION_COUNT);
  }

  @Override
  public List<BeanPointer<?>> findQualified(InfraQualifier qualifier) {
    return this.localBeansByQualifier.get(qualifier);
  }
}
