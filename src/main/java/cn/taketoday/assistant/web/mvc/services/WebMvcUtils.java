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

import com.intellij.javaee.web.CommonServlet;
import com.intellij.javaee.web.CommonServletMapping;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.javaee.web.ServletMappingType;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.microservices.url.UrlPath;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Plow;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;

import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.web.mvc.WebControllerClassInfo;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.PerformanceTracker;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.mappings.processors.AnnotatedRequestMappingsProcessor;
import cn.taketoday.assistant.web.mvc.model.mappings.processors.WebMvcConfigurationRequestMappingProcessor;
import cn.taketoday.assistant.web.mvc.model.mappings.processors.XmlDefinitionMappingProcessor;
import cn.taketoday.assistant.web.mvc.views.ViewResolver;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.functions.Function1;
import one.util.streamex.StreamEx;

public final class WebMvcUtils {
  private static final Key<CachedValue<List<UrlMappingElement>>> ANNOTATED_MAPPINGS_CACHE_KEY = Key.create("infra.annotated.mappings");

  public static boolean processVariables(PsiElement viewContext, Processor<? super PsiVariable> processor, @Nullable String nameHint) {
    return processVariables(processor, viewContext.getContainingFile().getOriginalFile(), nameHint);
  }

  public static boolean processVariables(Processor<? super PsiVariable> processor, PsiFile psiFile, @Nullable String nameHint) {
    Module module = ModuleUtilCore.findModuleForFile(psiFile);
    if (module == null) {
      return true;
    }
    Set<BeanPointer<?>> controllers = WebMvcService.getInstance().getControllers(module);
    if (controllers.isEmpty()) {
      return true;
    }
    Set<ViewResolver> resolvers = WebMvcService.getInstance().getViewResolvers(module);
    Promise<MultiMap<String, PsiVariable>> emptyVariablesPromise = Promises.resolvedPromise(MultiMap.empty());
    List<Promise<MultiMap<String, PsiVariable>>> promises = StreamEx.of(controllers).map(pointer -> {
      ProgressManager.checkCanceled();
      PsiClass beanClass = pointer.getBeanClass();
      if (beanClass == null || (beanClass instanceof PsiCompiledElement)) {
        return emptyVariablesPromise;
      }
      return WebControllerClassInfo.getVariablesAsync(beanClass);
    }).toList();

    List<MultiMap<String, PsiVariable>> allCollectedResults = ProgressIndicatorUtils.awaitWithCheckCanceled(Promises.asCompletableFuture(Promises.collectResults(promises)));
    for (MultiMap<String, PsiVariable> controllerVariables : allCollectedResults) {
      if (nameHint == null || ContainerUtil.exists(controllerVariables.values(), variable -> nameHint.equals(variable.getName()))) {
        for (String view : controllerVariables.keySet()) {
          for (ViewResolver resolver : resolvers) {
            ProgressManager.checkCanceled();
            for (PsiElement resolvedView : resolver.resolveView(view)) {
              if (psiFile.equals(resolvedView)) {
                Collection<PsiVariable> variables = controllerVariables.get(view);
                if (!ContainerUtil.process(variables, processor)) {
                  return false;
                }
              }
            }
          }
        }
        continue;
      }
    }
    return true;
  }

  public static Set<WebDirectoryElement> findWebDirectoryElements(String path, Module module) {
    Set<WebDirectoryElement> elements = new HashSet<>();
    for (WebFacet webFacet : WebFacet.getInstances(module)) {
      ContainerUtil.addIfNotNull(elements, WebUtil.getWebUtil().findWebDirectoryElement(path, webFacet));
    }
    return elements;
  }

  public static Set<ServletMappingInfo> getServletMappingInfos(Module module) {
    Set<ServletMappingInfo> infos = new HashSet<>();
    for (WebFacet webFacet : WebFacet.getInstances(module)) {
      for (CommonServletMapping<CommonServlet> mapping : webFacet.getWebModel().getServletMappings()) {
        infos.addAll(ServletMappingInfo.createMappingInfos(mapping));
      }
    }
    return infos;
  }

  public static List<UrlMappingElement> getUrlMappings(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      ModificationTracker tracker = InfraModificationTrackersManager.from(module.getProject()).getEndpointsModificationTracker();
      List<UrlMappingElement> mappings = getUrlMappings(module, InfraManager.from(module.getProject()).getAllModels(module));
      return CachedValueProvider.Result.create(mappings, tracker);
    });
  }

  public static List<UrlMappingElement> getUrlMappings(Module module, Collection<InfraModel> models) {
    CommonProcessors.CollectProcessor<UrlMappingElement> processor = new CommonProcessors.CollectProcessor<>();
    processMappingDefinitions(module, models, processor);
    return new ArrayList<>(processor.getResults());
  }

  public static Iterable<UrlMappingElement> getUrlMappingsWithoutSpringModel(Module module, BiConsumer<? super Long, ? super Integer> listener) {
    PerformanceTracker tracker = new PerformanceTracker(listener, 2);
    CachedValue<List<UrlMappingElement>> annotationListMapping = getAnnotatedMappingsCacheValue(module);
    return PerformanceTracker.weakTrack(tracker, List::size, annotationListMapping);
  }

  private static CachedValue<List<UrlMappingElement>> getAnnotatedMappingsCacheValue(Module module) {
    CachedValue<List<UrlMappingElement>> existingCache = module.getUserData(ANNOTATED_MAPPINGS_CACHE_KEY);
    if (existingCache != null) {
      return existingCache;
    }
    CachedValue<List<UrlMappingElement>> cachedValue = CachedValuesManager.getManager(module.getProject()).createCachedValue(() -> {
      return CachedValueProvider.Result.create(
              Plow.of(new Function1<Processor<UrlMappingElement>, Boolean>() {
                @Override
                public Boolean invoke(Processor<UrlMappingElement> processor) {
                  return AnnotatedRequestMappingsProcessor.processAnnotationMappings(processor, module);
                }
              }).toList(), UastModificationTracker.getInstance(module.getProject()));
    });
    module.putUserData(ANNOTATED_MAPPINGS_CACHE_KEY, cachedValue);
    return cachedValue;
  }

  private static boolean processMappingDefinitions(Module module, Collection<InfraModel> models, CommonProcessors.CollectProcessor<UrlMappingElement> processor) {
    return XmlDefinitionMappingProcessor.processXmlDefinitions(module, models, processor)
            && AnnotatedRequestMappingsProcessor.processAnnotationMappings(processor, module)
            && WebMvcConfigurationRequestMappingProcessor.processWebMvcSupport(processor, module, models);
  }

  public static Collection<UrlMappingElement> getMatchingUrlMappings(Module module, UrlPath urlPath) {
    String servletInfoUrl;
    List<UrlMappingElement> urlMappings = getUrlMappings(module);
    String url = urlPath.getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION);
    SmartList smartList = new SmartList(getResolvedResults(urlMappings, urlPath));
    for (ServletMappingInfo info : getServletMappingInfos(module)) {
      if (smartList.isEmpty() && info.getType() == ServletMappingType.EXTENSION && !info.matches(url) && (servletInfoUrl = info.addMapping(url)) != null && !servletInfoUrl.equals(url)) {
        smartList.addAll(getResolvedResults(urlMappings, InfraMvcUrlPathSpecification.INSTANCE.parsePath(servletInfoUrl)));
      }
    }
    return smartList;
  }

  private static List<UrlMappingElement> getResolvedResults(List<UrlMappingElement> urlMappings, UrlPath urlPath) {
    SmartList smartList = new SmartList();
    for (UrlMappingElement mapping : urlMappings) {
      if (mapping.getUrlPath().isCompatibleWith(urlPath)) {
        ContainerUtil.addIfNotNull(smartList, mapping);
      }
    }
    return smartList;
  }
}
