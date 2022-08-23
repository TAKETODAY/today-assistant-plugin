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
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringModelVisitorUtils;
import com.intellij.spring.contexts.model.LocalAnnotationModel;
import com.intellij.spring.contexts.model.LocalAnnotationModelDependentModelsProvider;
import com.intellij.spring.contexts.model.LocalModel;
import com.intellij.spring.contexts.model.graph.LocalModelDependency;
import com.intellij.spring.contexts.model.graph.LocalModelDependencyType;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.jam.stereotype.SpringComponentScan;
import com.intellij.spring.model.jam.stereotype.SpringContextImport;
import com.intellij.spring.model.jam.stereotype.SpringImport;
import com.intellij.spring.model.jam.stereotype.SpringImportResource;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScan;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScans;
import com.intellij.spring.model.jam.stereotype.SpringJamPropertySource;
import com.intellij.spring.model.jam.stereotype.SpringPropertySource;
import com.intellij.spring.model.jam.stereotype.SpringPropertySources;
import com.intellij.spring.model.jam.utils.filters.SpringContextFilter;
import com.intellij.spring.model.utils.SpringProfileUtils;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.beans.ConstructorArg;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.model.xml.context.SpringBeansPackagesScan;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PairProcessor;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 14:00
 */
public class InfraJamService {

  private static final Key<CachedValue<Set<CommonSpringBean>>>
          ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE = Key.create("ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE");
  private static final List<String> ANNOTATIONS = List.of(
          AnnotationConstant.CONTEXT_IMPORT, AnnotationConstant.CONTEXT_IMPORT_RESOURCE);

  public static InfraJamService getInstance() {
    return ApplicationManager.getApplication().getService(InfraJamService.class);
  }

  private static Map<SpringBean, Set<CommonSpringBean>> getAnnotationConfigAppContextedBeans(CommonSpringModel model, Module module) {
    Map<SpringBean, Set<CommonSpringBean>> ac = new LinkedHashMap<>();
    for (SpringBeanPointer appConfig : SpringModelVisitorUtils.getAnnotationConfigApplicationContexts(model)) {
      CommonSpringBean commonSpringBean = appConfig.getSpringBean();
      if (commonSpringBean instanceof SpringBean springBean) {
        ac.put(springBean, getAnnotationAppContextBeans(module, springBean));
      }
    }
    return ac;
  }

  private static Set<CommonSpringBean> getAnnotationAppContextBeans(Module module, SpringBean springBean) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(springBean, ANNOTATION_CONFIG_APPLICATION_CONTEXT_CACHE, () -> {
      Set<CommonSpringBean> set = new LinkedHashSet<>();
      Set<ConstructorArg> allConstructorArgs = springBean.getAllConstructorArgs();
      if (allConstructorArgs.size() == 1) {
        set.addAll(getAnnotatedStereotypes(allConstructorArgs.iterator().next(), module));
      }
      return CachedValueProvider.Result.create(set, PsiModificationTracker.MODIFICATION_COUNT);
    }, false);
  }

  private static Set<CommonSpringBean> getAnnotatedStereotypes(ConstructorArg arg, Module module) {
    Set<String> names = collectNames(arg);
    if (names.isEmpty()) {
      return Collections.emptySet();
    }
    Set<CommonSpringBean> stereotypeElements = new LinkedHashSet<>();
    for (CommonSpringBean stereotypeElement : InfraJamModel.from(module).getStereotypeComponents()) {
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

  public Set<CommonModelElement> findStereotypeConfigurationBeans(
          CommonSpringModel model, List<? extends SpringBeanPointer<?>> beansInModel, @Nullable Module module) {
    if (module == null) {
      return Collections.emptySet();
    }
    Set<CommonModelElement> result = new LinkedHashSet<>();
    for (Map.Entry<SpringBeansPackagesScan, List<CommonSpringBean>> entry : getComponentScannedBeans(model, module).entrySet()) {
      for (SpringBeanPointer stereotypeMappedBean : beansInModel) {
        List<CommonSpringBean> stereotypeElements = entry.getValue();
        if (stereotypeElements.contains(stereotypeMappedBean.getSpringBean())) {
          result.add(entry.getKey());
        }
      }
    }
    for (Map.Entry<SpringBean, Set<CommonSpringBean>> entry2 : getAnnotationConfigAppContextedBeans(model, module).entrySet()) {
      for (SpringBeanPointer stereotypeMappedBean2 : beansInModel) {
        Set<CommonSpringBean> stereotypeElements2 = entry2.getValue();
        if (stereotypeElements2.contains(stereotypeMappedBean2.getSpringBean())) {
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
    for (String s : SpringPropertyUtils.getListOrSetValues(arg)) {
      addIfNotNull(strings, s);
    }
    return strings;
  }

  private static void addIfNotNull(Set<? super String> strings, String s) {
    if (!StringUtil.isEmptyOrSpaces(s)) {
      strings.add(s);
    }
  }

  private static Map<SpringBeansPackagesScan, List<CommonSpringBean>> getComponentScannedBeans(
          CommonSpringModel model, Module module) {
    Map<SpringBeansPackagesScan, List<CommonSpringBean>> cs = new LinkedHashMap<>();
    for (SpringBeansPackagesScan packagesScan : SpringModelVisitorUtils.getComponentScans(model)) {
      cs.put(packagesScan, SpringProfileUtils.filterBeansInActiveProfiles(packagesScan.getScannedElements(module), model.getActiveProfiles()));
    }
    return cs;
  }

  public Set<CommonSpringBean> filterComponentScannedStereotypes(
          Module module, SpringBeansPackagesScan componentScan, List<? extends CommonSpringBean> allComponents) {
    Set<PsiPackage> psiPackages = componentScan.getPsiPackages();
    if (psiPackages.isEmpty()) {
      return Collections.emptySet();
    }
    return filterComponentScannedStereotypes(module, allComponents, psiPackages, componentScan.useDefaultFilters(),
            componentScan.getExcludeContextFilters(), componentScan.getIncludeContextFilters());
  }

  public Set<CommonSpringBean> filterComponentScannedStereotypes(
          Module module, List<? extends CommonSpringBean> allComponents, Set<PsiPackage> psiPackages,
          boolean useDefaultFilters, Set<SpringContextFilter.Exclude> excludeContextFilters, Set<SpringContextFilter.Include> includeContextFilters) {
    List<CommonSpringBean> includeFiltered = new ArrayList<>();
    for (SpringContextFilter.Include includeFilter : includeContextFilters) {
      includeFiltered.addAll(includeFilter.includeStereotypes(module, psiPackages));
    }
    Set<CommonSpringBean> resultElements = new LinkedHashSet<>();
    if (useDefaultFilters) {
      resultElements.addAll(filterStereotypeComponents(allComponents, excludeContextFilters, psiPackages));
    }
    resultElements.addAll(filterStereotypeComponents(includeFiltered, excludeContextFilters, psiPackages));
    return resultElements;
  }

  public List<ContextJavaBean> getContextBeans(PsiClass beanClass, @Nullable Set<String> activeProfiles) {
    List<ContextJavaBean> contextJavaBeans = doGetContextBeans(beanClass);
    return SpringProfileUtils.filterBeansInActiveProfiles(contextJavaBeans, activeProfiles);
  }

  private static List<ContextJavaBean> doGetContextBeans(PsiClass beanClass) {
    return JamService.getJamService(beanClass.getProject()).getAnnotatedMembersList(beanClass, ContextJavaBean.BEAN_JAM_KEY, 10);
  }

  @Nullable
  public InfraStereotypeElement findStereotypeElement(PsiClass psiClass) {
    JamPsiMemberSpringBean bean = JamService.getJamService(psiClass.getProject()).getJamElement(JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY, psiClass);
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

  public Set<XmlFile> getImportedResources(PsiClass psiClass, Module... contexts) {
    SpringImportResource importResource = SpringImportResource.META.getJamElement(psiClass);
    if (importResource == null) {
      return Collections.emptySet();
    }
    return new LinkedHashSet<>(importResource.getImportedResources(contexts));
  }

  public boolean processImportedResources(PsiClass psiClass, Processor<Pair<List<XmlFile>, ? extends PsiElement>> processor, Module... contexts) {
    SpringImportResource importResource = SpringImportResource.META.getJamElement(psiClass);
    if (importResource == null) {
      return true;
    }
    return importResource.processImportedResources(processor, contexts);
  }

  public Set<PsiClass> getImportedClasses(PsiClass clazz, @Nullable Module module) {
    SpringImport springImport = SemService.getSemService(clazz.getProject()).getSemElement(SpringImport.IMPORT_JAM_KEY, clazz);
    if (springImport == null) {
      return Collections.emptySet();
    }
    return new LinkedHashSet<>(springImport.getImportedClasses());
  }

  public boolean processImportedClasses(PsiClass clazz, Processor<Pair<PsiClass, ? extends PsiElement>> processor) {
    SpringContextImport springImport = SpringContextImport.META.getJamElement(clazz);
    if (springImport == null) {
      return true;
    }
    return springImport.processImportedClasses(processor);
  }

  public List<? extends SpringBeansPackagesScan> getBeansPackagesScan(PsiClass psiClass) {
    SmartList<SpringBeansPackagesScan> smartList = new SmartList<>();
    SemService service = SemService.getSemService(psiClass.getProject());
    for (PsiElement psiElement : AnnotationUtil.findAnnotations(psiClass, Collections.singleton(AnnotationConstant.COMPONENT_SCAN))) {
      ContainerUtil.addAllNotNull(smartList, service.getSemElements(SpringJamComponentScan.REPEATABLE_ANNO_JAM_KEY, psiElement));
    }
    List<SpringComponentScan> elements = service.getSemElements(SpringComponentScan.COMPONENT_SCAN_JAM_KEY, psiClass);
    smartList.addAll(ContainerUtil.filter(elements, scan -> {
      return !(scan instanceof SpringJamComponentScan);
    }));
    SpringJamComponentScans springJamComponentScans = SpringJamComponentScans.META.getJamElement(psiClass);
    if (springJamComponentScans != null) {
      smartList.addAll(springJamComponentScans.getComponentScans());
    }
    return smartList;
  }

  public Collection<SpringPropertySource> getPropertySources(PsiClass psiClass) {
    SmartList smartList = new SmartList();
    PsiElement[] findAnnotations = AnnotationUtil.findAnnotations(psiClass, Collections.singleton(AnnotationConstant.PROPERTY_SOURCE));
    SemService semService = SemService.getSemService(psiClass.getProject());
    for (PsiElement psiElement : findAnnotations) {
      smartList.addAll(semService.getSemElements(SpringJamPropertySource.REPEATABLE_ANNO_JAM_KEY, psiElement));
    }
    JamService jamService = JamService.getJamService(psiClass.getProject());
    SpringPropertySource propertySource = jamService.getJamElement(SpringPropertySource.PROPERTY_SOURCE_JAM_KEY, psiClass);
    if (propertySource != null && propertySource.getAnnotation() != null && !ArrayUtil.contains(propertySource.getAnnotation(), findAnnotations)) {
      smartList.add(propertySource);
    }
    SpringPropertySources propertySources = SpringPropertySources.META.getJamElement(psiClass);
    if (propertySources != null) {
      smartList.addAll(propertySources.getPropertySources());
    }
    return smartList;
  }

  private static List<CommonSpringBean> filterStereotypeComponents(
          List<? extends CommonSpringBean> components,
          Set<? extends SpringContextFilter.Exclude> excludeFilters, Set<? extends PsiPackage> psiPackages) {
    List<CommonSpringBean> filtered = new ArrayList<>();
    for (CommonSpringBean component : components) {
      PsiClass psiClass = PsiTypesUtil.getPsiClass(component.getBeanType());
      if (psiClass != null && isInPackage(psiPackages, psiClass)) {
        boolean exclude = false;
        Iterator<? extends SpringContextFilter.Exclude> it = excludeFilters.iterator();
        while (true) {
          if (!it.hasNext()) {
            break;
          }
          SpringContextFilter.Exclude excludeFilter = it.next();
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

  public boolean processCustomDependentLocalModels(LocalAnnotationModel localAnnotationModel, PairProcessor<? super LocalModel, ? super LocalModelDependency> processor) {
    for (LocalAnnotationModelDependentModelsProvider provider : LocalAnnotationModelDependentModelsProvider.EP_NAME.getExtensions()) {
      if (!provider.processCustomDependentLocalModels(localAnnotationModel, processor)) {
        return false;
      }
    }
    return true;
  }

}
