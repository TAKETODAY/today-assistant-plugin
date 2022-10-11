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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.DomUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitorContext;
import cn.taketoday.assistant.context.model.visitors.InfraModelVisitors;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.dom.PlaceholderDomReferenceInjector;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraJavaBean;
import cn.taketoday.assistant.model.jam.javaConfig.InfraOldJavaConfigurationUtil;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraImport;
import cn.taketoday.assistant.model.xml.context.BeansPackagesScanBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.assistant.service.InfraJamService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class LocalXmlModelImpl extends LocalXmlModel {
  private static final Key<CachedValue<Set<PsiClass>>> EXPLICIT_BEAN_CLASSES = Key.create("explicitBeanClasses");

  private volatile Collection<BeanPointer<?>> localXmlBeans;

  private final Module module;
  private final XmlFile configFile;
  private final Set<String> activeProfiles;
  private final CachedValue<Set<String>> profiles;
  private final LocalXmlModelIndexProcessor indexProcessor;
  private final NotNullLazyValue<BeanNamesMapper> localBeanNamesMapper;
  private final CachedValue<List<BeanPointer<?>>> placeholders;
  private final CachedValue<CommonInfraModel> javaConfigurationModel;
  private final CachedValue<Set<InfraComponentScanModel<?>>> scannedModels;
  private final CachedValue<CommonInfraModel> explicitlyDefinedBeansModel;

  private final Map<String, Collection<XmlTag>> customBeanIdCandidates;
  private final Map<InfraQualifier, List<BeanPointer<?>>> localBeansByQualifier;

  private final CachedValue<List<InfraBeansPackagesScan>> componentScanBeans;
  private final CachedValue<List<BeanPointer<?>>> annotationConfigApplicationContexts;
  private final CachedValue<MultiMap<BeanPointer<?>, BeanPointer<?>>> directInheritorsMap;

  public LocalXmlModelImpl(XmlFile configFile, Module module, Set<String> activeProfiles) {
    this.localBeanNamesMapper = NotNullLazyValue.volatileLazy(() -> new BeanNamesMapper(this));
    this.module = module;
    this.configFile = configFile;
    this.activeProfiles = activeProfiles;
    this.indexProcessor = CachedValuesManager.getCachedValue(configFile, () -> createIndexProcessor(configFile));
    this.customBeanIdCandidates = ConcurrentFactoryMap.createMap(key -> {
      return !canProcessBeans() ? Collections.emptyList() : indexProcessor.getCustomBeanCandidates(key);
    });
    this.localBeansByQualifier = ConcurrentFactoryMap.createMap(key2 -> findLocalBeansByQualifier(this, key2));
    this.scannedModels = CachedValuesManager.getManager(this.module.getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(getPackagesScans(this.activeProfiles).stream().map(scan -> {
        return new InfraComponentScanModel<>(this.module, scan, this.activeProfiles);
      }).collect(Collectors.toSet()), getOutsideModelDependencies(this));
    }, false);
    this.placeholders = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(computePlaceholders(), getOutsideModelDependencies(this));
    }, false);
    this.profiles = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(computeProfiles(), getOutsideModelDependencies(this));
    }, false);
    this.annotationConfigApplicationContexts = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(computeAnnotationConfigApplicationContexts(), getOutsideModelDependencies(this));
    }, false);
    this.componentScanBeans = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(canProcessBeans() ? this.indexProcessor.getComponentScans() : Collections.emptyList(), getOutsideModelDependencies(this));
    }, false);
    this.directInheritorsMap = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(computeDirectInheritorsMap(), getOutsideModelDependencies(this));
    }, false);
    this.javaConfigurationModel = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      CommonInfraModel model = new BeansInfraModel(this.module, NotNullLazyValue.lazy(this::computeJavaConfigurations));
      return CachedValueProvider.Result.create(model, getOutsideModelDependencies(this));
    }, false);
    this.explicitlyDefinedBeansModel = CachedValuesManager.getManager(getProject()).createCachedValue(() -> {
      CommonInfraModel model = new BeansInfraModel(this.module, NotNullLazyValue.lazy(this::computeExplicitlyDefinedBeans));
      return CachedValueProvider.Result.create(model, getOutsideModelDependencies(this));
    }, false);
  }

  public static CachedValueProvider.Result<LocalXmlModelIndexProcessor> createIndexProcessor(XmlFile configFile) {
    return CachedValueProvider.Result.create(new LocalXmlModelIndexProcessor(configFile),
            LocalModelFactory.getLocalXmlModelDependencies(configFile));
  }

  @Override
  @Nullable
  public DomFileElement<Beans> getRoot() {
    return InfraDomUtils.getDomFileElement(this.configFile);
  }

  private Project getProject() {
    return this.configFile.getProject();
  }

  @Override

  public XmlFile getConfig() {
    return configFile;
  }

  @Override
  public Collection<XmlTag> getCustomBeans(String id) {
    return this.customBeanIdCandidates.get(id);
  }

  private BeanNamesMapper getLocalBeanNamesMapper() {
    return this.localBeanNamesMapper.getValue();
  }

  @Override

  public Set<String> getAllBeanNames(BeanPointer<?> beanPointer) {
    String beanName = beanPointer.getName();
    if (StringUtil.isEmptyOrSpaces(beanName)) {
      return Collections.emptySet();
    }
    return new HashSet<>(getLocalBeanNamesMapper().getAllBeanNames(beanName));
  }

  @Override
  public Collection<BeanPointer<?>> getLocalBeans() {
    if (this.localXmlBeans == null) {
      Collection<BeanPointer<?>> calculateLocalXmlBeans = calculateLocalXmlBeans();
      this.localXmlBeans = calculateLocalXmlBeans;
      return calculateLocalXmlBeans;
    }
    return this.localXmlBeans;
  }

  protected Collection<BeanPointer<?>> calculateLocalXmlBeans() {
    List<BeanPointer<?>> beans = new ArrayList<>();
    Processor<CommonInfraBean> processor = bean -> {
      beans.add(InfraBeanService.of().createBeanPointer(bean));
      return true;
    };
    DomFileElement<Beans> element = getRoot();
    if (element != null) {
      processDomBeans(element.getRootElement(), processor);
    }
    return Collections.unmodifiableList(beans);
  }

  @Override
  public Set<CommonInfraModel> getRelatedModels() {
    Set<CommonInfraModel> models = new LinkedHashSet<>();
    ContainerUtil.addAllNotNull(models, getDependentLocalModels().stream().map(pair -> pair.first).collect(Collectors.toSet()));
    ContainerUtil.addAllNotNull(models, getCachedPackageScanModel());
    ContainerUtil.addAllNotNull(models, getCustomDiscoveredBeansModel());
    if (InfraUtils.findLibraryClass(module, AnnotationConstant.CONFIGURATION) != null) {
      ContainerUtil.addIfNotNull(models, getOldJavaConfigurationBeansModel());
    }
    if (isProcessExplicitlyDefinedAnnotatedBeans(module)) {
      ContainerUtil.addIfNotNull(models, getExplicitlyDefinedBeansModel());
    }
    return models;
  }

  private Set<InfraComponentScanModel<?>> getCachedPackageScanModel() {
    return this.scannedModels.getValue();
  }

  private CommonInfraModel getOldJavaConfigurationBeansModel() {
    return this.javaConfigurationModel.getValue();
  }

  private void processDomBeans(Beans rootElement, Processor<CommonInfraBean> processor) {
    if (ProfileUtils.isActiveProfile(rootElement, activeProfiles)) {
      InfraBeanUtils.of().processChildBeans(rootElement, false, processor);
      for (Beans profile : rootElement.getBeansProfiles()) {
        processDomBeans(profile, processor);
      }
    }
  }

  @Override
  public List<BeanPointer<?>> getDescendantBeans(BeanPointer<?> pointer) {
    Set<BeanPointer<?>> visited = new HashSet<>();
    visited.add(pointer);
    MultiMap<BeanPointer<?>, BeanPointer<?>> map = this.directInheritorsMap.getValue();
    addDescendants(map, pointer, visited);
    visited.remove(pointer);
    return new SmartList<>(visited);
  }

  private static void addDescendants(MultiMap<BeanPointer<?>, BeanPointer<?>> map, BeanPointer<?> current, Set<BeanPointer<?>> result) {
    Collection<BeanPointer<?>> pointers = map.get(current);
    for (BeanPointer<?> pointer : pointers) {
      if (result.add(pointer)) {
        addDescendants(map, pointer, result);
      }
    }
  }

  @Override
  public Module getModule() {
    return this.module;
  }

  @Override
  public List<BeanPointer<?>> getPlaceholderConfigurerBeans() {
    return this.placeholders.getValue();
  }

  @Override

  public List<InfraBeansPackagesScan> getPackagesScans() {
    return getPackagesScans(activeProfiles);
  }

  protected List<InfraBeansPackagesScan> getPackagesScans(Set<String> activeProfiles) {
    List<InfraBeansPackagesScan> allComponentScans = this.componentScanBeans.getValue();
    return (activeProfiles.isEmpty() || InfraProfile.DEFAULT_TEST_PROFILE_NAME.equals(ContainerUtil.getOnlyItem(activeProfiles))) ? allComponentScans : ContainerUtil.mapNotNull(allComponentScans,
            scan -> {
              if ((scan instanceof DomInfraBean) && isInActiveProfile((DomInfraBean) scan, activeProfiles)) {
                return scan;
              }
              return null;
            });
  }

  @Override
  public List<BeanPointer<?>> getAnnotationConfigAppContexts() {
    return this.annotationConfigApplicationContexts.getValue();
  }

  @Override
  public Set<String> getProfiles() {
    return this.profiles.getValue();
  }

  @Override

  public Set<String> getActiveProfiles() {
    return this.activeProfiles;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocalXmlModel model)) {
      return false;
    }
    return this.configFile.equals(model.getConfig()) && this.module.equals(model.getModule()) && ProfileUtils.profilesAsString(this.activeProfiles)
            .equals(ProfileUtils.profilesAsString(model.getActiveProfiles()));
  }

  public int hashCode() {
    int result = this.configFile.hashCode();
    return (31 * ((31 * result) + this.module.hashCode())) + ProfileUtils.profilesAsString(this.activeProfiles).hashCode();
  }

  @Override
  public boolean processByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch() || !canProcessBeans()) {
      return true;
    }
    if (processLocalBeansByClass(params, processor)) {
      return InfraModelVisitors.visitRelatedModels(this, InfraModelVisitorContext.context(processor, (model, p) -> {
        return model.processByClass(params, p);
      }), false);
    }
    return false;
  }

  @Override
  public boolean processByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    if (!processLocalBeansByName(params, processor)) {
      return false;
    }
    return InfraModelVisitors.visitRelatedModels(this, InfraModelVisitorContext.context(processor, (model, p) -> {
      return model.processByName(params, p);
    }), false);
  }

  @Override
  public boolean processLocalBeansByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    return processLocalBeansByClass(params, processor, false);
  }

  public Boolean processLocalBeansByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor, boolean onlyPlainBeans) {
    if (!params.canSearch() || !canProcessBeans()) {
      return true;
    }
    return this.indexProcessor.processByClass(params, processor, activeProfiles, this.module, onlyPlainBeans);
  }

  @Override
  public boolean processLocalBeansByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch() || !canProcessBeans()) {
      return true;
    }
    return this.indexProcessor.processByName(params, processor, activeProfiles);
  }

  private boolean canProcessBeans() {
    DomFileElement<Beans> root = getRoot();
    return root != null && ProfileUtils.isActiveProfile(root.getRootElement(), activeProfiles);
  }

  @Override

  public Set<Pair<LocalModel, LocalModelDependency>> getDependentLocalModels() {
    return getCachedDependentLocalModels(this);
  }

  private static Set<Pair<LocalModel, LocalModelDependency>> getCachedDependentLocalModels(LocalXmlModelImpl model) {
    return CachedValuesManager.getManager(model.configFile.getProject()).getCachedValue(model, () -> {
      Set<Pair<LocalModel, LocalModelDependency>> models = new LinkedHashSet<>();
      Set<String> profiles = model.activeProfiles;
      Module module = model.module;
      for (Map.Entry<XmlFile, LocalModelDependency> importedXml : getImports(model.configFile, profiles).entrySet()) {
        addNotNullModel(models, LocalModelFactory.of().getOrCreateLocalXmlModel(importedXml.getKey(), module, profiles), importedXml.getValue());
      }
      for (InfraBeansPackagesScan packagesScan : model.getPackagesScans()) {
        if (packagesScan.isValid()) {
          if (!(packagesScan instanceof BeansPackagesScanBean domScan)) {
            throw new UnsupportedOperationException("not support " + packagesScan);
          }
          String label = "<" + domScan.getXmlElementName() + " [" + domScan.getProviderName() + "] \"" + domScan.getBasePackage().getStringValue() + "\">";
          LocalModelDependency dependency = LocalModelDependency.create(label, LocalModelDependencyType.COMPONENT_SCAN, domScan);
          Set<CommonInfraBean> beans = packagesScan.getScannedElements(module);
          for (CommonInfraBean bean : beans) {
            if ((bean instanceof cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement stereotypeElement) && bean.isValid()) {
              PsiClass psiClass = stereotypeElement.getPsiElement();
              if (InfraUtils.isBeanCandidateClass(psiClass) && JamService.getJamService(psiClass.getProject())
                      .getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass) != null && ProfileUtils.isInActiveProfiles(stereotypeElement, profiles)) {
                addNotNullModel(models, LocalModelFactory.of().getOrCreateLocalAnnotationModel(psiClass, module, profiles), dependency);
              }
            }
          }
        }
      }
      Set<Object> dependencies = new LinkedHashSet<>();
      ContainerUtil.addAll(dependencies, getOutsideModelDependencies(model));
      dependencies.addAll(models.stream().map(pair -> {
        return pair.first.getConfig();
      }).collect(Collectors.toSet()));
      return CachedValueProvider.Result.create(models, ArrayUtil.toObjectArray(dependencies));
    });
  }

  private static Map<XmlFile, LocalModelDependency> getImports(XmlFile file, @Nullable Set<String> activeProfiles) {
    Map<XmlFile, LocalModelDependency> includes = new LinkedHashMap<>();
    DomFileElement<Beans> beans = InfraDomUtils.getDomFileElement(file);
    if (beans != null) {
      processLocalImports(includes, beans.getRootElement(), activeProfiles);
    }
    includes.remove(file);
    return includes;
  }

  private static void processLocalImports(Map<XmlFile, LocalModelDependency> includes, Beans beans, @Nullable Set<String> activeProfiles) {
    for (InfraImport infraImport : beans.getImports()) {
      Set<PsiFile> psiFiles = infraImport.getResource().getValue();
      if (psiFiles != null) {
        for (PsiFile psiFile : psiFiles) {
          if (psiFile instanceof XmlFile xmlFile) {
            if (!includes.containsKey(xmlFile) && InfraDomUtils.isInfraXml(xmlFile)) {
              DomFileElement<Beans> fileElement = InfraDomUtils.getDomFileElement(xmlFile);
              assert fileElement != null;
              Beans child = fileElement.getRootElement();
              if (activeProfiles == null || ProfileUtils.isActiveProfile(child, activeProfiles)) {
                String label = "<import resource=\"" + infraImport.getResource().getStringValue() + "/>";
                includes.put(xmlFile, LocalModelDependency.create(label, LocalModelDependencyType.IMPORT, infraImport));
              }
            }
          }
        }
        continue;
      }
    }
    for (Beans beanProfiles : beans.getBeansProfiles()) {
      if (activeProfiles == null || ProfileUtils.isActiveProfile(beanProfiles, activeProfiles)) {
        processLocalImports(includes, beanProfiles, activeProfiles);
      }
    }
  }

  private CommonInfraModel getExplicitlyDefinedBeansModel() {
    return this.explicitlyDefinedBeansModel.getValue();
  }

  private static boolean isProcessExplicitlyDefinedAnnotatedBeans(Module module) {
    CustomSetting.BOOLEAN setting;
    InfraFacet infraFacet = InfraFacet.from(module);
    return infraFacet == null || (setting = infraFacet.findSetting(PROCESS_EXPLICITLY_ANNOTATED)) == null || setting.getBooleanValue();
  }

  private static Set<PsiClass> getExplicitBeanCandidatePsiClasses(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, EXPLICIT_BEAN_CLASSES, () -> {
      PsiClass annotationClass = InfraUtils.findLibraryClass(module, AnnotationConstant.BEAN);
      Object[] outerModelsDependencies = InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies();
      if (annotationClass == null) {
        return CachedValueProvider.Result.create(Collections.emptySet(), outerModelsDependencies);
      }
      Set<PsiClass> psiClasses = new LinkedHashSet<>();
      Query<PsiMethod> annotatedMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
      for (PsiMember member : annotatedMethods.findAll()) {
        PsiClass containingClass = member.getContainingClass();
        if (InfraUtils.isBeanCandidateClass(containingClass)) {
          ContainerUtil.addIfNotNull(psiClasses, containingClass);
        }
      }
      Set<Object> dependencies = new LinkedHashSet<>();
      ContainerUtil.addAll(dependencies, outerModelsDependencies);
      dependencies.addAll(psiClasses.stream().filter(psiClass -> !(psiClass instanceof ClsClassImpl)).collect(Collectors.toSet()));
      return CachedValueProvider.Result.create(psiClasses, ArrayUtil.toObjectArray(dependencies));
    }, false);
  }

  private List<BeanPointer<?>> computePlaceholders() {
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    try {
      PlaceholderDomReferenceInjector.IS_COMPUTING.set(true);
      DomFileElement<Beans> element = getRoot();
      if (element != null) {
        CommonProcessors.CollectProcessor<CommonInfraBean> processor = new CommonProcessors.CollectProcessor<>() {
          public boolean process(CommonInfraBean bean) {
            String className;
            if ((bean instanceof AbstractDomInfraBean) && (className = ((AbstractDomInfraBean) bean).getClassName()) != null) {
              PsiClass psiClass = DomJavaUtil.findClass(className.trim(), LocalXmlModelImpl.this.getConfig(), LocalXmlModelImpl.this.getModule(), null);
              if (InheritanceUtil.isInheritor(psiClass, InfraConstant.PROPERTIES_LOADER_SUPPORT)) {
                smartList.add(InfraBeanService.of().createBeanPointer(bean));
                return true;
              }
              return true;
            }
            return true;
          }
        };
        processDomBeans(element.getRootElement(), processor);
      }
      PlaceholderDomReferenceInjector.IS_COMPUTING.set(false);
      return smartList;
    }
    catch (Throwable th) {
      PlaceholderDomReferenceInjector.IS_COMPUTING.set(false);
      throw th;
    }
  }

  private List<BeanPointer<?>> computeAnnotationConfigApplicationContexts() {
    if (!canProcessBeans()) {
      return Collections.emptyList();
    }
    PsiClass annotationConfigAppContext = InfraUtils.findLibraryClass(this.module, InfraConstant.ANNOTATION_CONFIG_APPLICATION_CONTEXT);
    if (annotationConfigAppContext == null) {
      return Collections.emptyList();
    }
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    Processor<BeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>(smartList) {
      public boolean process(BeanPointer pointer) {
        ProgressManager.checkCanceled();
        return super.process(pointer);
      }

      public boolean accept(BeanPointer pointer) {
        return pointer.getBean() instanceof InfraBean;
      }
    };
    var searchParameters = ModelSearchParameters.byClass(annotationConfigAppContext).withInheritors();
    indexProcessor.processByClass(searchParameters, processor, activeProfiles, this.module, true);
    return smartList;
  }

  private MultiMap<BeanPointer<?>, BeanPointer<?>> computeDirectInheritorsMap() {
    MultiMap<BeanPointer<?>, BeanPointer<?>> map = new MultiMap<>(new ConcurrentHashMap<>());
    for (BeanPointer<?> pointer : getLocalBeans()) {
      BeanPointer<?> parentPointer = pointer.getParentPointer();
      if (parentPointer != null) {
        map.putValue(parentPointer.getBasePointer(), pointer);
      }
    }
    return map;
  }

  private Collection<? extends BeanPointer<?>> computeJavaConfigurations() {
    Set<BeanPointer<?>> pointers = new LinkedHashSet<>();
    for (var javaConfig : InfraOldJavaConfigurationUtil.getJavaConfigurations(this.module)) {
      for (InfraJavaBean javaBean : javaConfig.getBeans()) {
        if (javaBean.isPublic()) {
          pointers.add(InfraBeanService.of().createBeanPointer(javaBean));
        }
      }
    }
    return pointers;
  }

  private Collection<? extends BeanPointer<?>> computeExplicitlyDefinedBeans() {
    Set<CommonInfraBean> explicitBeans = new LinkedHashSet<>();
    Module module = this.module;
    if (isProcessExplicitlyDefinedAnnotatedBeans(module)) {
      for (PsiClass explicitBeanClass : getExplicitBeanCandidatePsiClasses(module)) {
        if (InfraUtils.isBeanCandidateClass(explicitBeanClass)) {
          ModelSearchParameters.BeanClass candidateClassParams = ModelSearchParameters.byClass(explicitBeanClass).effectiveBeanTypes();
          var findFirstProcessor = new CommonProcessors.FindFirstProcessor<>();
          indexProcessor.processByClass(candidateClassParams, findFirstProcessor, activeProfiles, this.module, true);
          if (findFirstProcessor.isFound()) {
            explicitBeans.addAll(InfraJamService.of().getContextBeans(explicitBeanClass, Collections.emptySet()));
          }
        }
      }
    }
    return InfraBeanService.of().mapBeans(explicitBeans);
  }

  @Override
  public List<BeanPointer<?>> findQualified(InfraQualifier qualifier) {
    return this.localBeansByQualifier.get(qualifier);
  }

  private Set<String> computeProfiles() {
    DomFileElement<Beans> element = getRoot();
    return element == null ? Collections.emptySet() : getAllProfiles(element.getRootElement());
  }

  private static Set<String> getAllProfiles(@Nullable Beans beans) {
    Set<String> names = new LinkedHashSet<>();
    processProfiles(beans, nestedBeans -> {
      names.addAll(nestedBeans.getProfile().getNames());
      return true;
    });
    return names;
  }

  private static boolean processProfiles(@Nullable Beans beans, Processor<? super Beans> processor) {
    if (beans != null) {
      if (!processor.process(beans)) {
        return false;
      }
      for (Beans childrenBeans : beans.getBeansProfiles()) {
        if (!processProfiles(childrenBeans, processor)) {
          return false;
        }
      }
      return true;
    }
    return true;
  }

  private static boolean isInActiveProfile(DomElement domElement, Set<String> profiles) {
    Beans beans = DomUtil.getParentOfType(domElement, Beans.class, true);
    return beans == null || ProfileUtils.isActiveProfile(beans, profiles);
  }
}
