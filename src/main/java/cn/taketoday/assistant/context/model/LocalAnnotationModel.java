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
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class LocalAnnotationModel extends AbstractSimpleLocalModel<PsiClass> {

  private final PsiClass myClass;

  protected final Module myModule;

  protected final Set<String> myActiveProfiles;

  private final CachedValue<Collection<ContextJavaBean>> myLocalContextBeansCachedValue;

  public LocalAnnotationModel(PsiClass aClass, Module module, Set<String> activeProfiles) {
    this.myClass = aClass;
    this.myModule = module;
    this.myActiveProfiles = Set.copyOf(activeProfiles);
    this.myLocalContextBeansCachedValue = CachedValuesManager.getManager(myClass.getProject()).createCachedValue(() -> {
      List<ContextJavaBean> beans = JamService.getJamService(myClass.getProject())
              .getAnnotatedMembersList(myClass, ContextJavaBean.BEAN_JAM_KEY, JamService.CHECK_METHOD | JamService.CHECK_DEEP);
      Set<PsiFile> dependencies = ContainerUtil.newHashSet(myClass.getContainingFile());
      for (ContextJavaBean bean : beans) {
        if (bean.isValid()) {
          ContainerUtil.addIfNotNull(dependencies, bean.getContainingFile());
        }
      }
      return CachedValueProvider.Result.create(beans, dependencies);
    });
  }

  @Override
  public PsiClass getConfig() {
    return myClass;
  }

  @Override
  public Collection<BeanPointer<?>> getLocalBeans() {
    Collection<CommonInfraBean> allBeans = new SmartList<>();
    ContainerUtil.addIfNotNull(allBeans, getBeanForClass(myClass));
    ContainerUtil.addAllNotNull(allBeans, ProfileUtils.filterBeansInActiveProfiles(myLocalContextBeansCachedValue.getValue(), myActiveProfiles));
    return InfraBeanService.of().mapBeans(allBeans);
  }

  @Nullable
  private static CommonInfraBean getBeanForClass(PsiClass aClass) {
    return CachedValuesManager.getCachedValue(aClass, () -> {
      CommonInfraBean commonInfraBean = JamService.getJamService(aClass.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, aClass);
      if (commonInfraBean == null && !aClass.isInterface() && !aClass.hasModifierProperty("abstract") && InfraUtils.isBeanCandidateClass(aClass)) {
        commonInfraBean = new CustomInfraComponent(aClass);
      }
      return CachedValueProvider.Result.createSingleDependency(commonInfraBean, aClass);
    });
  }

  @Override
  public Set<CommonInfraModel> getRelatedModels() {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    ContainerUtil.addAllNotNull(models, getRelatedLocalModels());
    ContainerUtil.addAllNotNull(models, getCachedPackageScanModel());
    ContainerUtil.addAllNotNull(models, getCachedInnerStaticClassConfigurations());
    ContainerUtil.addAllNotNull(models, getCustomDiscoveredBeansModel());
    return models;
  }

  private Set<CommonInfraModel> getCachedPackageScanModel() {
    return CachedValuesManager.getManager(myClass.getProject())
            .getCachedValue(this, () -> CachedValueProvider.Result.create(getPackageScanModels(this), getOutsideModelDependencies(this)));
  }

  protected Set<CommonInfraModel> getPackageScanModels(LocalAnnotationModel localModel) {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    for (InfraBeansPackagesScan scan : localModel.getPackagesScans()) {
      models.add(new InfraComponentScanModel<>(localModel.myModule, scan, localModel.myActiveProfiles));
    }
    return models;
  }

  private Set<CommonInfraModel> getCachedInnerStaticClassConfigurations() {
    return CachedValuesManager.getManager(myClass.getProject()).getCachedValue(this, () -> {
      return CachedValueProvider.Result.create(getInnerStaticClassConfigurations(myClass), getOutsideModelDependencies(this));
    });
  }

  private Set<CommonInfraModel> getInnerStaticClassConfigurations(PsiClass config) {
    return getInnerStaticClassModels(config, this::getLocalAnnotationModel);
  }

  public static Set<CommonInfraModel> getInnerStaticClassModels(PsiClass config, Function<? super PsiClass, ? extends CommonInfraModel> mapper) {
    return Arrays.stream(config.getAllInnerClasses()).filter(psiClass -> {
      return psiClass.hasModifierProperty("static") && JamService.getJamService(psiClass.getProject()).getJamElement(Configuration.JAM_KEY, psiClass) != null;
    }).map(mapper).collect(Collectors.toSet());
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public Set<String> getProfiles() {
    Set<String> allProfiles = new LinkedHashSet<>();
    Configuration configuration = getConfiguration();
    if (configuration != null) {
      allProfiles.addAll(configuration.getProfile().getNames());
      for (InfraJavaBean bean : configuration.getBeans()) {
        allProfiles.addAll(bean.getProfile().getNames());
      }
    }
    return allProfiles;
  }

  @Nullable
  private Configuration getConfiguration() {
    return JamService.getJamService(myClass.getProject()).getJamElement(Configuration.JAM_KEY, myClass);
  }

  @Override
  public List<InfraBeansPackagesScan> getPackagesScans() {
    ArrayList<InfraBeansPackagesScan> smartList = new ArrayList<>(
            InfraJamService.of().getBeansPackagesScan(myClass)
    );
    for (PsiClass superClass : InheritanceUtil.getSuperClasses(myClass)) {
      if (!"java.lang.Object".equals(superClass.getQualifiedName())) {
        smartList.addAll(InfraJamService.of().getBeansPackagesScan(superClass));
      }
    }
    return smartList;
  }

  @Override
  public Set<String> getActiveProfiles() {
    return myActiveProfiles;
  }

  @Override
  public Set<Pair<LocalModel, LocalModelDependency>> getDependentLocalModels() {
    return CachedValuesManager.getManager(myClass.getProject()).getCachedValue(this, () -> {
      Module module = myModule;
      HashSet<Pair<LocalModel, LocalModelDependency>> models = new HashSet<>();
      if (!module.isDisposed()) {
        collectImportDependentLocalModels(models);
        collectScanDependentLocalModels(models);

        InfraJamService.of().processCustomAnnotations(myClass, enableAnnotation -> {
          addNotNullModel(models, getLocalAnnotationModel(enableAnnotation.getFirst()), enableAnnotation.getSecond());
          return true;
        });
        InfraJamService.of().processCustomDependentLocalModels(this, (model, dependency) -> {
          addNotNullModel(models, model, dependency);
          return true;
        });
      }
      Set<Object> dependencies = new LinkedHashSet<>();
      ContainerUtil.addAll(dependencies, getOutsideModelDependencies(this));
      dependencies.addAll(models.stream().map(pair -> pair.first.getConfig()).collect(Collectors.toSet()));
      return CachedValueProvider.Result.create(models, ArrayUtil.toObjectArray(dependencies));
    });
  }

  private void collectImportDependentLocalModels(Set<Pair<LocalModel, LocalModelDependency>> models) {
    Module module = myModule;
    collectImportDependentLocalModels(models, module, myClass);
    for (PsiClass superClass : InheritanceUtil.getSuperClasses(myClass)) {
      if (!"java.lang.Object".equals(superClass.getQualifiedName())) {
        collectImportDependentLocalModels(models, module, superClass);
      }
    }
  }

  private void collectImportDependentLocalModels(Set<Pair<LocalModel, LocalModelDependency>> models, Module module, PsiClass psiClass) {
    InfraJamService.of().processImportedResources(psiClass, pair -> {
      for (XmlFile xmlFile : pair.first) {
        addNotNullModel(models, LocalModelFactory.of().getOrCreateLocalXmlModel(xmlFile, module, myActiveProfiles),
                LocalModelDependency.create(LocalModelDependencyType.IMPORT, pair.second));
      }
      return true;
    }, myModule);
    InfraJamService.of().processImportedClasses(psiClass, pair2 -> {
      addNotNullModel(models, getLocalAnnotationModel(pair2.first),
              LocalModelDependency.create(LocalModelDependencyType.IMPORT, pair2.second));
      return true;
    });
  }

  private void collectScanDependentLocalModels(Set<Pair<LocalModel, LocalModelDependency>> models) {
    String className;
    Module module = myModule;
    List<InfraBeansPackagesScan> scans = getPackagesScans();
    for (InfraBeansPackagesScan packagesScan : scans) {
      if (!module.isDisposed()) {
        Set<CommonInfraBean> beans = packagesScan.getScannedElements(module);
        for (CommonInfraBean bean : beans) {
          if (bean instanceof InfraStereotypeElement stereotypeElement) {
            PsiClass psiClass = stereotypeElement.getPsiElement();
            if (InfraUtils.isBeanCandidateClass(psiClass)
                    && JamService.getJamService(psiClass.getProject())
                    .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass) != null
                    && ProfileUtils.isInActiveProfiles(stereotypeElement, myActiveProfiles)) {
              PsiElement identifyingElementForDependency = packagesScan.getIdentifyingPsiElement();
              if (identifyingElementForDependency == null && (className = psiClass.getQualifiedName()) != null) {
                Set<PsiPackage> packages = packagesScan.getPsiPackages();
                Iterator<PsiPackage> it = packages.iterator();
                while (true) {
                  if (!it.hasNext()) {
                    break;
                  }
                  PsiPackage next = it.next();
                  if (next.containsClassNamed(className)) {
                    identifyingElementForDependency = next;
                    break;
                  }
                }
              }
              if (identifyingElementForDependency == null) {
                identifyingElementForDependency = psiClass;
              }
              LocalModelDependency dependency = LocalModelDependency.create(LocalModelDependencyType.COMPONENT_SCAN, identifyingElementForDependency);
              addNotNullModel(models, getLocalAnnotationModel(stereotypeElement.getPsiElement()), dependency);
            }
          }
        }
      }
      else {
        return;
      }
    }
  }

  @Nullable
  protected LocalAnnotationModel getLocalAnnotationModel(PsiClass aClass) {
    return LocalModelFactory.of().getOrCreateLocalAnnotationModel(aClass, myModule, myActiveProfiles);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocalAnnotationModel model)) {
      return false;
    }
    return myClass.equals(model.myClass) && myModule.equals(model.myModule) && ProfileUtils.profilesAsString(myActiveProfiles)
            .equals(ProfileUtils.profilesAsString(model.myActiveProfiles));
  }

  public int hashCode() {
    int result = myClass.hashCode();
    int result2 = (31 * result) + this.myModule.hashCode();
    int profilesHashCode = 0;
    for (String profile : this.myActiveProfiles) {
      if (!profile.equals(InfraProfile.DEFAULT_PROFILE_NAME)) {
        profilesHashCode += profile.hashCode();
      }
    }
    return (31 * result2) + profilesHashCode;
  }

  @Override
  protected Collection<Object> getCachingProcessorsDependencies() {
    Collection<Object> dependencies = new HashSet<>();
    Collections.addAll(dependencies, InfraModificationTrackersManager.from(this.myClass.getProject()).getOuterModelsDependencies());
    Collections.addAll(dependencies, Arrays.stream(this.myClass.getSupers()).map(PsiElement::getContainingFile)
            .filter(psiFile -> psiFile != null && !(psiFile instanceof ClsFileImpl)).toArray());
    return dependencies;
  }

  @Override
  public final List<BeanPointer<?>> findQualified(InfraQualifier qualifier) {
    return findLocalBeansByQualifier(this, qualifier);
  }
}
