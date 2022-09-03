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

package cn.taketoday.assistant.service;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamService;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.semantic.SemService;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PairProcessor;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.beans.stereotype.ComponentScan;
import cn.taketoday.assistant.beans.stereotype.ComponentScans;
import cn.taketoday.assistant.beans.stereotype.ContextImport;
import cn.taketoday.assistant.beans.stereotype.ImportResource;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModelDependentModelsProvider;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.jam.stereotype.AbstractComponentScan;
import cn.taketoday.assistant.model.jam.stereotype.JamPropertySource;
import cn.taketoday.assistant.model.jam.stereotype.PropertySource;
import cn.taketoday.assistant.model.jam.stereotype.PropertySources;
import cn.taketoday.assistant.model.jam.utils.filters.InfraContextFilter;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.ProfileUtils;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 14:00
 */
public class InfraJamService {

  private static final Key<CachedValue<Set<CommonInfraBean>>>
          ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE = Key.create("ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE");
  private static final List<String> ANNOTATIONS = List.of(
          AnnotationConstant.CONTEXT_IMPORT, AnnotationConstant.CONTEXT_IMPORT_RESOURCE);

  public static InfraJamService of() {
    return ApplicationManager.getApplication().getService(InfraJamService.class);
  }

  private static Map<InfraBean, Set<CommonInfraBean>> getAnnotationConfigAppContextedBeans(CommonInfraModel model, Module module) {
    Map<InfraBean, Set<CommonInfraBean>> ac = new LinkedHashMap<>();
    for (BeanPointer appConfig : InfraModelVisitorUtils.getAnnotationConfigApplicationContexts(model)) {
      CommonInfraBean commonInfraBean = appConfig.getBean();
      if (commonInfraBean instanceof InfraBean infraBean) {
        ac.put(infraBean, getAnnotationAppContextBeans(module, infraBean));
      }
    }
    return ac;
  }

  private static Set<CommonInfraBean> getAnnotationAppContextBeans(Module module, InfraBean infraBean) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(infraBean, ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE, () -> {
      Set<CommonInfraBean> set = new LinkedHashSet<>();
      Set<ConstructorArg> allConstructorArgs = infraBean.getAllConstructorArgs();
      if (allConstructorArgs.size() == 1) {
        set.addAll(getAnnotatedStereotypes(allConstructorArgs.iterator().next(), module));
      }
      return CachedValueProvider.Result.create(set, PsiModificationTracker.MODIFICATION_COUNT);
    }, false);
  }

  private static Set<CommonInfraBean> getAnnotatedStereotypes(ConstructorArg arg, Module module) {
    Set<String> names = collectNames(arg);
    if (names.isEmpty()) {
      return Collections.emptySet();
    }
    Set<CommonInfraBean> stereotypeElements = new LinkedHashSet<>();
    for (CommonInfraBean stereotypeElement : InfraJamModel.from(module).getStereotypeComponents()) {
      PsiElement identifyingPsiElement = stereotypeElement.getIdentifyingPsiElement();
      if (identifyingPsiElement instanceof PsiClass psiClass && isStereotypeAccepted(psiClass, names)) {
        stereotypeElements.add(stereotypeElement);
      }
    }
    return stereotypeElements;
  }

  private static boolean isStereotypeAccepted(PsiClass aClass, Set<String> names) {
    String fqn = aClass.getQualifiedName();
    if (fqn != null) {
      for (String classOrPackageName : names) {
        if (fqn.startsWith(classOrPackageName) || InheritanceUtil.isInheritor(aClass, classOrPackageName)) {
          return true;
        }
      }
//
//      Iterator<String> var3 = names.iterator();
//      String classOrPackageName;
//      do {
//        if (!var3.hasNext()) {
//          return false;
//        }
//
//        classOrPackageName = var3.next();
//      }
//      while (!fqn.startsWith(classOrPackageName) && !InheritanceUtil.isInheritor(aClass, classOrPackageName));
    }

    return false;
  }

  /**
   * Search configuration beans (<component-scan/>, @ComponentScan, @Repositories, etc.)
   * which loaded beans (beansInModel parameter) in model.
   */
  public Set<CommonModelElement> findStereotypeConfigurationBeans(
          CommonInfraModel model, List<? extends BeanPointer<?>> beansInModel, @Nullable Module module) {
    if (module == null) {
      return Collections.emptySet();
    }
    Set<CommonModelElement> result = new LinkedHashSet<>();
    for (Map.Entry<InfraBeansPackagesScan, List<CommonInfraBean>> entry : getComponentScannedBeans(model, module).entrySet()) {
      for (BeanPointer stereotypeMappedBean : beansInModel) {
        List<CommonInfraBean> stereotypeElements = entry.getValue();
        if (stereotypeElements.contains(stereotypeMappedBean.getBean())) {
          result.add(entry.getKey());
        }
      }
    }
    for (Map.Entry<InfraBean, Set<CommonInfraBean>> entry2 : getAnnotationConfigAppContextedBeans(model, module).entrySet()) {
      for (BeanPointer stereotypeMappedBean2 : beansInModel) {
        Set<CommonInfraBean> stereotypeElements2 = entry2.getValue();
        if (stereotypeElements2.contains(stereotypeMappedBean2.getBean())) {
          result.add(entry2.getKey());
        }
      }
    }
    return result;
  }

  private static Set<String> collectNames(ConstructorArg arg) {
    Set<String> strings = new LinkedHashSet<>();
    if (DomUtil.hasXml(arg.getValueAttr())) {
      addIfNotNull(strings, arg.getValueAttr().getStringValue());
    }
    if (DomUtil.hasXml(arg.getValue())) {
      addIfNotNull(strings, arg.getValue().getStringValue());
    }
    for (String s : InfraPropertyUtils.getListOrSetValues(arg)) {
      addIfNotNull(strings, s);
    }
    return strings;
  }

  private static void addIfNotNull(Set<? super String> strings, String s) {
    if (!StringUtil.isEmptyOrSpaces(s)) {
      strings.add(s);
    }
  }

  private static Map<InfraBeansPackagesScan, List<CommonInfraBean>> getComponentScannedBeans(
          CommonInfraModel model, Module module) {
    Map<InfraBeansPackagesScan, List<CommonInfraBean>> cs = new LinkedHashMap<>();
    for (InfraBeansPackagesScan packagesScan : InfraModelVisitorUtils.getComponentScans(model)) {
      cs.put(packagesScan, ProfileUtils.filterBeansInActiveProfiles(packagesScan.getScannedElements(module), model.getActiveProfiles()));
    }
    return cs;
  }

  /**
   * Filter components loaded by componentScan (analyses packages, include/exclude filters).
   */
  public Set<CommonInfraBean> filterComponentScannedStereotypes(
          Module module, InfraBeansPackagesScan componentScan, List<? extends CommonInfraBean> allComponents) {
    Set<PsiPackage> psiPackages = componentScan.getPsiPackages();
    if (psiPackages.isEmpty()) {
      return Collections.emptySet();
    }
    return filterComponentScannedStereotypes(module, allComponents, psiPackages, componentScan.useDefaultFilters(),
            componentScan.getExcludeContextFilters(), componentScan.getIncludeContextFilters());
  }

  public Set<CommonInfraBean> filterComponentScannedStereotypes(
          Module module, List<? extends CommonInfraBean> allComponents, Set<PsiPackage> psiPackages,
          boolean useDefaultFilters, Set<InfraContextFilter.Exclude> excludeContextFilters, Set<InfraContextFilter.Include> includeContextFilters) {
    List<CommonInfraBean> includeFiltered = new ArrayList<>();
    for (InfraContextFilter.Include includeFilter : includeContextFilters) {
      includeFiltered.addAll(includeFilter.includeStereotypes(module, psiPackages));
    }
    Set<CommonInfraBean> resultElements = new LinkedHashSet<>();
    if (useDefaultFilters) {
      resultElements.addAll(filterStereotypeComponents(allComponents, excludeContextFilters, psiPackages));
    }
    resultElements.addAll(filterStereotypeComponents(includeFiltered, excludeContextFilters, psiPackages));
    return resultElements;
  }

  public List<ContextJavaBean> getContextBeans(PsiClass beanClass, @Nullable Set<String> activeProfiles) {
    List<ContextJavaBean> contextJavaBeans = doGetContextBeans(beanClass);
    return ProfileUtils.filterBeansInActiveProfiles(contextJavaBeans, activeProfiles);
  }

  private static List<ContextJavaBean> doGetContextBeans(PsiClass beanClass) {
    return JamService.getJamService(beanClass.getProject()).getAnnotatedMembersList(beanClass, ContextJavaBean.BEAN_JAM_KEY, JamService.CHECK_METHOD | JamService.CHECK_DEEP);
  }

  @Nullable
  public InfraStereotypeElement findStereotypeElement(PsiClass psiClass) {
    JamPsiMemberInfraBean bean = JamService.getJamService(psiClass.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiClass);
    if (bean instanceof InfraStereotypeElement) {
      return (InfraStereotypeElement) bean;
    }
    return null;
  }

  static boolean isInPackage(Set<? extends PsiPackage> psiPackages, PsiClass psiClass) {
    String qualifiedName = psiClass.getQualifiedName();
    if (qualifiedName != null) {
      for (PsiPackage psiPackage : psiPackages) {
        if (psiPackage.getQualifiedName().isEmpty() && !qualifiedName.contains(".")) {
          return true;
        }

        if (StringUtil.startsWithConcatenation(qualifiedName, psiPackage.getQualifiedName(), ".")) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * XML config files defined with @ImportResource annotations.
   */
  public Set<XmlFile> getImportedResources(PsiClass psiClass, Module... contexts) {
    ImportResource importResource = ImportResource.META.getJamElement(psiClass);
    if (importResource == null) {
      return Collections.emptySet();
    }
    return new LinkedHashSet<>(importResource.getImportedResources(contexts));
  }

  /**
   * Processes {@code @ImportResource} annotation. For each element of annotation value XML config files defined by this element and
   * PsiElement defining this element are passed to processor.
   *
   * @param psiClass Class with {@code @ImportResource} annotation to visit.
   * @param processor Processor, both pair elements are guaranteed to be non-{@code null}.
   */
  public boolean processImportedResources(PsiClass psiClass, Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, Module... contexts) {
    ImportResource importResource = ImportResource.META.getJamElement(psiClass);
    if (importResource == null) {
      return true;
    }
    return importResource.processImportedResources(processor, contexts);
  }

  /**
   * Classes (@Configuration) defined with @Import annotations.
   */
  public Set<PsiClass> getImportedClasses(PsiClass clazz, @Nullable Module module) {
    ContextImport contextImport = ContextImport.from(clazz);
    if (contextImport == null) {
      return Collections.emptySet();
    }
    return new LinkedHashSet<>(contextImport.getImportedClasses());
  }

  /**
   * Processes {@code @Import} annotation. For each element of annotation value Java config files defined by this element and
   * PsiElement defining this element are passed to processor.
   *
   * @param clazz Class with {@code @Import} annotation to visit.
   * @param processor Processor, both pair elements are guaranteed to be non-{@code null}.
   */
  public boolean processImportedClasses(PsiClass clazz, Processor<Pair<PsiClass, ? extends PsiElement>> processor) {
    ContextImport element = ContextImport.META.getJamElement(clazz);
    if (element == null) {
      return true;
    }
    return element.processImportedClasses(processor);
  }

  public List<? extends InfraBeansPackagesScan> getBeansPackagesScan(PsiClass psiClass) {
    SmartList<InfraBeansPackagesScan> smartList = new SmartList<>();
    SemService service = SemService.getSemService(psiClass.getProject());
    for (PsiElement psiElement : AnnotationUtil.findAnnotations(psiClass, Collections.singleton(AnnotationConstant.COMPONENT_SCAN))) {
      ContainerUtil.addAllNotNull(smartList, service.getSemElements(ComponentScan.REPEATABLE_ANNO_JAM_KEY, psiElement));
    }
    List<AbstractComponentScan> elements = service.getSemElements(AbstractComponentScan.COMPONENT_SCAN_JAM_KEY, psiClass);
    smartList.addAll(ContainerUtil.filter(elements, Objects::isNull));
    ComponentScans infraJamComponentScans = ComponentScans.META.getJamElement(psiClass);
    if (infraJamComponentScans != null) {
      smartList.addAll(infraJamComponentScans.getComponentScans());
    }
    return smartList;
  }

  public Collection<PropertySource> getPropertySources(PsiClass psiClass) {
    SmartList<PropertySource> smartList = new SmartList<>();
    PsiElement[] findAnnotations = AnnotationUtil.findAnnotations(psiClass, Collections.singleton(AnnotationConstant.PROPERTY_SOURCE));
    SemService semService = SemService.getSemService(psiClass.getProject());
    for (PsiElement psiElement : findAnnotations) {
      smartList.addAll(semService.getSemElements(JamPropertySource.REPEATABLE_ANNO_JAM_KEY, psiElement));
    }
    JamService jamService = JamService.getJamService(psiClass.getProject());
    PropertySource propertySource = jamService.getJamElement(PropertySource.PROPERTY_SOURCE_JAM_KEY, psiClass);
    if (propertySource != null && propertySource.getAnnotation() != null && !ArrayUtil.contains(propertySource.getAnnotation(), findAnnotations)) {
      smartList.add(propertySource);
    }
    PropertySources propertySources = PropertySources.META.getJamElement(psiClass);
    if (propertySources != null) {
      smartList.addAll(propertySources.getPropertySources());
    }
    return smartList;
  }

  private static List<CommonInfraBean> filterStereotypeComponents(
          List<? extends CommonInfraBean> components,
          Set<? extends InfraContextFilter.Exclude> excludeFilters, Set<? extends PsiPackage> psiPackages) {
    List<CommonInfraBean> filtered = new ArrayList<>();
    for (CommonInfraBean component : components) {
      PsiClass psiClass = PsiTypesUtil.getPsiClass(component.getBeanType());
      if (psiClass != null && isInPackage(psiPackages, psiClass)) {
        boolean exclude = false;
        Iterator<? extends InfraContextFilter.Exclude> it = excludeFilters.iterator();
        while (true) {
          if (!it.hasNext()) {
            break;
          }
          InfraContextFilter.Exclude excludeFilter = it.next();
          if (excludeFilter.exclude(psiClass)) {
            exclude = true;
            break;
          }
        }
        if (!exclude) {
          filtered.add(component);
        }
      }
    }
    return filtered;
  }

  /**
   * Processes all resolved {@code @Enable...} annotations.
   *
   * @param psiClass Class with @Enable... annotations to visit.
   * @param processor Processor, both pair elements are guaranteed to be non-{@code null}.
   */
  public boolean processCustomAnnotations(PsiClass psiClass, Processor<Pair<PsiClass, LocalModelDependency>> processor) {
    PsiClass annotationClass;
    boolean enableAnnotation;
    PsiModifierList modifierList = psiClass.getModifierList();
    if (modifierList != null) {
      for (PsiAnnotation psiAnnotation : modifierList.getAnnotations()) {
        String name = psiAnnotation.getQualifiedName();
        if (!StringUtil.isEmptyOrSpaces(name) && (annotationClass = JavaPsiFacade.getInstance(psiClass.getProject())
                .findClass(name, psiClass.getResolveScope())) != null && ((enableAnnotation = StringUtil.getShortName(name).startsWith("Enable")) || containsImportAnnotations(annotationClass))) {
          LocalModelDependency dependency = LocalModelDependency.create(enableAnnotation ? LocalModelDependencyType.ENABLE_ANNO : LocalModelDependencyType.IMPORT, psiAnnotation.getOriginalElement());
          if (!processor.process(Pair.create(annotationClass, dependency))) {
            return false;
          }
        }
      }
      return true;
    }
    return true;
  }

  private static boolean containsImportAnnotations(PsiClass psiClass) {
    return AnnotationUtil.isAnnotated(psiClass, ANNOTATIONS, 0);
  }

  /**
   * Allows extending {@link LocalAnnotationModel} with customized dependent models, e.g. {@code @EnableXYZ}-style models.
   *
   * @param localAnnotationModel Local annotation model to visit custom dependent local models for.
   * @param processor Processor instance.
   * @return Processor result.
   */
  public boolean processCustomDependentLocalModels(LocalAnnotationModel localAnnotationModel, PairProcessor<? super LocalModel, ? super LocalModelDependency> processor) {
    for (LocalAnnotationModelDependentModelsProvider provider : LocalAnnotationModelDependentModelsProvider.EP_NAME.getExtensions()) {
      if (!provider.processCustomDependentLocalModels(localAnnotationModel, processor)) {
        return false;
      }
    }
    return true;
  }

}
