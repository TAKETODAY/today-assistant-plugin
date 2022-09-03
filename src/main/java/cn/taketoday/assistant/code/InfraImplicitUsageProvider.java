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

package cn.taketoday.assistant.code;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.jam.JamService;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.JavaClassInfo;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/27 21:44
 */
final class InfraImplicitUsageProvider implements ImplicitUsageProvider {
  private static final List<String> WRITE_ANNOTATIONS = List.of(
          AnnotationConstant.AUTOWIRED,
          AnnotationConstant.REQUIRED,
          AnnotationConstant.BEAN,
          AnnotationConstant.COMPONENT,
          AnnotationConstant.VALUE,
          AnnotationConstant.JMX_MANAGED_OPERATION,
          AnnotationConstant.JMX_MANAGED_ATTRIBUTE,
          AnnotationConstant.SCHEDULED,
          AnnotationConstant.SCHEDULES,
          AnnotationConstant.EVENT_LISTENER,
          AnnotationConstant.TRANSACTIONAL_EVENT_LISTENER,
          AnnotationConstant.TEST_BEFORE_TRANSACTION,
          AnnotationConstant.TEST_AFTER_TRANSACTION
  );

  private static final List<String> ENTRY_POINTS = List.of(
          AnnotationConstant.BEAN,
          AnnotationConstant.COMPONENT,
          AnnotationConstant.JMX_MANAGED_OPERATION,
          AnnotationConstant.JMX_MANAGED_ATTRIBUTE,
          AnnotationConstant.SCHEDULED,
          AnnotationConstant.SCHEDULES,
          AnnotationConstant.EVENT_LISTENER,
          AnnotationConstant.TRANSACTIONAL_EVENT_LISTENER,
          "javax.annotation.PostConstruct",
          "jakarta.annotation.PostConstruct",
          "javax.annotation.PreDestroy",
          "jakarta.annotation.PreDestroy"
  );

  private static final List<String> NON_WRITE_ANNOTATIONS = List.of(
          JavaeeConstant.JAVAX_INJECT,
          JavaeeConstant.JAKARTA_INJECT,
          JavaeeConstant.JAVAX_RESOURCE,
          JavaeeConstant.JAKARTA_RESOURCE
  );

  private static final List<String> READ_ANNOTATIONS = List.of(AnnotationConstant.VALUE);
  private static final List<String> REGULAR_COMPONENT_ANNOTATIONS = List.of(AnnotationConstant.SERVICE, AnnotationConstant.COMPONENT);

  public boolean isImplicitUsage(PsiElement element) {
    return hasWriteAnnotation(element)
            || isBeanSetterOrLifeCycleMethod(element)
            || isBeanClassOrConstructor(element)
            || isDynamicPropertySource(element)
            || isJsonComponentClass(element);
  }

  public boolean isImplicitRead(PsiElement element) {
    return (element instanceof PsiMethod) && AnnotationUtil.isAnnotated((PsiMethod) element, READ_ANNOTATIONS, 0);
  }

  private static boolean isMetaAnnotated(PsiModifierListOwner element) {
    Module module;
    if (!(element instanceof PsiClass) && (module = ModuleUtilCore.findModuleForPsiElement(element)) != null) {
      List<String> annotations = getWriteMetaAnnotations(module);
      return AnnotationUtil.isAnnotated(element, annotations, 0);
    }
    return false;
  }

  private static List<String> getWriteMetaAnnotations(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      return CachedValueProvider.Result.create(getMetaAnnotations(module,
                      AnnotationConstant.SCHEDULED,
                      AnnotationConstant.EVENT_LISTENER,
                      AnnotationConstant.AUTOWIRED,
                      AnnotationConstant.COMPONENT,
                      AnnotationConstant.VALUE
              ),
              UastModificationTracker.getInstance(module.getProject()));
    });
  }

  private static List<String> getMetaAnnotations(Module module, String... annotations) {
    SmartList<PsiClass> smartList = new SmartList<>();
    for (String annotation : annotations) {
      smartList.addAll(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotation, false));
    }
    List<String> fqns = ContainerUtil.mapNotNull(smartList, PsiClass::getQualifiedName);
    ContainerUtil.removeAll(fqns, annotations);
    if (!fqns.isEmpty()) {
      return fqns;
    }
    return Collections.emptyList();
  }

  public boolean isImplicitWrite(PsiElement element) {
    return hasWriteAnnotation(element)
            || isBeanSetterOrLifeCycleMethod(element)
            || ((element instanceof PsiModifierListOwner) && AnnotationUtil.isAnnotated((PsiModifierListOwner) element, NON_WRITE_ANNOTATIONS, 0));
  }

  private static boolean hasWriteAnnotation(PsiElement element) {
    PsiModifierListOwner modifierListOwner;
    PsiModifierList modifierList;
    if (!(element instanceof PsiModifierListOwner)
            || (element instanceof PsiParameter)
            || (element instanceof PsiLocalVariable)
            || (modifierList = (modifierListOwner = (PsiModifierListOwner) element).getModifierList()) == null
            || modifierList.getAnnotations().length == 0) {
      return false;
    }
    if (InfraUtils.hasFacets(element.getProject())
            || InfraLibraryUtil.hasLibrary(element.getProject())) {
      return AnnotationUtil.isAnnotated(modifierListOwner, WRITE_ANNOTATIONS, 0)
              || isMetaAnnotated(modifierListOwner);
    }
    return false;
  }

  private static boolean isBeanSetterOrLifeCycleMethod(PsiElement element) {
    Module module;
    if (element instanceof PsiMethod method) {
      if (isEntryPoint(method)) {
        return true;
      }
      int parametersCount = method.getParameterList().getParametersCount();
      if (parametersCount > 1) {
        return false;
      }
      PsiClass psiClass = method.getContainingClass();
      PsiIdentifier identifier = method.getNameIdentifier();
      if (!InfraUtils.isBeanCandidateClassInProject(psiClass)
              || identifier == null
              || (module = ModuleUtilCore.findModuleForPsiElement(psiClass)) == null) {
        return false;
      }
      if (InfraUtils.hasXmlConfigs(module)) {
        JavaClassInfo info = JavaClassInfo.from(psiClass);
        if (info.isMappedProperty(method)) {
          return true;
        }
        if (PropertyUtilBase.isSimplePropertySetter((PsiMethod) element) && info.isAutowired() && AutowiredAnnotator.checkAutowiredMethod(method, null, info, identifier)) {
          return true;
        }
        return parametersCount == 0 && !info.getMethodTypes(method).isEmpty();
      }
      PsiManager psiManager = PsiManager.getInstance(module.getProject());
      for (PsiMethod wiredMethod : getWiredConfigurationLifecycleMethods(module)) {
        if (wiredMethod.isValid() && psiManager.areElementsEquivalent(method, wiredMethod)) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  private static List<PsiMethod> getWiredConfigurationLifecycleMethods(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      List<ContextJavaBean> factoryBeanMethods = JamService.getJamService(module.getProject())
              .getJamMethodElements(ContextJavaBean.BEAN_JAM_KEY, AnnotationConstant.BEAN, GlobalSearchScope.moduleScope(module));
      List<PsiMethod> wiredMethods = new ArrayList<>();
      for (ContextJavaBean beanMethod : factoryBeanMethods) {
        ContainerUtil.addIfNotNull(wiredMethods, beanMethod.getInitMethodAttributeElement().getValue());
        ContainerUtil.addIfNotNull(wiredMethods, beanMethod.getDestroyMethodAttributeElement().getValue());
      }
      Set<PsiFile> files = new HashSet<>(ContainerUtil.mapNotNull(wiredMethods, PsiElement::getContainingFile));
      List<Object> dependencies = new ArrayList<>(files);
      dependencies.add(InfraModificationTrackersManager.from(module.getProject()).getOuterModelsModificationTracker());
      dependencies.add(ProjectRootManager.getInstance(module.getProject()));
      return CachedValueProvider.Result.create(List.copyOf(wiredMethods), dependencies);
    });
  }

  private static boolean isBeanClassOrConstructor(PsiElement element) {
    if (element instanceof PsiClass) {
      return isMappedBeanClass((PsiClass) element, false);
    }
    if ((element instanceof PsiMethod) && ((PsiMethod) element).isConstructor()) {
      return isMappedBeanClass(((PsiMethod) element).getContainingClass(), true);
    }
    return false;
  }

  private static boolean isMappedBeanClass(@Nullable PsiClass element, boolean forMembers) {
    if (element != null && !element.isInterface()) {
      if (AnnotationUtil.isAnnotated(element, REGULAR_COMPONENT_ANNOTATIONS, 0)) {
        if (hasSuperClasses(element) || ContainerUtil.exists(element.getMethods(), InfraImplicitUsageProvider::isEntryPoint)) {
          return true;
        }
        if (!forMembers) {
          return false;
        }
      }
      return InfraUtils.isBeanCandidateClassInProject(element) && InfraUtils.isStereotypeComponentOrMeta(element);
    }
    return false;
  }

  public static boolean isEntryPoint(PsiMethod method) {
    return !method.hasModifier(JvmModifier.ABSTRACT) && AnnotationUtil.isAnnotated(method, ENTRY_POINTS, 0);
  }

  private static boolean hasSuperClasses(PsiClass element) {
    PsiClass[] supers;
    for (PsiClass superClass : element.getSupers()) {
      if (superClass != null && !"java.lang.Record".equals(superClass.getQualifiedName()) && !"java.lang.Object".equals(superClass.getQualifiedName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDynamicPropertySource(PsiElement element) {
    if (element instanceof PsiMethod psiMethod) {
      return psiMethod.hasModifier(JvmModifier.STATIC) && AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.DYNAMIC_PROPERTY_SOURCE, 0);
    }
    return false;
  }

  private static boolean isJsonComponentClass(PsiElement element) {
    if (!(element instanceof PsiClass psiClass)) {
      return false;
    }
    if (psiClass.isAnnotationType() || psiClass.isInterface() || psiClass.isEnum()) {
      return false;
    }
    PsiClass targetClass = psiClass.getContainingClass();
    if (targetClass == null) {
      targetClass = psiClass;
    }
    if (!AnnotationUtil.isAnnotated(targetClass, AnnotationConstant.JSON_COMPONENT, 0)) {
      return false;
    }
    return InheritanceUtil.isInheritor(psiClass, "com.fasterxml.jackson.databind.JsonDeserializer")
            || InheritanceUtil.isInheritor(psiClass, "com.fasterxml.jackson.databind.JsonSerializer");
  }
}
