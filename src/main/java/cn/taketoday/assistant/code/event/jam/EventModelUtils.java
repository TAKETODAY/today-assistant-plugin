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

package cn.taketoday.assistant.code.event.jam;

import com.intellij.jam.JamService;
import com.intellij.jam.JavaLibraryUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.boot.reactor.ReactorConstants;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.code.event.beans.PublishEventPointDescriptor;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.assistant.InfraConstant.APPLICATION_EVENT;
import static cn.taketoday.assistant.InfraConstant.APPLICATION_EVENT_MULTICASTER;
import static cn.taketoday.assistant.InfraConstant.APPLICATION_EVENT_PUBLISHER;
import static cn.taketoday.assistant.InfraConstant.APPLICATION_EVENT_SHORT_NAME;
import static cn.taketoday.assistant.InfraConstant.APPLICATION_LISTENER;
import static cn.taketoday.assistant.InfraConstant.ASYNC_EVENT_WRAPPER_CLASSES;
import static cn.taketoday.assistant.InfraConstant.EVENT_WRAPPER_CLASSES;
import static cn.taketoday.assistant.InfraConstant.ON_APPLICATION_EVENT_METHOD;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public abstract class EventModelUtils {
  private static final String PUBLISH_EVENT_METHOD = "publishEvent";
  private static final String MULTICAST_EVENT_METHOD = "multicastEvent";

  public static Collection<PublishEventPointDescriptor> getPublishPoints(Module module, PsiType handledType) {
    PsiClass eventClass;
    boolean includeLibraries = (!(handledType instanceof PsiClassType)) || (eventClass = ((PsiClassType) handledType).resolve()) == null || ModuleUtilCore.findModuleForPsiElement(eventClass) == null;
    return ContainerUtil.filter(getPublishPoints(module, includeLibraries), descriptor -> {
      PsiType publishType = descriptor.getEventType();
      return publishType != null && handledType.isAssignableFrom(publishType);
    });
  }

  private static Collection<PublishEventPointDescriptor> getPublishPoints(Module module, boolean includeLibraries) {
    List<PublishEventPointDescriptor> modulePublishPoints = CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      List<PublishEventPointDescriptor> cacheValue = findEventPublishingPoints(module.getProject(), module, moduleNetworkScope(module));
      return CachedValueProvider.Result.createSingleDependency(cacheValue, UastModificationTracker.getInstance(module.getProject()));
    });
    if (!includeLibraries) {
      return modulePublishPoints;
    }
    List<PublishEventPointDescriptor> libraryPublishPoints = getLibraryPublishPoints(module.getProject());
    return ContainerUtil.concat(libraryPublishPoints, modulePublishPoints);
  }

  private static GlobalSearchScope moduleNetworkScope(Module module) {
    Set<Module> moduleNetwork = new HashSet<>();
    moduleNetwork.add(module);
    collectModuleNetwork(moduleNetwork, module);
    if (moduleNetwork.size() == 1) {
      return GlobalSearchScope.moduleScope(module);
    }
    List<GlobalSearchScope> scopes = ContainerUtil.map2List(moduleNetwork, GlobalSearchScope::moduleScope);
    return GlobalSearchScope.union(scopes);
  }

  private static void collectModuleNetwork(Set<Module> visited, Module module) {
    SmartList<Module> smartList = new SmartList<>();
    Set<Module> dependencies = new HashSet<>();
    ModuleUtilCore.getDependencies(module, dependencies);
    for (Module networkModule : dependencies) {
      if (!visited.contains(networkModule) && InfraLibraryUtil.hasLibrary(networkModule)) {
        visited.add(networkModule);
        smartList.add(networkModule);
      }
    }
    Set<Module> dependents = new HashSet<>();
    ModuleUtilCore.collectModulesDependsOn(module, dependents);
    for (Module networkModule2 : dependents) {
      if (!visited.contains(networkModule2)) {
        visited.add(networkModule2);
        smartList.add(networkModule2);
      }
    }
    for (Module visitModule : smartList) {
      collectModuleNetwork(visited, visitModule);
    }
  }

  private static List<PublishEventPointDescriptor> getLibraryPublishPoints(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      GlobalSearchScope scope = ProjectScope.getLibrariesScope(project);
      List<PublishEventPointDescriptor> cacheValue = findEventPublishingPoints(project, null, scope);
      return CachedValueProvider.Result.createSingleDependency(cacheValue, ProjectRootManager.getInstance(project));
    });
  }

  public static List<PublishEventPointDescriptor> findEventPublishingPoints(Project project, @Nullable Module module, GlobalSearchScope scope) {
    var descriptors = new LinkedHashSet<PublishEventPointDescriptor>();
    for (PsiMethod publishEventMethod : getPublishEventMethods(project, module)) {
      descriptors.addAll(searchPublishPoints(publishEventMethod, scope));
    }
    for (PsiMethod publishEventMethod2 : getMulticastEventMethods(project, module)) {
      descriptors.addAll(searchPublishPoints(publishEventMethod2, scope));
    }
    descriptors.addAll(searchEventListenerDescriptors(project, module));
    return descriptors.isEmpty() ? List.of() : new ArrayList<>(descriptors);
  }

  private static List<PsiMethod> getPublishEventMethods(Project project, @Nullable Module module) {
    return getEventMethods(project, module, APPLICATION_EVENT_PUBLISHER, PUBLISH_EVENT_METHOD);
  }

  private static List<PsiMethod> getMulticastEventMethods(Project project, @Nullable Module module) {
    return getEventMethods(project, module, APPLICATION_EVENT_MULTICASTER, MULTICAST_EVENT_METHOD);
  }

  private static List<PsiMethod> getEventMethods(
          Project project, @Nullable Module module, String className, String methodName) {
    if (module != null) {
      PsiClass eventPublisherClass = CommonUtils.findLibraryClass(module, className);
      if (eventPublisherClass != null) {
        return List.of(eventPublisherClass.findMethodsByName(methodName, false));
      }
      return List.of();
    }
    JavaPsiFacade javaPsi = JavaPsiFacade.getInstance(project);
    PsiClass[] classes = javaPsi.findClasses(className, GlobalSearchScope.allScope(project));
    if (classes.length == 0) {
      return List.of();
    }
    return Arrays.stream(classes).flatMap(c -> Arrays.stream(c.findMethodsByName(methodName, false))).collect(Collectors.toList());
  }

  public static List<EventListenerElement> getEventListeners(Project project, @Nullable Module module, PsiType publishType) {
    if (!publishType.isValid()) {
      return ContainerUtil.emptyList();
    }
    return ContainerUtil.filter(getEventListeners(project, module), listener -> {
      for (PsiType eventType : getEventListenerHandledType(listener)) {
        if (eventType.isAssignableFrom(publishType)) {
          return true;
        }
      }
      return false;
    });
  }

  private static List<EventListenerElement> getEventListeners(Project project, @Nullable Module module) {
    if (module != null) {
      return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
        SmartList<EventListenerElement> smartList = new SmartList<>();
        findModuleEventListeners(module, smartList, moduleNetworkScope(module), GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
        return CachedValueProvider.Result.create(smartList,
                UastModificationTracker.getInstance(module.getProject()),
                ProjectRootManager.getInstance(module.getProject())
        );
      });
    }
    return getProjectEventListeners(project);
  }

  private static List<EventListenerElement> getProjectEventListeners(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      SmartList<EventListenerElement> smartList = new SmartList<>();
      for (Module m : ModuleManager.getInstance(project).getModules()) {
        if (InfraLibraryUtil.hasLibrary(m)
                && CommonUtils.hasFacet(m) && !m.getName().endsWith(".test")) {
          findModuleEventListeners(m, smartList, GlobalSearchScope.moduleScope(m), GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(m));
        }
      }
      return CachedValueProvider.Result.create(smartList, UastModificationTracker.getInstance(project), ProjectRootManager.getInstance(project));
    });
  }

  private static void findModuleEventListeners(Module module, List<EventListenerElement> result, GlobalSearchScope scope, GlobalSearchScope apiScope) {
    JamService jamService = JamService.getJamService(module.getProject());
    result.addAll(jamService.getJamMethodElements(JamEventListenerElement.SEM_KEY, AnnotationConstant.EVENT_LISTENER, scope));

    collectEventListenerBeans(module, scope, apiScope, result);
  }

  private static void collectEventListenerBeans(Module module, GlobalSearchScope scope, GlobalSearchScope apiScope, List<EventListenerElement> result) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
    for (PsiClass listenerInterface : javaPsiFacade.findClasses(APPLICATION_LISTENER, apiScope)) {
      PsiClass applicationEventClass = javaPsiFacade.findClass(APPLICATION_EVENT, apiScope);
      Query<PsiClass> entries = ClassInheritorsSearch.search(listenerInterface, scope, true, true, false);
      for (PsiClass entry : entries) {
        if (CommonUtils.isBeanCandidateClass(entry)) {
          PsiMethod[] onEventMethods = entry.findMethodsByName(ON_APPLICATION_EVENT_METHOD, false);
          for (PsiMethod method : onEventMethods) {
            if (isValidReceiverMethod(method, applicationEventClass)) {
              BeanEventListenerElement beanEventListener = BeanEventListenerElement.from(method);
              if (beanEventListener != null) {
                result.add(beanEventListener);
              }
            }
          }
        }
      }
    }
  }

  private static boolean isValidReceiverMethod(PsiMethod method, PsiClass applicationEventClass) {
    if (!PsiType.VOID.equals(method.getReturnType()) || method.getParameterList().getParametersCount() != 1) {
      return false;
    }
    PsiParameter parameter = method.getParameterList().getParameters()[0];
    if (parameter.getType() instanceof PsiClassType) {
      String parameterClassName = ((PsiClassType) parameter.getType()).getClassName();
      if (parameterClassName == null || parameterClassName.equals(APPLICATION_EVENT_SHORT_NAME)) {
        PsiClass resolvedClass = ((PsiClassType) parameter.getType()).resolve();
        return resolvedClass != null && !resolvedClass.isEquivalentTo(applicationEventClass);
      }
      return true;
    }
    return true;
  }

  private static Collection<PublishEventPointDescriptor> searchEventListenerDescriptors(Project project, @Nullable Module module) {
    PsiType returnType;
    if (module == null) {
      return List.of();
    }
    List<PublishEventPointDescriptor> descriptors = new ArrayList<>();
    for (EventListenerElement listener : getEventListeners(project, module)) {
      PsiMethod psiMethod = listener.getPsiElement();
      if (psiMethod != null && (returnType = psiMethod.getReturnType()) != null && !PsiType.VOID.equals(returnType)) {
        descriptors.add(PublishEventPointDescriptor.create(psiMethod, module));
      }
    }
    return descriptors;
  }

  private static List<PublishEventPointDescriptor> searchPublishPoints(PsiMethod publishEventMethod, SearchScope searchScope) {
    var result = new ArrayList<PublishEventPointDescriptor>();
    MethodReferencesSearch.search(publishEventMethod, searchScope, true).forEach(psiReference -> {
      UElement expression = UastContextKt.toUElement(psiReference.getElement());
      UCallExpression callExpression = UastUtils.getUCallExpression(expression);
      if (callExpression != null) {
        List<UExpression> arguments = callExpression.getValueArguments();
        if (arguments.size() > 0) {
          result.add(PublishEventPointDescriptor.create(arguments.get(0)));
          return true;
        }
        return true;
      }
      return true;
    });
    return result;
  }

  public static Collection<PsiType> getEventListenerHandledType(EventListenerElement eventListener) {
    PsiMethod psiMethod = eventListener.getPsiElement();
    if (psiMethod == null) {
      return List.of();
    }
    PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
    if (parameters.length == 1) {
      return Collections.singleton(parameters[0].getType());
    }
    else if (parameters.length == 0) {
      return ContainerUtil.mapNotNull(eventListener.getEventListenerClasses(), PsiTypesUtil::getClassType);
    }
    else {
      return List.of();
    }
  }

  public static boolean isPublishEventExpression(UCallExpression psiElement) {
    return isMethodCallExpression(psiElement, APPLICATION_EVENT_PUBLISHER, PUBLISH_EVENT_METHOD);
  }

  public static boolean isMulticastEventExpression(UCallExpression psiElement) {
    return isMethodCallExpression(psiElement, APPLICATION_EVENT_MULTICASTER, MULTICAST_EVENT_METHOD);
  }

  private static boolean isMethodCallExpression(UCallExpression psiElement, String className, String methodName) {
    PsiMethod element;
    PsiClass containingClass;
    return methodName.equals(psiElement.getMethodName())
            && (element = psiElement.resolve()) != null
            && (containingClass = element.getContainingClass()) != null
            && InheritanceUtil.isInheritor(containingClass, className);
  }

  @Nullable
  public static PsiType getEventType(@Nullable PsiType methodReturnType, @Nullable Module module) {
    if (methodReturnType instanceof PsiArrayType) {
      return ((PsiArrayType) methodReturnType).getComponentType();
    }
    if (methodReturnType instanceof PsiClassType returnType) {
      PsiType[] parameters = returnType.getParameters();
      if (parameters.length == 1 && isEventWrapperType(returnType.resolve(), module)) {
        PsiType parameter = parameters[0];
        if (parameter instanceof PsiClassReferenceType psiClassReferenceType) {
          PsiClass resolvedType = psiClassReferenceType.resolve();
          if (resolvedType != null && "java.lang.Void".equals(resolvedType.getQualifiedName())) {
            return PsiType.VOID;
          }
          return parameter;
        }
      }
    }
    return methodReturnType;
  }

  private static boolean isEventWrapperType(@Nullable PsiClass resolvedClass, @Nullable Module module) {
    if (resolvedClass == null) {
      return false;
    }
    String qualifiedName = resolvedClass.getQualifiedName();
    return EVENT_WRAPPER_CLASSES.contains(qualifiedName) || (
            (ASYNC_EVENT_WRAPPER_CLASSES.contains(qualifiedName) || ReactorConstants.REACTIVE_EVENT_WRAPPER_CLASSES.contains(qualifiedName))
                    && JavaLibraryUtils.hasLibraryClass(module, ReactorConstants.FLUX)
    );
  }
}
