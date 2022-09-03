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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.custom.CustomLocalComponentsDiscoverer;
import cn.taketoday.lang.Nullable;

public abstract class AbstractSimpleLocalModel<T extends PsiElement> extends CacheableCommonInfraModel implements LocalModel<T> {

  private final NotNullLazyValue<CustomDiscoveredBeansModel> myCustomDiscoveredBeansModel
          = NotNullLazyValue.volatileLazy(() -> new CustomDiscoveredBeansModel(this));

  public CustomDiscoveredBeansModel getCustomDiscoveredBeansModel() {
    return this.myCustomDiscoveredBeansModel.getValue();
  }

  public static void addNotNullModel(Set<? super Pair<LocalModel, LocalModelDependency>> models,
          @Nullable LocalModel model, LocalModelDependency dependency) {
    if (model != null) {
      models.add(Pair.create(model, dependency));
    }
  }

  @Override
  public Set<LocalModel> getRelatedLocalModels() {
    return getDependentLocalModels().stream().map(pair -> pair.first)
            .filter(model -> !equals(model))
            .collect(Collectors.toSet());
  }

  public String toString() {
    return getClass().getSimpleName() + "[" + getConfig() + "]";
  }

  public static Object[] getOutsideModelDependencies(LocalModel model) {
    Project project = model.getConfig().getProject();
    return ArrayUtil.append(InfraModificationTrackersManager.from(project).getOuterModelsDependencies(), model.getConfig());
  }

  public static final class CustomDiscoveredBeansModel extends CacheableCommonInfraModel {

    private final LocalModel<? extends PsiElement> myHostingLocalModel;
    private volatile CachedValue<Collection<BeanPointer<?>>> myCustomDiscoveredBeans;

    CustomDiscoveredBeansModel(LocalModel<? extends PsiElement> hostingLocalModel) {
      this.myHostingLocalModel = hostingLocalModel;
    }

    @Override
    public Collection<BeanPointer<?>> getLocalBeans() {
      Module module = getModule();
      if (module == null) {
        return Collections.emptyList();
      }
      if (this.myCustomDiscoveredBeans == null) {
        this.myCustomDiscoveredBeans = CachedValuesManager.getManager(module.getProject()).createCachedValue(() -> {
          Collection<BeanPointer<?>> pointers = computeCustomBeans();
          return CachedValueProvider.Result.create(
                  pointers, getDependencies(pointers.stream()
                          .map(BeanPointer::getContainingFile)
                          .collect(Collectors.toSet()))
          );
        }, false);
      }
      return this.myCustomDiscoveredBeans.getValue();
    }

    private Collection<BeanPointer<?>> computeCustomBeans() {
      Set<CommonInfraBean> customSpringComponents = new LinkedHashSet<>();
      for (CustomLocalComponentsDiscoverer discoverer : CustomLocalComponentsDiscoverer.EP_NAME.getExtensionList()) {
        Collection<CommonInfraBean> customComponents = discoverer.getCustomComponents(myHostingLocalModel);
        ContainerUtil.addAllNotNull(customSpringComponents, customComponents);
      }
      return InfraBeanService.of().mapBeans(customSpringComponents);
    }

    @Override
    @Nullable
    public Module getModule() {
      return this.myHostingLocalModel.getModule();
    }
  }
}
