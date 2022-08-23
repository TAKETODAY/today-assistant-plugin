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

package cn.taketoday.assistant.beans;

import com.intellij.codeInsight.AnnotationTargetUtil;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.jam.JamService;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiConstantEvaluationHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringManager;
import com.intellij.spring.SpringModelVisitorUtils;
import com.intellij.spring.contexts.model.CombinedSpringModel;
import com.intellij.spring.contexts.model.CombinedSpringModelImpl;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.facet.SpringFileSet;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringConditional;
import com.intellij.spring.model.SpringImplicitBeanMarker;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.SpringObjectFactoryEffectiveTypeProvider;
import com.intellij.spring.model.highlighting.xml.SpringConstructorArgResolveUtil;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.JamSpringBeanPointer;
import com.intellij.spring.model.jam.contexts.CustomContextJavaBean;
import com.intellij.spring.model.jam.qualifiers.SpringJamQualifier;
import com.intellij.spring.model.jam.testContexts.SpringTestContextUtil;
import com.intellij.spring.model.utils.ExplicitRedefinitionAwareBeansCollector;
import com.intellij.spring.model.utils.SpringBeanUtils;
import com.intellij.spring.model.utils.SpringConstructorArgUtils;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.beans.Autowire;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.model.xml.beans.ConstructorArg;
import com.intellij.spring.model.xml.beans.DefaultableBoolean;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.model.xml.beans.SpringProperty;
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition;
import com.intellij.spring.model.xml.beans.SpringValue;
import com.intellij.spring.references.SpringBeanReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.beans.stereotype.SpringJamModel;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.assistant.util.JamAnnotationTypeUtil;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:39
 */

public final class AutowireUtil {
  private static final Set<String> AUTOWIRED_ANNOTATIONS = Set.of(
          AnnotationConstant.AUTOWIRED,
          JavaeeConstant.JAVAX_INJECT,
          JavaeeConstant.JAKARTA_INJECT,
          JavaeeConstant.JAVAX_RESOURCE,
          JavaeeConstant.JAKARTA_RESOURCE
  );

  private static final Set<String> STANDARD_AUTOWIRED_TYPES = ContainerUtil.immutableSet(
          InfraConstant.BEAN_FACTORY_CLASS, InfraConstant.APPLICATION_CONTEXT,
          InfraConstant.APPLICATION_EVENT_PUBLISHER,
          InfraConstant.APPLICATION_EVENT_MULTICASTER,
          InfraConstant.MESSAGE_SOURCE,
          InfraConstant.RESOURCE_LOADER, InfraConstant.ENVIRONMENT_CLASS);

  public static Map<PsiMethod, Collection<SpringBeanPointer<?>>> getByTypeAutowiredProperties(SpringBean springBean, CommonSpringModel model) {
    Map<PsiMethod, Collection<SpringBeanPointer<?>>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null && model != null && isByTypeAutowired(springBean)) {
      for (PsiMethod psiMethod : beanClass.getAllMethods()) {
        if (isPropertyAutowired(psiMethod, springBean)) {
          PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
          Collection<SpringBeanPointer<?>> list = new HashSet<>();
          PsiAnnotation qualifiedAnnotation = getQualifiedAnnotation(psiMethod);
          if (qualifiedAnnotation != null) {
            list.addAll(getQualifiedBeans(qualifiedAnnotation, model));
          }
          else {
            list.addAll(autowireByType(model, parameter.getType(), parameter.getName()));
            list.addAll(autowireByType(model, getAutowiredEffectiveBeanTypes(parameter.getType()), parameter.getName()));
          }
          if (list.size() > 0) {
            autowiredMap.put(psiMethod, list);
          }
        }
      }
    }
    return autowiredMap;
  }

  public static Set<SpringBeanPointer<?>> excludeAutowireCandidates(Collection<SpringBeanPointer<?>> beans, @Nullable CommonSpringModel model) {
    return excludeAutowireCandidates(beans, null, model);
  }

  public static Set<SpringBeanPointer<?>> excludeAutowireCandidates(Collection<SpringBeanPointer<?>> beans, @Nullable String primaryCandidateName, @Nullable CommonSpringModel model) {
    Set<SpringBeanPointer<?>> pointers = new LinkedHashSet<>();
    Collection<SpringBeanPointer<?>> primaryBeans = beans.size() > 1 ? getPrimaryBeans(beans, primaryCandidateName, model) : beans;
    if (!primaryBeans.isEmpty()) {
      for (SpringBeanPointer primaryBean : primaryBeans) {
        if (isAutowireCandidate(primaryBean)) {
          pointers.add(primaryBean);
        }
      }
      return pointers;
    }
    for (SpringBeanPointer beanPointer : beans) {
      if (isAutowireCandidate(beanPointer)) {
        pointers.add(beanPointer);
      }
    }
    return pointers;
  }

  private static List<SpringBeanPointer<?>> getPrimaryBeans(Collection<? extends SpringBeanPointer<?>> beans, @Nullable String primaryCandidateName, @Nullable CommonSpringModel model) {
    if (primaryCandidateName == null) {
      return emptyList();
    }
    List<SpringBeanPointer<?>> byPrimary = new SmartList<>();
    List<SpringBeanPointer<?>> byName = new SmartList<>();
    for (SpringBeanPointer springBeanPointer : beans) {
      if (springBeanPointer.isValid()) {
        CommonSpringBean springBean = springBeanPointer.getSpringBean();
        if (springBean.isPrimary()) {
          if (isMyName(primaryCandidateName, springBeanPointer, model)) {
            return Collections.singletonList(springBeanPointer);
          }
          byPrimary.add(springBeanPointer);
        }
        else if (isMyName(primaryCandidateName, springBeanPointer, model)) {
          byName.add(springBeanPointer);
        }
      }
    }
    return byPrimary.isEmpty() ? byName : byPrimary;
  }

  private static boolean isMyName(@Nullable String name, SpringBeanPointer<?> springBeanPointer, @Nullable CommonSpringModel model) {
    if (name == null) {
      return false;
    }
    String beanName = springBeanPointer.getName();
    if (name.equals(beanName)) {
      return true;
    }
    if (beanName != null && model != null) {
      for (String aliasName : SpringModelVisitorUtils.getAllBeanNames(model, springBeanPointer)) {
        if (name.equals(aliasName)) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  private static boolean isAutowireCandidate(@Nullable SpringBeanPointer<?> pointer) {
    if (pointer == null || !pointer.isValid()) {
      return false;
    }
    var getSpringBean = pointer.getSpringBean();
    if (!(getSpringBean instanceof SpringBean)) {
      return true;
    }
    if (((SpringBean) getSpringBean).isAbstract()) {
      return false;
    }
    DefaultableBoolean autoWireCandidate = ((SpringBean) getSpringBean).getAutowireCandidate().getValue();
    return (autoWireCandidate == null || autoWireCandidate.getBooleanValue()) && isDefaultAutowireCandidate(getSpringBean);
  }

  private static boolean isDefaultAutowireCandidate(CommonSpringBean springBean) {
    Beans beans;
    if ((springBean instanceof SpringBean) && (beans = ((SpringBean) springBean).getParentOfType(Beans.class, false)) != null) {
      String autowireCandidates = beans.getDefaultAutowireCandidates().getValue();
      if (StringUtil.isNotEmpty(autowireCandidates)) {
        String beanName = springBean.getBeanName();
        String[] aliases = springBean.getAliases();
        for (String patternText : StringUtil.split(autowireCandidates, ",")) {
          Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(patternText.trim()));
          if (isMatched(pattern, beanName)) {
            return true;
          }
          for (String alias : aliases) {
            if (isMatched(pattern, alias)) {
              return true;
            }
          }
        }
        return false;
      }
      return true;
    }
    return true;
  }

  private static boolean isMatched(Pattern pattern, @Nullable String beanName) {
    return beanName != null && pattern.matcher(beanName).matches();
  }

  public static Map<PsiType, Collection<SpringBeanPointer<?>>> getConstructorAutowiredProperties(SpringBean springBean, CommonSpringModel model) {
    PsiMethod springBeanConstructor;
    Collection<SpringBeanPointer<?>> autowireByType;
    Map<PsiType, Collection<SpringBeanPointer<?>>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null && isConstructorAutowire(springBean)) {
      boolean instantiatedByFactory = SpringConstructorArgResolveUtil.isInstantiatedByFactory(springBean);
      if (instantiatedByFactory) {
        springBeanConstructor = springBean.getFactoryMethod().getValue();
      }
      else {
        springBeanConstructor = SpringConstructorArgUtils.getInstance().getSpringBeanConstructor(springBean, model);
      }
      PsiMethod checkedMethod = springBeanConstructor;
      if (checkedMethod != null) {
        List<ConstructorArg> list = springBean.getConstructorArgs();
        Map<Integer, ConstructorArg> indexedArgs = SpringConstructorArgResolveUtil.getIndexedConstructorArgs(list);
        PsiParameter[] parameters = checkedMethod.getParameterList().getParameters();
        SmartList smartList = new SmartList();
        for (int i = 0; i < parameters.length; i++) {
          PsiParameter parameter = parameters[i];
          if (!SpringConstructorArgResolveUtil.acceptParameter(parameter, new ArrayList<>(springBean.getConstructorArgs()), indexedArgs, i, smartList)) {
            PsiType psiType = parameter.getType();
            PsiAnnotation qualifiedAnnotation = getQualifiedAnnotation(parameter);
            if (qualifiedAnnotation != null) {
              autowireByType = getQualifiedBeans(qualifiedAnnotation, model);
            }
            else {
              autowireByType = autowireByType(model, getAutowiredEffectiveBeanTypes(psiType));
            }
            Collection<SpringBeanPointer<?>> springBeans = autowireByType;
            if (!springBeans.isEmpty()) {
              autowiredMap.put(psiType, springBeans);
            }
          }
        }
      }
    }
    return autowiredMap;
  }

  public static boolean isPropertyNotDefined(SpringBean springBean, String propertyName) {
    return springBean.getProperty(propertyName) == null;
  }

  public static Map<PsiMethod, SpringBeanPointer<?>> getByNameAutowiredProperties(SpringBean springBean) {
    Map<PsiMethod, SpringBeanPointer<?>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null) {
      CommonSpringModel model = SpringModelUtils.getInstance().getSpringModelByBean(springBean);
      if (isByNameAutowired(springBean)) {
        for (PsiMethod psiMethod : beanClass.getAllMethods()) {
          if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
            PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
            Collection<SpringBeanPointer<?>> list = autowireByType(model, getAutowiredEffectiveBeanTypes(parameter.getType()));
            String propertyName = PropertyUtilBase.getPropertyNameBySetter(psiMethod);
            for (SpringBeanPointer pointer : list) {
              if (pointer.isValid() && SpringBeanUtils.getInstance().findBeanNames(pointer.getSpringBean()).contains(propertyName) && isPropertyNotDefined(springBean, propertyName)) {
                autowiredMap.put(psiMethod, pointer);
              }
            }
          }
        }
      }
    }
    return autowiredMap;
  }

  private static boolean isPropertyAutowired(PsiMethod psiMethod, SpringBean springBean) {
    if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
      PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
      PsiType type = parameter.getType();
      if (type instanceof PsiClassType classType) {
        PsiClass psiClass = classType.resolve();
        return psiClass != null
                && isPropertyNotDefined(springBean, PropertyUtilBase.getPropertyNameBySetter(psiMethod));
      }
      return false;
    }
    return false;
  }

  public static boolean isByTypeAutowired(SpringBean springBean) {
    return springBean.getBeanAutowire().equals(Autowire.BY_TYPE);
  }

  public static boolean isByNameAutowired(SpringBean springBean) {
    return springBean.getBeanAutowire().equals(Autowire.BY_NAME);
  }

  public static boolean isConstructorAutowire(SpringBean springBean) {
    return springBean.getBeanAutowire().equals(Autowire.CONSTRUCTOR);
  }

  public static Map<PsiMember, Set<SpringBeanPointer<?>>> getAutowireAnnotationProperties(CommonSpringBean springBean, CommonSpringModel model) {
    Map<PsiMember, Set<SpringBeanPointer<?>>> map = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass != null) {
      for (PsiMethod psiMethod : getAnnotatedAutowiredMethods(beanClass)) {
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
          PsiAnnotation psiAnnotation = getQualifiedAnnotation(parameter);
          if (psiAnnotation != null) {
            addAutowiredBeans(map, psiMethod, getQualifiedBeans(psiAnnotation, model), model);
          }
          else {
            addAutowiredBeans(map, psiMethod, SpringBeanCoreUtils.getBeansByType(parameter.getType(), model), model);
          }
        }
      }
      for (PsiField psiField : getAnnotatedAutowiredFields(beanClass)) {
        PsiAnnotation psiAnnotation2 = getQualifiedAnnotation(psiField);
        if (psiAnnotation2 != null) {
          addAutowiredBeans(map, psiField, getQualifiedBeanPointers(psiAnnotation2, model), model);
        }
        else {
          addAutowiredBeans(map, psiField, getAutowiredBeansFor(psiField, psiField.getType(), model), model);
        }
      }
    }
    return map;
  }

  private static void addAutowiredBeans(Map<PsiMember, Set<SpringBeanPointer<?>>> map, PsiMember psiMember, Collection<SpringBeanPointer<?>> beans,
          CommonSpringModel model) {
    Set<SpringBeanPointer<?>> list = excludeAutowireCandidates(beans, null, model);
    if (!list.isEmpty()) {
      if (!map.containsKey(psiMember)) {
        map.put(psiMember, list);
      }
      else {
        map.get(psiMember).addAll(list);
      }
    }
  }

  public static Set<SpringBeanPointer<?>> getQualifiedBeans(PsiAnnotation psiAnnotation, @Nullable CommonSpringModel model) {
    if (model == null) {
      return Collections.emptySet();
    }
    SpringJamQualifier qualifier = getQualifier(null, psiAnnotation);
    Set<SpringBeanPointer<?>> findQualifiedBeans = SpringModelVisitorUtils.findQualifiedBeans(model, qualifier);
    return findQualifiedBeans;
  }

  @Nullable
  public static PsiAnnotation getQualifiedAnnotation(PsiModifierListOwner modifierListOwner) {
    return getQualifiedAnnotation(modifierListOwner, ModuleUtilCore.findModuleForPsiElement(modifierListOwner));
  }

  @Nullable
  private static PsiAnnotation getQualifiedAnnotation(PsiModifierListOwner modifierListOwner, @Nullable Module module) {
    PsiMethod psiMethod;
    PsiAnnotation annotation;
    if (module == null) {
      return null;
    }
    List<PsiClass> annotationTypeClasses = JamAnnotationTypeUtil.getQualifierAnnotationTypesWithChildren(module);
    for (PsiClass annotationTypeClass : annotationTypeClasses) {
      if (((modifierListOwner instanceof PsiField) && AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass,
              PsiAnnotation.TargetType.FIELD) != null) || (((modifierListOwner instanceof PsiParameter)
              && AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.PARAMETER) != null)
              || ((modifierListOwner instanceof PsiMethod)
              && AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.METHOD) != null))) {
        PsiAnnotation annotation2 = AnnotationUtil.findAnnotation(modifierListOwner, true, annotationTypeClass.getQualifiedName());
        if (annotation2 != null) {
          return annotation2;
        }
      }
    }
    if ((modifierListOwner instanceof PsiParameter)
            && (psiMethod = PsiTreeUtil.getParentOfType(modifierListOwner, PsiMethod.class)) != null && isAutowiredByAnnotation(psiMethod)) {
      for (PsiClass annotationTypeClass2 : annotationTypeClasses) {
        if (AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass2, PsiAnnotation.TargetType.METHOD) != null && (annotation = AnnotationUtil.findAnnotation(
                psiMethod, true, annotationTypeClass2.getQualifiedName())) != null) {
          return annotation;
        }
      }
      return null;
    }
    return null;
  }

  @Nullable
  public static PsiAnnotation getAutowiredAnnotation(PsiModifierListOwner owner) {
    PsiModifierList modifierList = owner.getModifierList();
    if (modifierList != null) {
      for (String annotation : AUTOWIRED_ANNOTATIONS) {
        PsiAnnotation autowireAnnotation = modifierList.findAnnotation(annotation);
        if (autowireAnnotation != null) {
          return autowireAnnotation;
        }
      }
      return null;
    }
    return null;
  }

  @Nullable
  public static PsiAnnotation getResourceAnnotation(PsiModifierListOwner owner) {
    PsiModifierList modifierList = owner.getModifierList();
    if (modifierList != null) {
      PsiAnnotation annotation = modifierList.findAnnotation(JavaeeConstant.JAVAX_RESOURCE);
      return annotation != null ? annotation : modifierList.findAnnotation(JavaeeConstant.JAKARTA_RESOURCE);
    }
    return null;
  }

  public static boolean isAutowiredByAnnotation(PsiModifierListOwner owner) {
    PsiModifierList modifierList = owner.getModifierList();
    if (modifierList == null || modifierList.hasModifierProperty("static") || modifierList.getAnnotations().length == 0) {
      return false;
    }
    Set<String> autowireAnnotations = getAutowiredAnnotations(ModuleUtilCore.findModuleForPsiElement(owner));
    return AnnotationUtil.isAnnotated(owner, autowireAnnotations, 0);
  }

  public static boolean isInjectionPoint(PsiMethod psiMethod) {
    boolean hasParameters = psiMethod.getParameterList().getParametersCount() != 0;
    return (hasParameters && isAutowiredByAnnotation(psiMethod)) || AnnotationUtil.isAnnotated(psiMethod, AnnotationConstant.BEAN, 1) || isCustomStereotypeBean(
            psiMethod) || (hasParameters && isDefaultStereotypeComponentConstructor(psiMethod));
  }

  public static boolean isDefaultStereotypeComponentConstructor(PsiMethod method) {
    PsiClass containingClass;
    return method.isConstructor() && (containingClass = method.getContainingClass()) != null && containingClass.getConstructors().length == 1 && CommonUtils.isStereotypeComponentOrMeta(
            containingClass);
  }

  public static boolean isCustomStereotypeBean(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(CustomContextJavaBean.JAM_KEY, method) != null;
  }

  public static boolean isRequired(PsiModifierListOwner owner) {
    PsiAnnotation autowiredAnnotation;
    Boolean value;
    PsiModifierList modifierList = owner.getModifierList();
    if (modifierList != null) {
      PsiAnnotation required = modifierList.findAnnotation(AnnotationConstant.REQUIRED);
      return required != null || (autowiredAnnotation = getAutowiredAnnotation(owner)) == null || (value = JamCommonUtil.getObjectValue(autowiredAnnotation.findAttributeValue("required"),
              Boolean.class)) == null || value.booleanValue();
    }
    return true;
  }

  public static List<PsiMethod> getAnnotatedAutowiredMethods(PsiClass psiClass) {
    SmartList smartList = new SmartList();
    for (PsiModifierListOwner psiModifierListOwner : psiClass.getAllMethods()) {
      if (isAutowiredByAnnotation(psiModifierListOwner)) {
        smartList.add(psiModifierListOwner);
      }
    }
    return smartList;
  }

  public static List<PsiField> getAnnotatedAutowiredFields(PsiClass psiClass) {
    SmartList smartList = new SmartList();
    for (PsiModifierListOwner psiModifierListOwner : psiClass.getAllFields()) {
      if (isAutowiredByAnnotation(psiModifierListOwner)) {
        smartList.add(psiModifierListOwner);
      }
    }
    return smartList;
  }

  public static Set<SpringBeanPointer<?>> autowireByType(CommonSpringModel model, PsiType psiType) {
    return autowireByType(model, psiType, null);
  }

  public static Set<SpringBeanPointer<?>> autowireByType(CommonSpringModel model, PsiType psiType, @Nullable String primaryCandidateName) {
    return autowireByType(model, psiType, primaryCandidateName, true);
  }

  public static Set<SpringBeanPointer<?>> autowireByType(CommonSpringModel model, PsiType psiType, @Nullable String primaryCandidateName, boolean filterByGenerics) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
    if (psiClass != null && psiClass.getQualifiedName() == null) {
      return Collections.emptySet();
    }
    SpringModelSearchParameters.BeanClass searchParameters = SpringModelSearchParameters.byType(psiType).withInheritors().effectiveBeanTypes();
    List<SpringBeanPointer<?>> beans = SpringModelSearchers.findBeans(model, searchParameters);
    if (beans.size() == 0 && (psiType instanceof PsiClassType)) {
      beans = findWildcardInjectedBeans(model, (PsiClassType) psiType);
      filterByGenerics = false;
    }
    Set<SpringBeanPointer<?>> pointers = CommonUtils.filterInnerClassBeans(
            filterByPriority(excludeExplicitlyRedefined(excludeTheSameIdentifyingElements(excludeOverridenDefaultBeans(excludeAutowireCandidates(beans, primaryCandidateName, model)))),
                    model.getModule()));
    return filterByGenerics ? filterByGenerics(pointers, psiType) : pointers;
  }

  private static List<SpringBeanPointer<?>> findWildcardInjectedBeans(CommonSpringModel model, PsiClassType psiType) {
    PsiClass psiClass = psiType.resolve();
    if (psiClass == null) {
      return emptyList();
    }
    List<SpringBeanPointer<?>> beansByClass = SpringModelSearchers.findBeans(model, SpringModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes());
    return ContainerUtil.filter(beansByClass, pointer -> isAssignableWildcardTypeBean(psiType, pointer));
  }

  public static boolean isAssignableWildcardTypeBean(PsiClassType searchType, SpringBeanPointer<?> bean) {
    for (PsiType effectiveBeanType : bean.getEffectiveBeanTypes()) {
      if (effectiveBeanType instanceof PsiClassType type) {
        for (PsiType parameter : type.getParameters()) {
          if (parameter instanceof PsiWildcardType effectiveType) {
            if (effectiveType.isAssignableFrom(searchType)) {
              return true;
            }
          }
        }
      }
    }

    return false;
//    return Arrays.stream(bean.getEffectiveBeanTypes())
//            .filter(PsiClassType.class::isInstance)
//            .filter(type -> ContainerUtil.exists(((PsiClassType) type).getParameters(), PsiWildcardType.class::isInstance))
//            .anyMatch(effectiveType -> effectiveType.isAssignableFrom(searchType));
  }

  private static Set<SpringBeanPointer<?>> filterByGenerics(Set<? extends SpringBeanPointer<?>> pointers, PsiType type) {
    Set<SpringBeanPointer<?>> filtered = new HashSet<>();
    for (SpringBeanPointer pointer : pointers) {
      PsiType[] effectiveBeanTypes = pointer.getEffectiveBeanTypes();
      int length = effectiveBeanTypes.length;
      int i = 0;
      while (true) {
        if (i < length) {
          PsiType psiType = effectiveBeanTypes[i];
          if (!TypeConversionUtil.isAssignable(type, psiType, pointers.size() == 1)) {
            i++;
          }
          else {
            filtered.add(pointer);
            break;
          }
        }
      }
    }
    return filtered;
  }

  private static boolean isAssignable(PsiType type, @Nullable PsiClass beanClass) {
    if (beanClass == null) {
      return false;
    }
    PsiClassType psiClassType = JavaPsiFacade.getInstance(beanClass.getProject()).getElementFactory().createType(beanClass);
    return type.isAssignableFrom(psiClassType);
  }

  public static PsiType getAutowiredEffectiveBeanTypes(PsiType psiType) {
    PsiType objectFactoryType;
    if (psiType instanceof PsiArrayType) {
      return getAutowiredEffectiveBeanTypes(((PsiArrayType) psiType).getComponentType());
    }
    PsiType beanType = PsiUtil.extractIterableTypeParameter(psiType, false);
    if (beanType != null) {
      return beanType;
    }
    if (psiType instanceof PsiClassType) {
      if (SpringObjectFactoryEffectiveTypeProvider.isJavaxInjectProvider((PsiClassType) psiType)) {
        PsiType injectProviderType = SpringObjectFactoryEffectiveTypeProvider.getJavaxInjectProviderType((PsiClassType) psiType);
        if (injectProviderType != null) {
          return injectProviderType;
        }
      }
      else if (SpringObjectFactoryEffectiveTypeProvider.isJakartaInjectProvider((PsiClassType) psiType)) {
        PsiType injectProviderType2 = SpringObjectFactoryEffectiveTypeProvider.getJakartaInjectProviderType((PsiClassType) psiType);
        if (injectProviderType2 != null) {
          return injectProviderType2;
        }
      }
      else if (SpringObjectFactoryEffectiveTypeProvider.isObjectFactory((PsiClassType) psiType) && (objectFactoryType = SpringObjectFactoryEffectiveTypeProvider.getObjectFactoryType(
              (PsiClassType) psiType)) != null) {
        return objectFactoryType;
      }
    }
    return psiType;
  }

  private static List<SpringBeanPointer<?>> emptyList() {
    return new ArrayList();
  }

  public static boolean isAutowiredByDefault(PsiType psiType) {
    if (!(psiType instanceof PsiClassType)) {
      return false;
    }
    String text = psiType.getCanonicalText();
    if (STANDARD_AUTOWIRED_TYPES.contains(text)) {
      return true;
    }
    for (String standardAutowiredType : STANDARD_AUTOWIRED_TYPES) {
      if (InheritanceUtil.isInheritor(psiType, standardAutowiredType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isAutowired(SpringBean springBean, CommonSpringModel springModel, PsiMethod psiMethod) {
    PsiClass psiClass;
    Autowire autowire = springBean.getBeanAutowire();
    switch (autowire) {
      case BY_TYPE:
        PsiType type = psiMethod.getParameterList().getParameters()[0].getType();
        return type instanceof PsiClassType psiClassType
                && (psiClass = psiClassType.resolve()) != null
                && SpringModelSearchers.doesBeanExist(springModel, psiClass);
      case BY_NAME:
        String propertyName = PropertyUtilBase.getPropertyNameBySetter(psiMethod);
        SpringBeanPointer<?> bean = SpringModelSearchers.findBean(springModel, propertyName);
        return bean != null && !bean.isReferenceTo(springBean);
      default:
        return false;
    }
  }

  public static Set<String> getAutowiredAnnotations(@Nullable Module module) {
    if (module == null) {
      return AUTOWIRED_ANNOTATIONS;
    }
    Set<String> annotations = new LinkedHashSet<>();
    annotations.addAll(AUTOWIRED_ANNOTATIONS);
    annotations.addAll(getMetaAutowiredAnnotations(module));
    if (isUsingAutowiredPostProcessor(module)) {
      annotations.addAll(getCustomAnnotationsFromPostProcessors(module));
    }
    return annotations;
  }

  private static List<String> getMetaAutowiredAnnotations(Module module) {
    return ContainerUtil.mapNotNull(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, AnnotationConstant.AUTOWIRED, false), PsiClass::getQualifiedName);
  }

  public static Set<PsiModifierListOwner> getAutowiredMembers(PsiType type, @Nullable Module module, PsiMember method) {
    if (module == null) {
      return Collections.emptySet();
    }
    Set<PsiModifierListOwner> membersCandidate = new LinkedHashSet<>();
    GlobalSearchScope scope = method.getResolveScope();
    Set<PsiType> effectiveTypes = getEffectiveTypes(type);
    Processor<PsiMember> processor = member -> {
      if (member instanceof PsiField) {
        PsiType psiType = ((PsiField) member).getType();
        for (PsiType effectiveType : effectiveTypes) {
          if (isAutowiredCandidate(psiType, effectiveType)) {
            membersCandidate.add(member);
            return true;
          }
        }
        return true;
      }
      else if (member instanceof PsiMethod) {
        for (PsiParameter psiParameter : ((PsiMethod) member).getParameterList().getParameters()) {
          Iterator<PsiType> it2 = effectiveTypes.iterator();
          while (true) {
            if (it2.hasNext()) {
              PsiType effectiveType2 = it2.next();
              if (isAutowiredCandidate(psiParameter.getType(), effectiveType2)) {
                membersCandidate.add(psiParameter);
                break;
              }
            }
          }
        }
        return true;
      }
      else {
        return true;
      }
    };
    Set<String> annotations = new HashSet<>(getAutowiredAnnotations(module));
    annotations.add(AnnotationConstant.BEAN);
    for (String annotation : annotations) {
      PsiClass annoClass = JavaPsiFacade.getInstance(module.getProject()).findClass(annotation, scope);
      if (annoClass != null) {
        AnnotatedMembersSearch.search(annoClass, scope).forEach(processor);
      }
    }
    for (InfraStereotypeElement stereotypeElement : SpringJamModel.from(module).getStereotypeComponents()) {
      PsiClass psiClass = stereotypeElement.getPsiElement();
      PsiMethod[] constructors = psiClass.getConstructors();
      if (constructors.length == 1 && !membersCandidate.contains(constructors[0])) {
        processor.process(constructors[0]);
      }
    }
    return filterCandidates(method, membersCandidate, module);
  }

  private static boolean isAutowiredCandidate(PsiType psiType, PsiType candidateType) {
    if ("java.lang.Object".equals(psiType.getCanonicalText())) {
      return false;
    }
    if (psiType.isAssignableFrom(candidateType)) {
      return true;
    }
    PsiType effectivePsiType = getAutowiredEffectiveBeanTypes(psiType);
    return !effectivePsiType.equals(psiType) && effectivePsiType.isAssignableFrom(candidateType);
  }

  public static Set<PsiModifierListOwner> filterCandidates(PsiMember method, Set<? extends PsiModifierListOwner> all, @Nullable Module module) {
    SpringJamQualifier qualifier = getQualifier(method, getQualifiedAnnotation(method, module));
    return all.stream().filter(owner -> {
      SpringJamQualifier candidateQualifier = getQualifier(owner);
      if (candidateQualifier != null) {
        if (qualifier != null && qualifier.compareQualifiers(candidateQualifier, module)) {
          return true;
        }
        String name = method.getName();
        return name != null && name.equals(candidateQualifier.getQualifierValue());
      }
      return true;
    }).collect(Collectors.toSet());
  }

  @Nullable
  public static SpringJamQualifier getQualifier(@Nullable PsiModifierListOwner modifierListOwner) {
    if (modifierListOwner == null) {
      return null;
    }
    return getQualifier(modifierListOwner, getQualifiedAnnotation(modifierListOwner));
  }

  @Nullable
  public static SpringJamQualifier getQualifier(@Nullable PsiModifierListOwner modifierListOwner, @Nullable PsiAnnotation qualifiedAnnotation) {
    if (qualifiedAnnotation == null) {
      return null;
    }
    return new SpringJamQualifier(qualifiedAnnotation, modifierListOwner);
  }

  private static Set<PsiType> getEffectiveTypes(PsiType type) {
    Set<PsiType> types = new LinkedHashSet<>();
    types.add(type);
    types.addAll(SpringBeanCoreUtils.getFactoryBeanTypes(type, null));
    return types;
  }

  private static Set<String> getCustomAnnotationsFromPostProcessors(@Nullable Module module) {
    if (module == null) {
      return Collections.emptySet();
    }
    PsiClass autowiredPostProcessor = CommonUtils.findLibraryClass(module, InfraConstant.AUTOWIRED_ANNO_POST_PROCESSOR_CLASS);
    if (autowiredPostProcessor == null) {
      return Collections.emptySet();
    }
    Set<String> annotations = new HashSet<>();
    SpringModel springModel = SpringManager.getInstance(module.getProject()).getCombinedModel(module);
    SpringModelSearchParameters.BeanClass searchParameters = SpringModelSearchParameters.byClass(autowiredPostProcessor).withInheritors();
    for (SpringBeanPointer pointer : SpringModelSearchers.findBeans(springModel, searchParameters)) {
      CommonSpringBean springBean = pointer.getSpringBean();
      if (springBean instanceof SpringBean) {
        addAutowiredAnnotationType(annotations, springBean);
        addAutowiredAnnotationTypes(annotations, springBean);
      }
    }
    return annotations;
  }

  private static boolean isUsingAutowiredPostProcessor(Module module) {
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    Project project = module.getProject();
    Collection<VirtualFile> springXmlCandidates = DomService.getInstance().getDomFileCandidates(Beans.class, scope);
    if (springXmlCandidates.isEmpty()) {
      return false;
    }
    String className = ClassUtil.extractClassName(InfraConstant.AUTOWIRED_ANNO_POST_PROCESSOR_CLASS);
    CommonProcessors.FindFirstProcessor<PsiFile> processor = new CommonProcessors.FindFirstProcessor<>();
    CacheManager.getInstance(project).processFilesWithWord(processor, className, (short) 255, GlobalSearchScope.filesWithLibrariesScope(project, springXmlCandidates), true);
    return processor.isFound();
  }

  private static void addAutowiredAnnotationType(Set<? super String> annotations, CommonSpringBean springBean) {
    SpringPropertyDefinition autowiredTypeProperty = SpringPropertyUtils.findPropertyByName(springBean, "autowiredAnnotationType");
    if (autowiredTypeProperty != null) {
      String value = autowiredTypeProperty.getValueAsString();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        annotations.add(value);
      }
    }
  }

  private static void addAutowiredAnnotationTypes(Set<? super String> annotations, CommonSpringBean springBean) {
    SpringPropertyDefinition autowiredTypes = SpringPropertyUtils.findPropertyByName(springBean, "autowiredAnnotationTypes");
    if (autowiredTypes instanceof SpringProperty springProperty) {
      addNotNullValues(annotations, springProperty.getList().getValues());
      addNotNullValues(annotations, springProperty.getSet().getValues());
      addNotNullValues(annotations, springProperty.getArray().getValues());
    }
  }

  private static void addNotNullValues(Collection<? super String> annotations, Collection<? extends SpringValue> values) {
    for (SpringValue value : values) {
      String stringValue = value.getStringValue();
      if (!StringUtil.isEmptyOrSpaces(stringValue)) {
        annotations.add(stringValue);
      }
    }
  }

  @Nullable
  public static CommonSpringModel getProcessingSpringModel(@Nullable PsiClass psiClass) {
    if (psiClass == null || psiClass.getQualifiedName() == null) {
      return null;
    }
    if (psiClass instanceof PsiAnonymousClass) {
      return SpringModelUtils.getInstance().getModuleCombinedSpringModel(psiClass);
    }
    CommonSpringModel model = SpringModelUtils.getInstance().getPsiClassSpringModel(psiClass);
    if (model instanceof CombinedSpringModel) {
      model = filterClassRelatedModels((CombinedSpringModel) model, psiClass);
    }
    if (!isEmptyModel(model)) {
      return model;
    }
    return null;
  }

  private static CommonSpringModel filterClassRelatedModels(CombinedSpringModel model, PsiClass aClass) {
    SpringFileSet fileSet;
    Set<CommonSpringModel> models = new HashSet<>();
    for (CommonSpringModel commonSpringModel : model.getUnderlyingModels()) {
      if (SpringModelSearchers.doesBeanExist(commonSpringModel, SpringModelSearchParameters.byClass(aClass).withInheritors())) {
        if ((commonSpringModel instanceof SpringModel) && (fileSet = ((SpringModel) commonSpringModel).getFileSet()) != null && fileSet.isAutodetected()) {
          return model;
        }
        models.add(commonSpringModel);
      }
    }
    return new CombinedSpringModelImpl(models, model.getModule());
  }

  private static boolean isEmptyModel(CommonSpringModel model) {
    return model.equals(SpringModel.UNKNOWN) || ((model instanceof CombinedSpringModel) && ((CombinedSpringModel) model).getUnderlyingModels().isEmpty());
  }

  private static Set<SpringBeanPointer<?>> excludeOverridenDefaultBeans(Set<SpringBeanPointer<?>> pointers) {
    if (pointers.size() == 1) {
      return pointers;
    }
    Set<SpringBeanPointer<?>> beans = new LinkedHashSet<>();
    SpringBeanPointer[] objects = pointers.toArray(new SpringBeanPointer[0]);
    for (SpringBeanPointer pointer : objects) {
      CommonSpringBean bean = pointer.getSpringBean();
      if (!isOverridden(bean, objects)) {
        beans.add(pointer);
      }
    }
    return beans;
  }

  private static Set<SpringBeanPointer<?>> excludeTheSameIdentifyingElements(Set<? extends SpringBeanPointer<?>> pointers) {
    Set<SpringBeanPointer<?>> filtered = new LinkedHashSet<>();
    Set<PsiElement> identifyingElements = new HashSet<>();
    for (SpringBeanPointer pointer : pointers) {
      PsiElement element = pointer.getSpringBean().getIdentifyingPsiElement();
      if (!identifyingElements.contains(element)) {
        filtered.add(pointer);
        ContainerUtil.addIfNotNull(identifyingElements, element);
      }
    }
    return filtered;
  }

  private static Set<SpringBeanPointer<?>> excludeExplicitlyRedefined(Set<SpringBeanPointer<?>> pointers) {
    if (pointers.size() <= 1) {
      return pointers;
    }
    ExplicitRedefinitionAwareBeansCollector collector = new ExplicitRedefinitionAwareBeansCollector();
    for (SpringBeanPointer pointer : pointers) {
      collector.process(pointer);
    }
    return (Set<SpringBeanPointer<?>>) collector.getResult();
  }

  private static Set<SpringBeanPointer<?>> filterByPriority(Set<SpringBeanPointer<?>> filtered, @Nullable Module module) {
    if (filtered.isEmpty()) {
      return filtered;
    }
    Long maxPriority = null;
    MultiMap<Long, SpringBeanPointer<?>> byPriority = new MultiMap<>();
    for (SpringBeanPointer<?> pointer : filtered) {
      if (pointer instanceof JamSpringBeanPointer) {
        JamPsiMemberSpringBean<?> psiMemberSpringBean = ((JamSpringBeanPointer) pointer).getSpringBean();
        PsiMember psiMember = psiMemberSpringBean.getPsiElement();
        PsiAnnotation priority = AnnotationUtil.findAnnotation(psiMember, getPriorityAnnotations(module));
        if (priority == null) {
          return filtered;
        }
        Long value = getPriorityAnnotationValue(priority, module);
        if (value == null) {
          return filtered;
        }
        if (maxPriority == null || value.intValue() < maxPriority.intValue()) {
          maxPriority = value;
        }
        byPriority.putValue(value, pointer);
      }
    }
    return maxPriority == null ? filtered : new HashSet(byPriority.get(maxPriority));
  }

  private static String[] getPriorityAnnotations(@Nullable Module module) {
    return module == null ? JavaeeConstant.PRIORITY_ANNOTATIONS : ArrayUtil.toStringArray(getMetaPriorityAnnotations(module));
  }

  private static List<String> getMetaPriorityAnnotations(Module module) {
    SmartList<String> smartList = new SmartList();
    for (String priorityAnnotation : JavaeeConstant.PRIORITY_ANNOTATIONS) {
      smartList.addAll(
              JamAnnotationTypeUtil.getAnnotationTypesWithChildrenIncludingTests(module, priorityAnnotation)
                      .stream()
                      .map(PsiClass::getQualifiedName)
                      .filter(Objects::nonNull)
                      .collect(Collectors.toSet())
      );
    }
    return smartList;
  }

  @Nullable
  private static Long getPriorityAnnotationValue(PsiAnnotation priority, @Nullable Module module) {
    PsiClass priorityAnnoClass;
    String qualifiedName = priority.getQualifiedName();
    if (qualifiedName == null) {
      return null;
    }
    if (ArrayUtil.contains(qualifiedName, JavaeeConstant.PRIORITY_ANNOTATIONS)) {
      return AnnotationUtil.getLongAttributeValue(priority, "value");
    }
    if (module == null || (priorityAnnoClass = JavaPsiFacade.getInstance(priority.getProject())
            .findClass(qualifiedName, GlobalSearchScope.allScope(priority.getProject()))) == null) {
      return null;
    }
    for (String priorityAnnotation : JavaeeConstant.PRIORITY_ANNOTATIONS) {
      PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(priorityAnnoClass, priorityAnnotation,
              JamAnnotationTypeUtil.getAnnotationTypesWithChildrenIncludingTests(module, priorityAnnotation));
      if (definingMetaAnnotation != null) {
        return AnnotationUtil.getLongAttributeValue(definingMetaAnnotation, "value");
      }
    }
    return null;
  }

  private static boolean isOverridden(CommonSpringBean bean, SpringBeanPointer[] objects) {
    if (bean instanceof SpringImplicitBeanMarker) {
      String beanName = bean.getBeanName();
      PsiType beanType = bean.getBeanType();
      if (beanType != null && beanName != null) {
        for (SpringBeanPointer pointer : objects) {
          if (!bean.equals(pointer.getSpringBean()) && beanName.equals(pointer.getName())) {
            for (PsiType psiType : pointer.getEffectiveBeanTypes()) {
              if (psiType.isAssignableFrom(beanType)) {
                return true;
              }
            }
          }
        }
        return false;
      }
      return false;
    }
    return false;
  }

  public static boolean isJavaUtilOptional(PsiType type) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
    return psiClass != null && "java.util.Optional".equals(psiClass.getQualifiedName());
  }

  @Nullable
  public static PsiType getOptionalType(PsiType psiClassType) {
    return PsiUtil.substituteTypeParameter(psiClassType, "java.util.Optional", 0, false);
  }

  public static Set<SpringBeanPointer<?>> getAutowiredBeansFor(PsiModifierListOwner injectionPointOwner, PsiType psiType, CommonSpringModel springModel) {
    PsiAnnotation resourceAnnotation = getResourceAnnotation(injectionPointOwner);
    if (resourceAnnotation != null && (injectionPointOwner instanceof PsiMember)) {
      SpringBeanPointer<?> bean = getResourceAutowiredBean(injectionPointOwner, springModel, resourceAnnotation);
      return bean != null ? Collections.singleton(bean) : Collections.emptySet();
    }
    PsiAnnotation qualifiedAnnotation = getEffectiveQualifiedAnnotation(injectionPointOwner);
    if (qualifiedAnnotation != null) {
      return getQualifiedAutowiredBeans(psiType, qualifiedAnnotation, springModel);
    }
    return getByTypeAutowiredBeans((PsiNameIdentifierOwner) injectionPointOwner, psiType, springModel);
  }

  private static Set<SpringBeanPointer<?>> getByTypeAutowiredBeans(PsiNameIdentifierOwner psiNameIdentifierOwner, PsiType searchType, CommonSpringModel model) {
    String primaryCandidateName = psiNameIdentifierOwner.getName();
    Set<SpringBeanPointer<?>> iterableBeanPointers = getIterableBeanPointers(searchType, model);
    return iterableBeanPointers.isEmpty() ? autowireByType(model, searchType, primaryCandidateName) : iterableBeanPointers;
  }

  private static Set<SpringBeanPointer<?>> getQualifiedAutowiredBeans(PsiType type, PsiAnnotation annotation, CommonSpringModel model) {
    return filterPointersByAutowiredType(type, getQualifiedBeanPointers(annotation, model));
  }

  @Nullable
  private static SpringBeanPointer<?> getResourceAutowiredBean(PsiModifierListOwner injectionPointOwner, CommonSpringModel springModel, PsiAnnotation resourceAnnotation) {
    PsiAnnotationMemberValue attributeValue = resourceAnnotation.findDeclaredAttributeValue("name");
    if (attributeValue != null) {
      return getByNameAutowiredBean(attributeValue, springModel);
    }
    return findBeanByImplicitInjectionPointName(injectionPointOwner, springModel);
  }

  @Nullable
  private static SpringBeanPointer<?> findBeanByImplicitInjectionPointName(PsiModifierListOwner injectionPointOwner, CommonSpringModel springModel) {
    SpringBeanPointer<?> bean;
    String name = null;
    if (injectionPointOwner instanceof PsiMethod) {
      name = PropertyUtilBase.getPropertyNameBySetter((PsiMethod) injectionPointOwner);
    }
    else if (injectionPointOwner instanceof PsiField) {
      name = ((PsiField) injectionPointOwner).getName();
    }
    if (name != null && (bean = SpringModelSearchers.findBean(springModel, name)) != null) {
      return bean.getBasePointer();
    }
    return null;
  }

  @Nullable
  public static PsiAnnotation getEffectiveQualifiedAnnotation(PsiModifierListOwner modifierListOwner) {
    if (modifierListOwner instanceof PsiMethod) {
      return null;
    }
    return getQualifiedAnnotation(modifierListOwner);
  }

  @Nullable
  private static SpringBeanPointer<?> getByNameAutowiredBean(PsiAnnotationMemberValue annotationMemberValue, CommonSpringModel model) {
    for (PsiReference reference : annotationMemberValue.getReferences()) {
      if (reference instanceof SpringBeanReference sbReference) {
        String beanName = sbReference.getValue();
        if (StringUtil.isNotEmpty(beanName)) {
          return SpringModelSearchers.findBean(model, beanName);
        }
      }
      else if (reference instanceof PsiReferenceExpression psiReferenceExpression) {
        PsiElement resolve = psiReferenceExpression.resolve();
        if (resolve instanceof PsiField psiField) {
          PsiConstantEvaluationHelper helper = JavaPsiFacade.getInstance(resolve.getProject()).getConstantEvaluationHelper();
          Object o = helper.computeConstantExpression(psiField.getInitializer());
          if (o instanceof String) {
            return SpringModelSearchers.findBean(model, (String) o);
          }
        }
      }
    }
    return null;
  }

  public static Set<SpringBeanPointer<?>> filterPointersByAutowiredType(PsiType searchType, Set<? extends SpringBeanPointer<?>> beanPointers) {
    Set<SpringBeanPointer<?>> autowiredPointers = new HashSet<>();
    for (SpringBeanPointer bean : beanPointers) {
      PsiType[] psiTypes = bean.getEffectiveBeanTypes();
      int length = psiTypes.length;
      int i = 0;
      while (true) {
        if (i < length) {
          PsiType psiType = psiTypes[i];
          if (!canBeAutowiredByType(searchType, psiType)) {
            i++;
          }
          else {
            autowiredPointers.add(bean);
            break;
          }
        }
      }
    }
    return autowiredPointers;
  }

  public static Set<SpringBeanPointer<?>> getQualifiedBeanPointers(PsiAnnotation qualifiedAnnotation, CommonSpringModel model) {
    SpringBeanPointer<?> pointer;
    Collection<SpringBeanPointer<?>> candidates = getQualifiedBeans(qualifiedAnnotation, model);
    String name = getQualifiedBeanName(qualifiedAnnotation);
    if (name != null && (pointer = SpringModelSearchers.findBean(model, name)) != null) {
      candidates = new ArrayList<>(candidates);
      candidates.add(pointer.getBasePointer());
    }
    return excludeAutowireCandidates(candidates, model);
  }

  public static boolean canBeAutowiredByType(PsiType psiType, PsiType searchType) {
    if (psiType.isAssignableFrom(searchType)) {
      return true;
    }
    PsiType iterableType = getIterableType(psiType);
    if ((iterableType != null && iterableType.isAssignableFrom(searchType)) || isObjectFactoryEffectiveType(psiType, searchType) || isJavaUtilOptionalEffectiveType(psiType, searchType)) {
      return true;
    }
    return isJavaUtilProperties(searchType) && isStringMap(psiType);
  }

  private static boolean isJavaUtilOptionalEffectiveType(PsiType psiType, PsiType type) {
    PsiType optionalType;
    return isJavaUtilOptional(psiType) && (optionalType = getOptionalType(psiType)) != null && optionalType.isAssignableFrom(type);
  }

  private static boolean isStringMap(PsiType psiType) {
    return isJavaUtilMap(psiType)
            && isJavaLangString(PsiUtil.substituteTypeParameter(psiType, "java.util.Map", 0, false))
            && isJavaLangString(PsiUtil.substituteTypeParameter(psiType, "java.util.Map", 1, false));
  }

  private static boolean isJavaLangString(@Nullable PsiType type) {
    return (type instanceof PsiClassType) && type.getCanonicalText().equals("java.lang.String");
  }

  private static boolean isJavaUtilProperties(PsiType psiType) {
    return (psiType instanceof PsiClassType) && InheritanceUtil.isInheritor(psiType, "java.util.Properties");
  }

  private static boolean isObjectFactoryEffectiveType(PsiType psiType, PsiType aType) {
    PsiType objectFactoryEffectiveType = SpringObjectFactoryEffectiveTypeProvider.getObjectFactoryEffectiveType(aType);
    return objectFactoryEffectiveType != null && psiType.isAssignableFrom(objectFactoryEffectiveType);
  }

  public static Set<SpringBeanPointer<?>> getIterableBeanPointers(PsiType searchType, CommonSpringModel model) {
    Set<SpringBeanPointer<?>> emptySet;
    PsiType secondarySearchType = getIterableSearchType(searchType);
    if (secondarySearchType != null) {
      emptySet = autowireByType(model, secondarySearchType, null);
    }
    else {
      emptySet = Collections.emptySet();
    }
    return emptySet;
  }

  @Nullable
  public static PsiType getIterableSearchType(PsiType searchType) {
    PsiType iterableType = getIterableType(searchType);
    if (iterableType != null) {
      return iterableType;
    }
    if (isTypedMapWithStringKey(searchType)) {
      return PsiUtil.substituteTypeParameter(searchType, "java.util.Map", 1, false);
    }
    return SpringObjectFactoryEffectiveTypeProvider.getObjectFactoryEffectiveType(searchType);
  }

  @Nullable
  private static PsiType getIterableType(PsiType psiType) {
    if (psiType instanceof PsiArrayType) {
      return ((PsiArrayType) psiType).getComponentType();
    }
    return PsiUtil.extractIterableTypeParameter(psiType, true);
  }

  @Nullable
  public static String getQualifiedBeanName(PsiAnnotation qualifiedAnnotation) {
    PsiAnnotationMemberValue attributeValue = qualifiedAnnotation.findDeclaredAttributeValue("value");
    if (attributeValue == null) {
      return null;
    }
    return JamCommonUtil.getObjectValue(attributeValue, String.class);
  }

  public static boolean isTypedMapWithStringKey(PsiType psiType) {
    if (isJavaUtilMap(psiType)) {
      return isJavaLangString(PsiUtil.substituteTypeParameter(psiType, "java.util.Map", 0, false));
    }
    return false;
  }

  public static boolean isJavaUtilMap(PsiType psiType) {
    return (psiType instanceof PsiClassType) && InheritanceUtil.isInheritor(psiType, "java.util.Map");
  }

  public static boolean isAutowiringRelevantClass(@Nullable PsiClass psiClass) {
    return psiClass != null && InfraLibraryUtil.hasLibrary(ModuleUtilCore.findModuleForPsiElement(psiClass))
            && (CommonUtils.isBeanCandidateClass(psiClass) || SpringTestContextUtil.getInstance().isTestContextConfigurationClass(psiClass));
  }

  public static boolean isValueAnnoInjection(@Nullable PsiModifierListOwner modifierListOwner) {
    return modifierListOwner != null && AnnotationUtil.isAnnotated(modifierListOwner, AnnotationConstant.VALUE, 1);
  }

  public static boolean hasConditional(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(SpringConditional.SPRING_CONDITIONAL_JAM_ELEMENT_KEY, method) != null;
  }
}

