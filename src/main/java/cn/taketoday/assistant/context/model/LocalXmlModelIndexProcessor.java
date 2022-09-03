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

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ExecutorsQuery;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.QueryExecutor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.utils.search.executors.AbstractBeanQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.BeanFactoriesQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.BeanFactoryClassesQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.CustomBeanWrappersQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.InfraAbstractBeanQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.XmlBeanClassQueryExecutor;
import cn.taketoday.assistant.model.utils.search.executors.XmlBeanNameQueryExecutor;
import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.CustomBeanWrapper;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.context.InfraBeansPackagesScan;

public final class LocalXmlModelIndexProcessor {
  private final XmlFile myConfigFile;
  private final Project myProject;
  private final GlobalSearchScope myLocalFileSearchScope;
  private final boolean myIsInTestSource;
  private static final List<QueryExecutor<BeanPointer<?>, BeanSearchParameters.BeanName>> ourByNameExecutors = List.of(
          XmlBeanNameQueryExecutor.INSTANCE,
          CustomBeanWrappersQueryExecutor.BeanName.INSTANCE
  );

  private final InfraExecutorsQueryCachingProcessor<ModelSearchParameters.BeanName, BeanSearchParameters.BeanName> myBeanNameProcessor = new InfraExecutorsQueryCachingProcessor<>() {
    @Override
    public ExecutorsQuery<BeanPointer<?>, BeanSearchParameters.BeanName> createQuery(ModelSearchParameters.BeanName params) {
      return new ExecutorsQuery<>(LocalXmlModelIndexProcessor.this.getByNameSearchParameters(params), LocalXmlModelIndexProcessor.ourByNameExecutors);
    }
  };
  private final ByClassCacheProcessor myBeansProcessor = new ByClassCacheProcessor(XmlBeanClassQueryExecutor.INSTANCE);
  private final ByClassCacheProcessor myFactoryBeansProcessor = new ByClassCacheProcessor(BeanFactoriesQueryExecutor.INSTANCE);
  private final ByClassCacheProcessor myFactoryBeanClassesProcessor = new ByClassCacheProcessor(BeanFactoryClassesQueryExecutor.INSTANCE);
  private final ByClassCacheProcessor myAbstractBeansProcessor = new ByClassCacheProcessor(InfraAbstractBeanQueryExecutor.INSTANCE);
  private final ByClassCacheProcessor myCustomBeanWrapperProcessor = new ByClassCacheProcessor(CustomBeanWrappersQueryExecutor.BeanClass.INSTANCE);

  public LocalXmlModelIndexProcessor(XmlFile file) {
    this.myConfigFile = file;
    this.myProject = file.getProject();
    this.myLocalFileSearchScope = GlobalSearchScope.fileScope(this.myConfigFile);
    this.myIsInTestSource = ProjectRootsUtil.isInTestSource(this.myConfigFile);
  }

  public Collection<XmlTag> getCustomBeanCandidates(String key) {
    BeanSearchParameters.BeanName params = getByNameSearchParameters(ModelSearchParameters.byName(key));
    SmartList<XmlTag> smartList = new SmartList();
    Processor<BeanPointer<?>> processor = pointer -> {
      CommonInfraBean bean = pointer.getBean();
      if (!(bean instanceof CustomBeanWrapper wrapper)) {
        return true;
      }
      if (!wrapper.isParsed()) {
        XmlTag tag = wrapper.getXmlTag();
        for (XmlAttribute attribute : tag.getAttributes()) {
          if (key.equals(attribute.getDisplayValue())) {
            smartList.add(tag);
            return true;
          }
        }
        return true;
      }
      return true;
    };
    CustomBeanWrappersQueryExecutor.AllWrappers.INSTANCE.execute(params, processor);
    return smartList;
  }

  public List<InfraBeansPackagesScan> getComponentScans() {
    SmartList smartList = new SmartList();
    Processor<BeanPointer<?>> collectProcessor = pointer -> {
      CommonInfraBean bean = pointer.getBean();
      if (bean instanceof InfraBeansPackagesScan) {
        smartList.add(bean);
        return true;
      }
      return true;
    };
    InfraXmlBeansIndex.processComponentScans(getByNameSearchParameters(ModelSearchParameters.byName("")), collectProcessor);
    return smartList;
  }

  private BeanSearchParameters.BeanName getByNameSearchParameters(ModelSearchParameters.BeanName parameters) {
    BeanSearchParameters.BeanName searchParameters = BeanSearchParameters.byName(this.myProject, parameters);
    applyLocalSearchScope(searchParameters);
    return searchParameters;
  }

  private BeanSearchParameters.BeanClass getByClassSearchParameters(ModelSearchParameters.BeanClass params) {
    BeanSearchParameters.BeanClass searchParameters = BeanSearchParameters.byClass(this.myProject, params);
    applyLocalSearchScope(searchParameters);
    return searchParameters;
  }

  public boolean processByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles) {
    return this.myBeanNameProcessor.process(params, processor, activeProfiles);
  }

  public final class ByClassCacheProcessor extends InfraExecutorsQueryCachingProcessor<ModelSearchParameters.BeanClass, BeanSearchParameters.BeanClass> {
    private final List<QueryExecutor<BeanPointer<?>, BeanSearchParameters.BeanClass>> myExecutors;

    private ByClassCacheProcessor(QueryExecutor<BeanPointer<?>, BeanSearchParameters.BeanClass> executor) {
      super(aClass -> {
        return aClass.getSearchType().isValid();
      });
      this.myExecutors = Collections.singletonList(executor);
    }

    @Override
    public ExecutorsQuery<BeanPointer<?>, BeanSearchParameters.BeanClass> createQuery(ModelSearchParameters.BeanClass params) {
      return new ExecutorsQuery<>(LocalXmlModelIndexProcessor.this.getByClassSearchParameters(params), this.myExecutors);
    }
  }

  public boolean processByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles, Module module, boolean onlyPlainBeans) {
    if (!this.myBeansProcessor.process(params, processor, activeProfiles)) {
      return false;
    }
    if (params.isWithInheritors() && !processInheritors(params, processor, activeProfiles, module)) {
      return false;
    }
    if (onlyPlainBeans) {
      return true;
    }
    if (!this.myFactoryBeansProcessor.process(params, processor, activeProfiles)) {
      return false;
    }
    return (!params.isEffectiveBeanTypes() || (this.myFactoryBeanClassesProcessor.process(params, processor, activeProfiles) && processNonIndexedFactoryBeans(params, processor, activeProfiles,
            module))) && processAbstractBeans(params, processor, activeProfiles) && this.myCustomBeanWrapperProcessor.process(params, processor, activeProfiles);
  }

  private void applyLocalSearchScope(BeanSearchParameters parameters) {
    parameters.setSearchScope(this.myLocalFileSearchScope);
    parameters.setVirtualFile(this.myConfigFile.getVirtualFile());
  }

  private boolean processInheritors(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles, Module module) {
    Collection<ModelSearchParameters.BeanClass> inheritorSearchParameters = BeanClassSearchInheritorsCache.getInheritorSearchParameters(module, this.myIsInTestSource, params);
    for (ModelSearchParameters.BeanClass inheritor : inheritorSearchParameters) {
      if (!this.myBeansProcessor.process(inheritor, processor, activeProfiles)) {
        return false;
      }
    }
    return true;
  }

  private boolean processNonIndexedFactoryBeans(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles, Module module) {
    BeanSearchParameters.BeanClass searchParameters = getByClassSearchParameters(params);
    Collection<BeanPointer<?>> nonIndexedFactoryBeans = getNonIndexedFactoryBeans(this.myProject, activeProfiles, module);
    return AbstractBeanQueryExecutor.processEffectiveBeanTypes(nonIndexedFactoryBeans, searchParameters, processor);
  }

  private boolean processAbstractBeans(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor, Set<String> activeProfiles) {
    Collection<BeanPointer<?>> pointers = getAbstractBeanPointers(params, activeProfiles);
    if (!pointers.isEmpty()) {
      Map<BeanPointer<?>, Set<PsiType>> processedBeans = new HashMap<>();
      BeanSearchParameters.BeanClass searchParameters = getByClassSearchParameters(params);
      for (BeanPointer<?> pointer : pointers) {
        if (!processAbstractBean(pointer, searchParameters, processor, new HashSet(), processedBeans, activeProfiles)) {
          return false;
        }
      }
      return true;
    }
    return true;
  }

  private Collection<BeanPointer<?>> getNonIndexedFactoryBeans(Project project, Set<String> activeProfiles, Module module) {
    SmartList smartList = new SmartList();
    Processor<BeanPointer<?>> processor = Processors.cancelableCollectProcessor(smartList);
    for (ModelSearchParameters.BeanClass factoryBeanClass : getNonIndexedFactoryBeanClasses(project)) {
      processByClass(factoryBeanClass, processor, activeProfiles, module, true);
    }
    return smartList;
  }

  private static List<ModelSearchParameters.BeanClass> getNonIndexedFactoryBeanClasses(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      SmartList smartList = new SmartList();
      for (PsiClass factoryBeanClass : FactoryBeansManager.of().getKnownBeanFactories(project)) {
        if (factoryBeanClass.isValid() && !factoryBeanClass.isInterface() && !factoryBeanClass.hasModifierProperty("abstract") && !isFactoryBeanClassInIndex(factoryBeanClass)) {
          ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(factoryBeanClass);
          if (searchParameters.canSearch()) {
            smartList.add(searchParameters);
          }
        }
      }
      return CachedValueProvider.Result.create(smartList, ProjectRootManager.getInstance(project), PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  private static boolean isFactoryBeanClassInIndex(PsiClass factoryBeanClass) {
    String qualifiedName = factoryBeanClass.getQualifiedName();
    return qualifiedName == null || StringUtil.endsWith(qualifiedName, FactoryBeansManager.FACTORY_BEAN_SUFFIX);
  }

  private Collection<BeanPointer<?>> getAbstractBeanPointers(ModelSearchParameters.BeanClass params, Set<String> activeProfiles) {
    SmartList smartList = new SmartList();
    Processor<BeanPointer<?>> processor = Processors.cancelableCollectProcessor(smartList);
    this.myAbstractBeansProcessor.process(params, processor, activeProfiles);
    return smartList;
  }

  private boolean processAbstractBean(BeanPointer<?> currentParent, BeanSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> consumer, Set<? super String> noSOE,
          Map<BeanPointer<?>, Set<PsiType>> map, Set<String> activeProfiles) {
    PsiClass psiClass;
    for (PsiType psiClassType : getSimpleBeanTypes(currentParent, noSOE, map, activeProfiles)) {
      if (params.matchesClass(psiClassType) && !consumer.process(currentParent)) {
        return false;
      }
      if (params.isEffectiveBeanTypes()
              && (psiClassType instanceof PsiClassType psiClassType1)
              && (psiClass = psiClassType1.resolve()) != null && FactoryBeansManager.of()
              .isFactoryBeanClass(psiClass)) {
        for (PsiType psiType : FactoryBeansManager.of().getObjectTypes(psiClassType, currentParent.getBean())) {
          if (params.matchesClass(psiType) && !consumer.process(currentParent)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private Set<PsiType> getSimpleBeanTypes(BeanPointer<?> currentParent, Set<? super String> noSOE, Map<BeanPointer<?>, Set<PsiType>> map, Set<String> activeProfiles) {
    Set<PsiType> psiTypes = map.get(currentParent);
    if (psiTypes != null) {
      return psiTypes;
    }
    Set<PsiType> beanTypes = new HashSet<>();
    Object mo448getSpringBean = currentParent.getBean();
    if (mo448getSpringBean instanceof InfraBean) {
      InfraBean bean = (InfraBean) mo448getSpringBean;
      if (hasClassAttribute(bean)) {
        beanTypes.addAll(getSimpleBeanTypes(bean));
      }
      else {
        GenericAttributeValue<BeanPointer<?>> parentBean = bean.getParentBean();
        if (DomUtil.hasXml(parentBean)) {
          String parent = parentBean.getRawText();
          if (StringUtil.isNotEmpty(parent) && !noSOE.contains(parent)) {
            noSOE.add(parent);
            for (BeanPointer<?> parentPointer : findParentsByName(parent, activeProfiles)) {
              if (parentPointer.getBean() instanceof InfraBean) {
                beanTypes.addAll(getSimpleBeanTypes(parentPointer, noSOE, map, activeProfiles));
              }
            }
          }
        }
      }
    }
    map.put(currentParent, beanTypes);
    return beanTypes;
  }

  private Collection<BeanPointer<?>> findParentsByName(String parent, Set<String> activeProfiles) {
    ModelSearchParameters.BeanName parentBeanName = ModelSearchParameters.byName(parent);
    SmartList smartList = new SmartList();
    Processor<BeanPointer<?>> collectBeans = Processors.cancelableCollectProcessor(smartList);
    processByName(parentBeanName, collectBeans, activeProfiles);
    return smartList;
  }

  private static Set<PsiType> getSimpleBeanTypes(InfraBean bean) {
    PsiClass aClass;
    Set<PsiType> psiTypes = new HashSet<>(1);
    if ((bean instanceof AbstractDomInfraBean) && (aClass = ((AbstractDomInfraBean) bean).getClassAttributeValue()) != null) {
      PsiType classType = PsiTypesUtil.getClassType(aClass);
      psiTypes.add(classType);
      if (FactoryBeansManager.of().isFactoryBeanClass(aClass)) {
        ContainerUtil.addAll(psiTypes, FactoryBeansManager.of().getObjectTypes(classType, bean));
      }
    }
    return psiTypes;
  }

  private static boolean hasClassAttribute(InfraBean bean) {
    return DomUtil.hasXml(bean.getClazz());
  }
}
