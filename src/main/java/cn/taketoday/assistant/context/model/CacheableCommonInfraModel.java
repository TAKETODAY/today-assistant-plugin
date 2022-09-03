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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.lang.Nullable;

public abstract class CacheableCommonInfraModel extends AbstractProcessableModel implements CommonInfraModel {
  private CachedValue<LocalBeansByNameCachingProcessor> myBeanNameCachingProcessor;
  private CachedValue<LocalBeansByClassCachingProcessor> myByClassCachingProcessor;

  public Collection<BeanPointer<?>> getLocalBeans() {
    return Collections.emptyList();
  }

  public Set<String> getProfiles() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getActiveProfiles() {
    return Collections.emptySet();
  }

  @Override
  public boolean processByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch()) {
      return true;
    }
    if (processLocalBeansByClass(params, processor)) {
      return super.processByClass(params, processor);
    }
    return false;
  }

  public boolean processLocalBeansByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch()) {
      return true;
    }
    CachedValue<LocalBeansByClassCachingProcessor> cachingProcessor = getBeanClassCachingProcessor();
    if (cachingProcessor != null) {
      return cachingProcessor.getValue().process(params, processor, getActiveProfiles());
    }
    return doProcessLocalBeansByClass(params, processor);
  }

  private boolean doProcessLocalBeansByClass(ModelSearchParameters.BeanClass params, Processor<? super BeanPointer<?>> processor) {
    PsiType searchType = params.getSearchType();
    if (params.isEffectiveBeanTypes()) {
      for (BeanPointer beanPointer : getLocalBeans()) {
        for (PsiType effectiveBeanType : beanPointer.getEffectiveBeanTypes()) {
          if (!processLocalBeanClass(processor, searchType, beanPointer, effectiveBeanType)) {
            return false;
          }
        }
      }
      return true;
    }
    for (BeanPointer beanPointer2 : getLocalBeans()) {
      if (!processLocalBeanClass(processor, searchType, beanPointer2, beanPointer2.getBean().getBeanType())) {
        return false;
      }
    }
    return true;
  }

  private static boolean processLocalBeanClass(Processor<? super BeanPointer<?>> processor, PsiType searchType, BeanPointer<?> beanPointer, @Nullable PsiType beanType) {
    if (beanType != null && searchType.isAssignableFrom(beanType)) {
      return processor.process(beanPointer);
    }
    return true;
  }

  @Override
  public boolean processByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    if (!params.canSearch()) {
      return true;
    }
    if (processLocalBeansByName(params, processor)) {
      return super.processByName(params, processor);
    }
    return false;
  }

  public boolean processLocalBeansByName(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    CachedValue<LocalBeansByNameCachingProcessor> cachingProcessor;
    return !params.canSearch()
            || (cachingProcessor = getBeanNameCachingProcessor()) == null
            || cachingProcessor.getValue().process(params, processor, getActiveProfiles());
  }

  protected void doProcessLocalBeans(ModelSearchParameters.BeanName params, Processor<? super BeanPointer<?>> processor) {
    for (BeanPointer<?> beanPointer : getLocalBeans()) {
      if (matchesName(params, beanPointer) && !processor.process(beanPointer)) {
        return;
      }
    }
  }

  @Override
  public final boolean processAllBeans(Processor<? super BeanPointer<?>> processor) {
    for (BeanPointer<?> pointer : getLocalBeans()) {
      if (!processor.process(pointer)) {
        return false;
      }
    }
    return super.processAllBeans(processor);
  }

  private static boolean matchesName(ModelSearchParameters.BeanName params, BeanPointer<?> pointer) {
    String paramsBeanName = params.getBeanName();
    if (paramsBeanName.equals(pointer.getName())) {
      return true;
    }
    for (String aliasName : pointer.getAliases()) {
      if (paramsBeanName.equals(aliasName)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> getAllBeanNames(BeanPointer<?> beanPointer) {
    String beanName = beanPointer.getName();
    if (StringUtil.isEmptyOrSpaces(beanName)) {
      return Collections.emptySet();
    }
    Set<String> names = ContainerUtil.newHashSet(beanName);
    for (String aliasName : beanPointer.getAliases()) {
      if (!StringUtil.isEmptyOrSpaces(aliasName)) {
        names.add(aliasName);
      }
    }
    return names;
  }

  @Nullable
  private CachedValue<LocalBeansByNameCachingProcessor> getBeanNameCachingProcessor() {
    Module module = getModule();
    if (module == null) {
      return null;
    }
    if (this.myBeanNameCachingProcessor == null) {
      this.myBeanNameCachingProcessor = CachedValuesManager.getManager(module.getProject()).createCachedValue(
              () -> CachedValueProvider.Result.create(new LocalBeansByNameCachingProcessor(), getCachingProcessorsDependencies()));
    }
    return this.myBeanNameCachingProcessor;
  }

  private CachedValue<LocalBeansByClassCachingProcessor> getBeanClassCachingProcessor() {
    Module module = getModule();
    if (module == null) {
      return null;
    }
    if (this.myByClassCachingProcessor == null) {
      this.myByClassCachingProcessor = CachedValuesManager.getManager(module.getProject()).createCachedValue(() -> {
        return CachedValueProvider.Result.create(new LocalBeansByClassCachingProcessor(), getCachingProcessorsDependencies());
      });
    }
    return this.myByClassCachingProcessor;
  }

  protected Collection<Object> getCachingProcessorsDependencies() {
    return Collections.singleton(ModificationTracker.EVER_CHANGED);
  }

  private abstract static class LocalBeansCachingProcessor<InParams extends ModelSearchParameters> extends InfraCachingProcessor<InParams> {
    protected abstract void doProcessBeans(InParams inparams, Processor<BeanPointer<?>> processor);

    @Override

    protected Collection<BeanPointer<?>> findPointers(InParams parameters) {
      Collection<BeanPointer<?>> results = new SmartList<>();
      Processor<BeanPointer<?>> collectProcessor = Processors.cancelableCollectProcessor(results);
      doProcessBeans(parameters, collectProcessor);
      Collection<BeanPointer<?>> emptyList = results.isEmpty() ? Collections.emptyList() : results;
      return emptyList;
    }

    @Override
    @Nullable
    protected BeanPointer<?> findFirstPointer(InParams parameters) {
      var firstProcessor = new CommonProcessors.FindFirstProcessor<BeanPointer<?>>();
      doProcessBeans(parameters, firstProcessor);
      return firstProcessor.getFoundValue();
    }
  }

  public class LocalBeansByNameCachingProcessor extends LocalBeansCachingProcessor<ModelSearchParameters.BeanName> {

    @Override
    protected void doProcessBeans(ModelSearchParameters.BeanName beanName, Processor processor) {
      doProcessBeans2(beanName, (Processor<BeanPointer<?>>) processor);
    }

    protected void doProcessBeans2(ModelSearchParameters.BeanName params, Processor<BeanPointer<?>> processor) {
      CacheableCommonInfraModel.this.doProcessLocalBeans(params, processor);
    }
  }

  public class LocalBeansByClassCachingProcessor extends LocalBeansCachingProcessor<ModelSearchParameters.BeanClass> {

    private LocalBeansByClassCachingProcessor() {
    }

    @Override
    protected void doProcessBeans(ModelSearchParameters.BeanClass beanClass, Processor processor) {
      doProcessBeans2(beanClass, (Processor<BeanPointer<?>>) processor);
    }

    protected void doProcessBeans2(ModelSearchParameters.BeanClass parameters, Processor<BeanPointer<?>> collectProcessor) {
      CacheableCommonInfraModel.this.doProcessLocalBeansByClass(parameters, collectProcessor);
    }
  }

  public Object[] getDependencies(Set<PsiFile> containingFiles) {
    Set<Object> dependencies = new LinkedHashSet<>();
    ContainerUtil.addAllNotNull(dependencies, containingFiles);
    ContainerUtil.addAll(dependencies, InfraModificationTrackersManager.from(getModule().getProject()).getOuterModelsDependencies());
    return ArrayUtil.toObjectArray(dependencies);
  }

  public List<BeanPointer<?>> findQualified(InfraQualifier qualifier) {
    return findLocalBeansByQualifier(this, qualifier);
  }

  public static List<BeanPointer<?>> findLocalBeansByQualifier(CacheableCommonInfraModel model, InfraQualifier infraQualifier) {
    SmartList smartList = new SmartList();
    for (BeanPointer beanPointer : model.getLocalBeans()) {
      if (beanPointer.isValid()) {
        CommonInfraBean bean = beanPointer.getBean();
        for (InfraQualifier qualifier : bean.getInfraQualifiers()) {
          if (qualifier.compareQualifiers(infraQualifier, model.getModule())) {
            smartList.add(beanPointer);
          }
        }
      }
    }
    return smartList.isEmpty() ? Collections.emptyList() : smartList;
  }
}
