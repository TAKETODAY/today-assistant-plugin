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

package cn.taketoday.assistant.web.mvc.model.mappings.processors;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.utils.UrlMappingBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.beans.stereotype.Controller;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMappingUtil;
import cn.taketoday.assistant.web.mvc.model.mappings.UrlMappingPsiBasedElement;
import cn.taketoday.lang.Nullable;

public final class AnnotatedRequestMappingsProcessor {
  static final Key<CachedValue<List<Pair<RequestMapping.Method, Boolean>>>> REQUEST_MAPPINGS_CACHE;

  static {
    REQUEST_MAPPINGS_CACHE = Key.create("REQUEST_MAPPINGS_CACHE");
  }

  public static boolean processAnnotationMappings(Processor<UrlMappingElement> processor, Module module) {
    return processAnnotationMappings(processor, getRequestMappingsFromAnnotations(module));
  }

  private static List<Pair<RequestMapping.Method, Boolean>> getRequestMappingsFromAnnotations(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, REQUEST_MAPPINGS_CACHE, () -> {
      Set<Object> cacheDependencies = new HashSet<>();
      List<Pair<RequestMapping.Method, Boolean>> allMappings = new ArrayList<>();
      for (RequestMapping.Method mapping : getModuleRequestMappings(module)) {
        PsiClass containingClass = mapping.getPsiElement().getContainingClass();
        if (containingClass != null) {
          cacheDependencies.add(containingClass);
          Controller springController = Controller.META.getJamElement(containingClass);
          allMappings.add(Pair.create(mapping, springController != null));
        }
      }
      ContainerUtil.addAll(cacheDependencies, InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
      return CachedValueProvider.Result.create(allMappings, ArrayUtil.toObjectArray(cacheDependencies));
    }, false);
  }

  private static List<RequestMapping.Method> getModuleRequestMappings(Module module) {
    return ContainerUtil.concat(getModuleSourceRequestMappings(module), getModuleLibsRequestMappings(module));
  }

  private static List<RequestMapping.Method> getModuleSourceRequestMappings(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesScope(module);
      return CachedValueProvider.Result.create(getRequestMappings(module, scope),
              UastModificationTracker.getInstance(module.getProject()), ProjectRootManager.getInstance(module.getProject()));
    });
  }

  private static List<RequestMapping.Method> getModuleLibsRequestMappings(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module).intersectWith(ProjectScope.getLibrariesScope(module.getProject()));
      return CachedValueProvider.Result.create(getRequestMappings(module, scope), ProjectRootManager.getInstance(module.getProject()));
    });
  }

  private static List<RequestMapping.Method> getRequestMappings(Module module, GlobalSearchScope scope) {
    JamService jamService = JamService.getJamService(module.getProject());
    Collection<PsiClass> requestMappingAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, InfraMvcConstant.REQUEST_MAPPING, false);
    return ContainerUtil.flatMap(requestMappingAnnotations, annotationClass -> {
      return jamService.getJamMethodElements(RequestMapping.METHOD_JAM_KEY, annotationClass.getQualifiedName(), scope);
    });
  }

  private static boolean processAnnotationMappings(Processor<UrlMappingElement> processor, List<Pair<RequestMapping.Method, Boolean>> requestMappings) {
    for (Pair<RequestMapping.Method, Boolean> pair : requestMappings) {
      RequestMapping.Method mapping = pair.first;
      Boolean inControllerClass = pair.second;
      if (inControllerClass) {
        if (!processMapping(processor, mapping.getClassLevelMapping(), mapping)) {
          return false;
        }
      }
      else {
        PsiClass psiClass = mapping.getPsiElement().getContainingClass();
        for (PsiClass controllerClass : ClassInheritorsSearch.search(psiClass)) {
          if (Controller.META.getJamElement(controllerClass) != null && !processMapping(processor, RequestMappingUtil.getClassLevelMapping(controllerClass), mapping)) {
            return false;
          }
        }
        continue;
      }
    }
    return true;
  }

  private static boolean processMapping(Processor<UrlMappingElement> processor, RequestMapping<PsiClass> classMapping, RequestMapping.Method mapping) {
    RequestMethod[] method = mapping.getMethods();
    List<JamStringAttributeElement<String>> urls = mapping.getMappingUrls();
    if (urls.isEmpty() && classMapping != null) {
      processRequestMapping(processor, mapping, classMapping, null, null, method);
    }
    for (JamStringAttributeElement<String> url : urls) {
      String urlValue = url.getStringValue();
      if (classMapping != null) {
        if (!processRequestMapping(processor, mapping, classMapping, url, urlValue, method)) {
          return false;
        }
      }
      else if (urlValue != null && !processor.process(new UrlMappingPsiBasedElement(urlValue, mapping.getPsiElement(), new JamPomTarget(mapping, url), urlValue, method))) {
        return false;
      }
    }
    return true;
  }

  private static boolean processRequestMapping(Processor<UrlMappingElement> processor, RequestMapping<PsiMethod> mapping, RequestMapping<PsiClass> classMapping,
          @Nullable JamStringAttributeElement<String> url, @Nullable String urlValue, RequestMethod... method) {
    List<JamStringAttributeElement<String>> classUrlAttributes = classMapping.getMappingUrls();
    if (classUrlAttributes.isEmpty() && urlValue != null) {
      return processor.process(new UrlMappingPsiBasedElement(urlValue, mapping.getPsiElement(), url != null ? new JamPomTarget(mapping, url) : null, urlValue, method));
    }
    for (JamStringAttributeElement<String> classUrl : classUrlAttributes) {
      String baseUrl = classUrl.getStringValue();
      if (baseUrl != null && urlValue == null) {
        UrlMappingPsiBasedElement psiBasedMapping = new UrlMappingPsiBasedElement(baseUrl, mapping.getPsiElement(), new JamPomTarget(classMapping, classUrl), baseUrl, method);
        if (!processor.process(psiBasedMapping)) {
          return false;
        }
      }
      else {
        if (baseUrl == null) {
          baseUrl = "";
        }
        UrlMappingBuilder builder = new UrlMappingBuilder(baseUrl).appendSegment(urlValue);
        String url1 = builder.build();
        if (!processor.process(new UrlMappingPsiBasedElement(url1, mapping.getPsiElement(), url != null ? new JamPomTarget(mapping, url) : null, url1, method))) {
          return false;
        }
      }
    }
    return true;
  }
}
