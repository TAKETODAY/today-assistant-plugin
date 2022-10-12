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

package cn.taketoday.assistant.web.mvc.services;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.beans.stereotype.Controller;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraBeanService;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.config.ServletFileSet;
import cn.taketoday.assistant.web.mvc.model.CodeConfigurationViewResolverParser;
import cn.taketoday.assistant.web.mvc.views.CustomTemplateViewResolverFactory;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;
import cn.taketoday.assistant.web.mvc.views.ViewResolverFactory;
import cn.taketoday.assistant.web.mvc.views.ViewResolverRegistry;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public class WebMvcServiceImpl extends WebMvcService {

  @Override
  public Set<BeanPointer<?>> getControllers(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Set<BeanPointer<?>> pointers = new HashSet<>();
      PsiClass servletMvcController = InfraUtils.findLibraryClass(module, InfraMvcConstant.SERVLET_MVC_CONTROLLER);
      if (servletMvcController != null) {
        for (InfraModel model : getInfraModels(module)) {
          pointers.addAll(InfraModelSearchers.findBeans(model, ModelSearchParameters.byClass(servletMvcController).withInheritors()));
        }
      }
      List<Controller> controllers = getModuleControllers(module);
      pointers.addAll(InfraBeanService.of().mapBeans(controllers));
      return CachedValueProvider.Result.create(pointers,
              ProjectRootManager.getInstance(module.getProject()), InfraModificationTrackersManager.from(module.getProject()).getEndpointsModificationTracker());
    });
  }

  @Override
  public Set<BeanPointer<?>> getBeanControllers(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Set<BeanPointer<?>> pointers = new LinkedHashSet<>();
      PsiClass servletMvcController = InfraUtils.findLibraryClass(module, InfraMvcConstant.SERVLET_MVC_CONTROLLER);
      if (servletMvcController != null) {
        ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(servletMvcController).withInheritors();
        for (InfraModel model : getInfraModels(module)) {
          pointers.addAll(InfraModelSearchers.findBeans(model, searchParameters));
        }
      }
      List<Controller> controllers = getModuleControllers(module);
      pointers.addAll(InfraBeanService.of().mapBeans(controllers));
      return CachedValueProvider.Result.create(pointers,
              ProjectRootManager.getInstance(module.getProject()), InfraModificationTrackersManager.from(module.getProject()).getEndpointsModificationTracker());
    });
  }

  private static List<Controller> getModuleControllers(Module module) {
    return ContainerUtil.concat(getModuleSourceControllers(module), getModuleLibsControllers(module));
  }

  private static List<Controller> getModuleSourceControllers(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesScope(module);
      return CachedValueProvider.Result.create(getModuleControllers(module, scope),
              UastModificationTracker.getInstance(module.getProject()), ProjectRootManager.getInstance(module.getProject()));
    });
  }

  private static List<Controller> getModuleLibsControllers(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module).intersectWith(ProjectScope.getLibrariesScope(module.getProject()));
      return CachedValueProvider.Result.create(getModuleControllers(module, scope), ProjectRootManager.getInstance(module.getProject()));
    });
  }

  private static List<Controller> getModuleControllers(Module module, GlobalSearchScope scope) {
    return InfraJamModel.from(module).getControllers(scope);
  }

  @Override
  public Set<BeanPointer<?>> getFunctionalRoutes(Module module) {
    // TODO Functional
    return Collections.emptySet();
  }

  @Override
  public Set<ViewResolver> getViewResolvers(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      return CachedValueProvider.Result.create(getViewResolversStream(module).toImmutableSet(),
              ProjectRootManager.getInstance(module.getProject()), InfraModificationTrackersManager.from(module.getProject()).getEndpointsModificationTracker());
    });
  }

  private static Set<InfraModel> getInfraModels(Module module) {
    Set<InfraModel> allModels = InfraManager.from(module.getProject()).getAllModels(module);
    Set<InfraModel> servletModels = allModels.stream().filter(model -> {
      return model.getFileSet() instanceof ServletFileSet;
    }).collect(Collectors.toSet());
    return servletModels.isEmpty() ? allModels : servletModels;
  }

  public static StreamEx<ViewResolver> getViewResolversStream(Module module) {
    CommonInfraModel[] models = InfraManager.from(module.getProject()).getAllModels(module).toArray(new CommonInfraModel[0]);
    return getResolversFromBeans(module, models).append(getResolversFromCode(module, models));
  }

  private static StreamEx<ViewResolver> getResolversFromBeans(Module module, CommonInfraModel... models) {
    PsiClass viewResolverClass = InfraUtils.findLibraryClass(module, InfraMvcConstant.VIEW_RESOLVER);
    PsiClass reactiveViewResolverClass = InfraUtils.findLibraryClass(module, InfraMvcConstant.REACTIVE_VIEW_RESOLVER);
    return StreamEx.of(ViewResolverRegistry.getInstance().getAllFactories()).flatMap(resolverFactory -> {
      if (resolverFactory instanceof CustomTemplateViewResolverFactory) {
        String customResolverClassName = ((CustomTemplateViewResolverFactory) resolverFactory).getCustomResolverClassName();
        return StreamEx.of(getResolvers(resolverFactory, InfraUtils.findLibraryClass(module, customResolverClassName), models));
      }
      return getResolvers(resolverFactory, viewResolverClass, models).append(getResolvers(resolverFactory, reactiveViewResolverClass, models));
    });
  }

  private static StreamEx<ViewResolver> getResolvers(ViewResolverFactory forFactories, @Nullable PsiClass viewResolverClass, CommonInfraModel... models) {
    if (viewResolverClass == null) {
      return StreamEx.empty();
    }
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(viewResolverClass).withInheritors();
    return StreamEx.of(models).flatMap(model -> {
      return StreamEx.of(InfraModelSearchers.findBeans(model, searchParameters)).flatMap(bean -> {
        return getViewResolvers(model, bean, forFactories);
      });
    });
  }

  public static StreamEx<ViewResolver> getViewResolvers(CommonInfraModel model, BeanPointer<?> pointer, ViewResolverFactory... forFactories) {
    CommonInfraBean infraBean = pointer.getBean();
    return StreamEx.ofNullable(PsiTypesUtil.getPsiClass(infraBean.getBeanType(true))).flatMap(beanClass -> {
      return StreamEx.of(forFactories).flatCollection(factory -> {
        return factory.createResolvers(infraBean, beanClass, model);
      });
    });
  }

  private static StreamEx<ViewResolver> getResolversFromCode(Module module, CommonInfraModel... models) {
    PsiClass webMvcConfigurationSupportClass = InfraUtils.findLibraryClass(module, InfraMvcConstant.WEB_MVC_CONFIGURATION_SUPPORT);
    if (webMvcConfigurationSupportClass == null) {
      return StreamEx.empty();
    }
    ModelSearchParameters.BeanClass mvcConfigurationSupportSearch = ModelSearchParameters.byClass(webMvcConfigurationSupportClass).withInheritors();
    return StreamEx.of(models).flatCollection(model -> {
      SmartList smartList = new SmartList();
      for (BeanPointer pointer : InfraModelSearchers.findBeans(model, mvcConfigurationSupportSearch)) {
        if (new CodeConfigurationViewResolverParser(model, pointer, smartList).collect()) {
          break;
        }
      }
      return smartList;
    });
  }
}
