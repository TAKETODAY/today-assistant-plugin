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
import com.intellij.psi.PsiModifier;
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
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.context.model.CombinedInfraModel;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraConditional;
import cn.taketoday.assistant.model.InfraImplicitBeanMarker;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.ObjectFactoryEffectiveTypeProvider;
import cn.taketoday.assistant.model.highlighting.xml.InfraConstructorArgResolveUtil;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.contexts.CustomContextJavaBean;
import cn.taketoday.assistant.model.jam.qualifiers.InfraJamQualifier;
import cn.taketoday.assistant.model.jam.testContexts.InfraTestContextUtil;
import cn.taketoday.assistant.model.utils.BeanCoreUtils;
import cn.taketoday.assistant.model.utils.ExplicitRedefinitionAwareBeansCollector;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraConstructorArgUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArg;
import cn.taketoday.assistant.model.xml.beans.DefaultableBoolean;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraProperty;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.InfraValue;
import cn.taketoday.assistant.references.InfraBeanReference;
import cn.taketoday.assistant.util.InfraUtils;
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

  public static Map<PsiMethod, Collection<BeanPointer<?>>> getByTypeAutowiredProperties(InfraBean infraBean, CommonInfraModel model) {
    Map<PsiMethod, Collection<BeanPointer<?>>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (beanClass != null && model != null && isByTypeAutowired(infraBean)) {
      for (PsiMethod psiMethod : beanClass.getAllMethods()) {
        if (isPropertyAutowired(psiMethod, infraBean)) {
          PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
          Collection<BeanPointer<?>> list = new HashSet<>();
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

  public static Set<BeanPointer<?>> excludeAutowireCandidates(Collection<BeanPointer<?>> beans, @Nullable CommonInfraModel model) {
    return excludeAutowireCandidates(beans, null, model);
  }

  public static Set<BeanPointer<?>> excludeAutowireCandidates(Collection<BeanPointer<?>> beans, @Nullable String primaryCandidateName, @Nullable CommonInfraModel model) {
    Set<BeanPointer<?>> pointers = new LinkedHashSet<>();
    Collection<BeanPointer<?>> primaryBeans = beans.size() > 1 ? getPrimaryBeans(beans, primaryCandidateName, model) : beans;
    if (!primaryBeans.isEmpty()) {
      for (BeanPointer primaryBean : primaryBeans) {
        if (isAutowireCandidate(primaryBean)) {
          pointers.add(primaryBean);
        }
      }
      return pointers;
    }
    for (BeanPointer beanPointer : beans) {
      if (isAutowireCandidate(beanPointer)) {
        pointers.add(beanPointer);
      }
    }
    return pointers;
  }

  private static List<BeanPointer<?>> getPrimaryBeans(Collection<? extends BeanPointer<?>> beans, @Nullable String primaryCandidateName, @Nullable CommonInfraModel model) {
    if (primaryCandidateName == null) {
      return emptyList();
    }
    List<BeanPointer<?>> byPrimary = new SmartList<>();
    List<BeanPointer<?>> byName = new SmartList<>();
    for (BeanPointer beanPointer : beans) {
      if (beanPointer.isValid()) {
        CommonInfraBean infraBean = beanPointer.getBean();
        if (infraBean.isPrimary()) {
          if (isMyName(primaryCandidateName, beanPointer, model)) {
            return Collections.singletonList(beanPointer);
          }
          byPrimary.add(beanPointer);
        }
        else if (isMyName(primaryCandidateName, beanPointer, model)) {
          byName.add(beanPointer);
        }
      }
    }
    return byPrimary.isEmpty() ? byName : byPrimary;
  }

  private static boolean isMyName(@Nullable String name, BeanPointer<?> beanPointer, @Nullable CommonInfraModel model) {
    if (name == null) {
      return false;
    }
    String beanName = beanPointer.getName();
    if (name.equals(beanName)) {
      return true;
    }
    if (beanName != null && model != null) {
      for (String aliasName : InfraModelVisitorUtils.getAllBeanNames(model, beanPointer)) {
        if (name.equals(aliasName)) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  private static boolean isAutowireCandidate(@Nullable BeanPointer<?> pointer) {
    if (pointer == null || !pointer.isValid()) {
      return false;
    }
    var infraBean = pointer.getBean();
    if (!(infraBean instanceof InfraBean)) {
      return true;
    }
    if (((InfraBean) infraBean).isAbstract()) {
      return false;
    }
    DefaultableBoolean autoWireCandidate = ((InfraBean) infraBean).getAutowireCandidate().getValue();
    return (autoWireCandidate == null || autoWireCandidate.getBooleanValue()) && isDefaultAutowireCandidate(infraBean);
  }

  private static boolean isDefaultAutowireCandidate(CommonInfraBean infraBean) {
    Beans beans;
    if ((infraBean instanceof InfraBean) && (beans = ((InfraBean) infraBean).getParentOfType(Beans.class, false)) != null) {
      String autowireCandidates = beans.getDefaultAutowireCandidates().getValue();
      if (StringUtil.isNotEmpty(autowireCandidates)) {
        String beanName = infraBean.getBeanName();
        String[] aliases = infraBean.getAliases();
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

  public static Map<PsiType, Collection<BeanPointer<?>>> getConstructorAutowiredProperties(InfraBean infraBean, CommonInfraModel model) {
    PsiMethod infraBeanConstructor;
    Collection<BeanPointer<?>> autowireByType;
    Map<PsiType, Collection<BeanPointer<?>>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (beanClass != null && isConstructorAutowire(infraBean)) {
      boolean instantiatedByFactory = InfraConstructorArgResolveUtil.isInstantiatedByFactory(infraBean);
      if (instantiatedByFactory) {
        infraBeanConstructor = infraBean.getFactoryMethod().getValue();
      }
      else {
        infraBeanConstructor = InfraConstructorArgUtils.of().getInfraBeanConstructor(infraBean, model);
      }
      PsiMethod checkedMethod = infraBeanConstructor;
      if (checkedMethod != null) {
        List<ConstructorArg> list = infraBean.getConstructorArgs();
        Map<Integer, ConstructorArg> indexedArgs = InfraConstructorArgResolveUtil.getIndexedConstructorArgs(list);
        PsiParameter[] parameters = checkedMethod.getParameterList().getParameters();
        SmartList smartList = new SmartList();
        for (int i = 0; i < parameters.length; i++) {
          PsiParameter parameter = parameters[i];
          if (!InfraConstructorArgResolveUtil.acceptParameter(
                  parameter, new ArrayList<>(infraBean.getConstructorArgs()), indexedArgs, i, smartList)) {
            PsiType psiType = parameter.getType();
            PsiAnnotation qualifiedAnnotation = getQualifiedAnnotation(parameter);
            if (qualifiedAnnotation != null) {
              autowireByType = getQualifiedBeans(qualifiedAnnotation, model);
            }
            else {
              autowireByType = autowireByType(model, getAutowiredEffectiveBeanTypes(psiType));
            }
            Collection<BeanPointer<?>> infraBeans = autowireByType;
            if (!infraBeans.isEmpty()) {
              autowiredMap.put(psiType, infraBeans);
            }
          }
        }
      }
    }
    return autowiredMap;
  }

  public static boolean isPropertyNotDefined(InfraBean infraBean, String propertyName) {
    return infraBean.getProperty(propertyName) == null;
  }

  public static Map<PsiMethod, BeanPointer<?>> getByNameAutowiredProperties(InfraBean infraBean) {
    Map<PsiMethod, BeanPointer<?>> autowiredMap = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (beanClass != null) {
      CommonInfraModel model = InfraModelService.of().getModelByBean(infraBean);
      if (isByNameAutowired(infraBean)) {
        for (PsiMethod psiMethod : beanClass.getAllMethods()) {
          if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
            PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
            Collection<BeanPointer<?>> list = autowireByType(model, getAutowiredEffectiveBeanTypes(parameter.getType()));
            String propertyName = PropertyUtilBase.getPropertyNameBySetter(psiMethod);
            for (BeanPointer pointer : list) {
              if (pointer.isValid() && InfraBeanUtils.of().findBeanNames(pointer.getBean()).contains(propertyName) && isPropertyNotDefined(infraBean, propertyName)) {
                autowiredMap.put(psiMethod, pointer);
              }
            }
          }
        }
      }
    }
    return autowiredMap;
  }

  private static boolean isPropertyAutowired(PsiMethod psiMethod, InfraBean infraBean) {
    if (PropertyUtilBase.isSimplePropertySetter(psiMethod)) {
      PsiParameter parameter = psiMethod.getParameterList().getParameters()[0];
      PsiType type = parameter.getType();
      if (type instanceof PsiClassType classType) {
        PsiClass psiClass = classType.resolve();
        return psiClass != null
                && isPropertyNotDefined(infraBean, PropertyUtilBase.getPropertyNameBySetter(psiMethod));
      }
      return false;
    }
    return false;
  }

  public static boolean isByTypeAutowired(InfraBean infraBean) {
    return infraBean.getBeanAutowire() == Autowire.BY_TYPE;
  }

  public static boolean isByNameAutowired(InfraBean infraBean) {
    return infraBean.getBeanAutowire() == Autowire.BY_NAME;
  }

  public static boolean isConstructorAutowire(InfraBean bean) {
    return bean.getBeanAutowire() == Autowire.CONSTRUCTOR;
  }

  public static Map<PsiMember, Set<BeanPointer<?>>> getAutowireAnnotationProperties(CommonInfraBean infraBean, CommonInfraModel model) {
    Map<PsiMember, Set<BeanPointer<?>>> map = new HashMap<>();
    PsiClass beanClass = PsiTypesUtil.getPsiClass(infraBean.getBeanType());
    if (beanClass != null) {
      for (PsiMethod psiMethod : getAnnotatedAutowiredMethods(beanClass)) {
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
          PsiAnnotation psiAnnotation = getQualifiedAnnotation(parameter);
          if (psiAnnotation != null) {
            addAutowiredBeans(map, psiMethod, getQualifiedBeans(psiAnnotation, model), model);
          }
          else {
            addAutowiredBeans(map, psiMethod, BeanCoreUtils.getBeansByType(parameter.getType(), model), model);
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

  private static void addAutowiredBeans(Map<PsiMember, Set<BeanPointer<?>>> map, PsiMember psiMember, Collection<BeanPointer<?>> beans,
          CommonInfraModel model) {
    Set<BeanPointer<?>> list = excludeAutowireCandidates(beans, null, model);
    if (!list.isEmpty()) {
      if (!map.containsKey(psiMember)) {
        map.put(psiMember, list);
      }
      else {
        map.get(psiMember).addAll(list);
      }
    }
  }

  public static Set<BeanPointer<?>> getQualifiedBeans(PsiAnnotation psiAnnotation, @Nullable CommonInfraModel model) {
    if (model == null) {
      return Collections.emptySet();
    }
    InfraJamQualifier qualifier = getQualifier(null, psiAnnotation);
    return InfraModelVisitorUtils.findQualifiedBeans(model, qualifier);
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
      if (((modifierListOwner instanceof PsiField)
              && AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass, PsiAnnotation.TargetType.FIELD) != null)
              || (((modifierListOwner instanceof PsiParameter) && AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass,
              PsiAnnotation.TargetType.PARAMETER) != null) || ((modifierListOwner instanceof PsiMethod)
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
        if (AnnotationTargetUtil.findAnnotationTarget(annotationTypeClass2, PsiAnnotation.TargetType.METHOD) != null
                && (annotation = AnnotationUtil.findAnnotation(psiMethod, true, annotationTypeClass2.getQualifiedName())) != null) {
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
    return method.isConstructor() && (containingClass = method.getContainingClass()) != null && containingClass.getConstructors().length == 1 && InfraUtils.isStereotypeComponentOrMeta(
            containingClass);
  }

  public static boolean isCustomStereotypeBean(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(CustomContextJavaBean.JAM_KEY, method) != null;
  }

  public static boolean isRequired(PsiModifierListOwner owner) {
    PsiAnnotation autowiredAnnotation;
    Boolean required;
    PsiModifierList modifierList = owner.getModifierList();
    if (modifierList != null) {
      PsiAnnotation requiredAnno = modifierList.findAnnotation(AnnotationConstant.REQUIRED);
      return requiredAnno != null
              || (autowiredAnnotation = getAutowiredAnnotation(owner)) == null
              || (required = JamCommonUtil.getObjectValue(autowiredAnnotation.findAttributeValue("required"), Boolean.class)) == null
              || required;
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

  public static Set<BeanPointer<?>> autowireByType(CommonInfraModel model, PsiType psiType) {
    return autowireByType(model, psiType, null);
  }

  public static Set<BeanPointer<?>> autowireByType(CommonInfraModel model, PsiType psiType, @Nullable String primaryCandidateName) {
    return autowireByType(model, psiType, primaryCandidateName, true);
  }

  public static Set<BeanPointer<?>> autowireByType(CommonInfraModel model, PsiType psiType, @Nullable String primaryCandidateName, boolean filterByGenerics) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
    if (psiClass != null && psiClass.getQualifiedName() == null) {
      return Collections.emptySet();
    }
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byType(psiType).withInheritors().effectiveBeanTypes();
    List<BeanPointer<?>> beans = InfraModelSearchers.findBeans(model, searchParameters);
    if (beans.size() == 0 && (psiType instanceof PsiClassType)) {
      beans = findWildcardInjectedBeans(model, (PsiClassType) psiType);
      filterByGenerics = false;
    }
    Set<BeanPointer<?>> pointers = InfraUtils.filterInnerClassBeans(
            filterByPriority(excludeExplicitlyRedefined(excludeTheSameIdentifyingElements(excludeOverridenDefaultBeans(excludeAutowireCandidates(beans, primaryCandidateName, model)))),
                    model.getModule()));
    return filterByGenerics ? filterByGenerics(pointers, psiType) : pointers;
  }

  private static List<BeanPointer<?>> findWildcardInjectedBeans(CommonInfraModel model, PsiClassType psiType) {
    PsiClass psiClass = psiType.resolve();
    if (psiClass == null) {
      return emptyList();
    }
    List<BeanPointer<?>> beansByClass = InfraModelSearchers.findBeans(model, ModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes());
    return ContainerUtil.filter(beansByClass, pointer -> isAssignableWildcardTypeBean(psiType, pointer));
  }

  public static boolean isAssignableWildcardTypeBean(PsiClassType searchType, BeanPointer<?> bean) {
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

  private static Set<BeanPointer<?>> filterByGenerics(Set<? extends BeanPointer<?>> pointers, PsiType type) {
    Set<BeanPointer<?>> filtered = new HashSet<>();
    for (BeanPointer pointer : pointers) {
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
      if (ObjectFactoryEffectiveTypeProvider.isJavaxInjectProvider((PsiClassType) psiType)) {
        PsiType injectProviderType = ObjectFactoryEffectiveTypeProvider.getJavaxInjectProviderType((PsiClassType) psiType);
        if (injectProviderType != null) {
          return injectProviderType;
        }
      }
      else if (ObjectFactoryEffectiveTypeProvider.isJakartaInjectProvider((PsiClassType) psiType)) {
        PsiType injectProviderType2 = ObjectFactoryEffectiveTypeProvider.getJakartaInjectProviderType((PsiClassType) psiType);
        if (injectProviderType2 != null) {
          return injectProviderType2;
        }
      }
      else if (ObjectFactoryEffectiveTypeProvider.isObjectFactory((PsiClassType) psiType)
              && (objectFactoryType = ObjectFactoryEffectiveTypeProvider.getObjectFactoryType((PsiClassType) psiType)) != null) {
        return objectFactoryType;
      }
    }
    return psiType;
  }

  private static List<BeanPointer<?>> emptyList() {
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

  public static boolean isAutowired(InfraBean infraBean, CommonInfraModel infraModel, PsiMethod psiMethod) {
    PsiClass psiClass;
    Autowire autowire = infraBean.getBeanAutowire();
    switch (autowire) {
      case BY_TYPE:
        PsiType type = psiMethod.getParameterList().getParameters()[0].getType();
        return type instanceof PsiClassType psiClassType
                && (psiClass = psiClassType.resolve()) != null
                && InfraModelSearchers.doesBeanExist(infraModel, psiClass);
      case BY_NAME:
        String propertyName = PropertyUtilBase.getPropertyNameBySetter(psiMethod);
        BeanPointer<?> bean = InfraModelSearchers.findBean(infraModel, propertyName);
        return bean != null && !bean.isReferenceTo(infraBean);
      default:
        return false;
    }
  }

  public static Set<String> getAutowiredAnnotations(@Nullable Module module) {
    if (module == null) {
      return AUTOWIRED_ANNOTATIONS;
    }
    var annotations = new LinkedHashSet<String>();
    annotations.addAll(AUTOWIRED_ANNOTATIONS);
    annotations.addAll(getMetaAutowiredAnnotations(module));
    if (isUsingAutowiredPostProcessor(module)) {
      annotations.addAll(getCustomAnnotationsFromPostProcessors(module));
    }
    return annotations;
  }

  private static List<String> getMetaAutowiredAnnotations(Module module) {
    return ContainerUtil.mapNotNull(
            MetaAnnotationUtil.getAnnotationTypesWithChildren(module, AnnotationConstant.AUTOWIRED, false),
            PsiClass::getQualifiedName);
  }

  public static Set<PsiModifierListOwner> getAutowiredMembers(PsiType type, @Nullable Module module, PsiMember psiMember) {
    if (module == null) {
      return Collections.emptySet();
    }
    var membersCandidate = new LinkedHashSet<PsiModifierListOwner>();
    GlobalSearchScope scope = psiMember.getResolveScope();
    Set<PsiType> effectiveTypes = getEffectiveTypes(type);
    // collector
    Processor<PsiMember> processor = member -> {
      if (member instanceof PsiField field) {
        PsiType psiType = field.getType();
        for (PsiType effectiveType : effectiveTypes) {
          if (isAutowiredCandidate(psiType, effectiveType)) {
            membersCandidate.add(field);
            return true;
          }
        }
        return true;
      }
      else if (member instanceof PsiMethod method) {
        for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
          for (PsiType effectiveType2 : effectiveTypes) {
            if (isAutowiredCandidate(psiParameter.getType(), effectiveType2)) {
              membersCandidate.add(psiParameter);
              break;
            }
          }
        }
        return true;
      }
      else {
        return true;
      }
    };

    // 遍历所有的 包括 字段注解 Autowired Bean Component 也需要
    Set<String> annotations = new HashSet<>(getAutowiredAnnotations(module));
    annotations.add(AnnotationConstant.BEAN);
    annotations.add(AnnotationConstant.COMPONENT);
    for (String annotation : annotations) {
      PsiClass annoClass = JavaPsiFacade.getInstance(module.getProject()).findClass(annotation, scope);
      if (annoClass != null) {
        AnnotatedMembersSearch.search(annoClass, scope).forEach(processor);
      }
    }

    var components = InfraJamModel.from(module).getStereotypeComponents();
    for (InfraStereotypeElement stereotype : components) {
      PsiClass psiClass = stereotype.getPsiElement();
      PsiMethod[] constructors = psiClass.getConstructors();
      if (constructors.length == 1 && !membersCandidate.contains(constructors[0])) {
        processor.process(constructors[0]);
      }

      // find final field
      var allFields = psiClass.getFields();
      for (PsiField field : allFields) {
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList != null) {
          if (!modifierList.hasModifierProperty(PsiModifier.STATIC)
                  && modifierList.hasModifierProperty(PsiModifier.FINAL)) {
            processor.process(field);
          }
        }
      }
    }

    return filterCandidates(psiMember, membersCandidate, module);
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

  public static Set<PsiModifierListOwner> filterCandidates(PsiMember method, Set<PsiModifierListOwner> all, @Nullable Module module) {
    InfraJamQualifier qualifier = getQualifier(method, getQualifiedAnnotation(method, module));
    return all.stream().filter(owner -> {
      InfraJamQualifier candidateQualifier = getQualifier(owner);
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
  public static InfraJamQualifier getQualifier(@Nullable PsiModifierListOwner modifierListOwner) {
    if (modifierListOwner == null) {
      return null;
    }
    return getQualifier(modifierListOwner, getQualifiedAnnotation(modifierListOwner));
  }

  @Nullable
  public static InfraJamQualifier getQualifier(@Nullable PsiModifierListOwner modifierListOwner, @Nullable PsiAnnotation qualifiedAnnotation) {
    if (qualifiedAnnotation == null) {
      return null;
    }
    return new InfraJamQualifier(qualifiedAnnotation, modifierListOwner);
  }

  private static Set<PsiType> getEffectiveTypes(PsiType type) {
    LinkedHashSet<PsiType> types = new LinkedHashSet<>();
    types.add(type);
    types.addAll(BeanCoreUtils.getFactoryBeanTypes(type, null));
    return types;
  }

  private static Set<String> getCustomAnnotationsFromPostProcessors(@Nullable Module module) {
    if (module == null) {
      return Collections.emptySet();
    }
    PsiClass autowiredPostProcessor = InfraUtils.findLibraryClass(module, InfraConstant.AUTOWIRED_ANNO_POST_PROCESSOR_CLASS);
    if (autowiredPostProcessor == null) {
      return Collections.emptySet();
    }
    Set<String> annotations = new HashSet<>();
    InfraModel infraModel = InfraManager.from(module.getProject()).getCombinedModel(module);
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(autowiredPostProcessor).withInheritors();
    for (BeanPointer pointer : InfraModelSearchers.findBeans(infraModel, searchParameters)) {
      CommonInfraBean infraBean = pointer.getBean();
      if (infraBean instanceof InfraBean) {
        addAutowiredAnnotationType(annotations, infraBean);
        addAutowiredAnnotationTypes(annotations, infraBean);
      }
    }
    return annotations;
  }

  private static boolean isUsingAutowiredPostProcessor(Module module) {
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    Project project = module.getProject();
    var xmlCandidates = DomService.getInstance().getDomFileCandidates(Beans.class, scope);
    if (xmlCandidates.isEmpty()) {
      return false;
    }
    String className = ClassUtil.extractClassName(InfraConstant.AUTOWIRED_ANNO_POST_PROCESSOR_CLASS);
    var processor = new CommonProcessors.FindFirstProcessor<PsiFile>();
    CacheManager.getInstance(project)
            .processFilesWithWord(processor, className, UsageSearchContext.ANY,
                    GlobalSearchScope.filesWithLibrariesScope(project, xmlCandidates), true);
    return processor.isFound();
  }

  private static void addAutowiredAnnotationType(Set<? super String> annotations, CommonInfraBean infraBean) {
    InfraPropertyDefinition autowiredTypeProperty = InfraPropertyUtils.findPropertyByName(infraBean, "autowiredAnnotationType");
    if (autowiredTypeProperty != null) {
      String value = autowiredTypeProperty.getValueAsString();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        annotations.add(value);
      }
    }
  }

  private static void addAutowiredAnnotationTypes(Set<? super String> annotations, CommonInfraBean infraBean) {
    InfraPropertyDefinition autowiredTypes = InfraPropertyUtils.findPropertyByName(infraBean, "autowiredAnnotationTypes");
    if (autowiredTypes instanceof InfraProperty infraProperty) {
      addNotNullValues(annotations, infraProperty.getList().getValues());
      addNotNullValues(annotations, infraProperty.getSet().getValues());
      addNotNullValues(annotations, infraProperty.getArray().getValues());
    }
  }

  private static void addNotNullValues(Collection<? super String> annotations, Collection<? extends InfraValue> values) {
    for (InfraValue value : values) {
      String stringValue = value.getStringValue();
      if (!StringUtil.isEmptyOrSpaces(stringValue)) {
        annotations.add(stringValue);
      }
    }
  }

  @Nullable
  public static CommonInfraModel getProcessingInfraModel(@Nullable PsiClass psiClass) {
    if (psiClass == null || psiClass.getQualifiedName() == null) {
      return null;
    }
    if (psiClass instanceof PsiAnonymousClass) {
      return InfraModelService.of().getModuleCombinedModel(psiClass);
    }
    CommonInfraModel model = InfraModelService.of().getPsiClassModel(psiClass);
    if (model instanceof CombinedInfraModel) {
      model = filterClassRelatedModels((CombinedInfraModel) model, psiClass);
    }
    if (!isEmptyModel(model)) {
      return model;
    }
    return null;
  }

  private static CommonInfraModel filterClassRelatedModels(CombinedInfraModel model, PsiClass aClass) {
    InfraFileSet fileSet;
    Set<CommonInfraModel> models = new HashSet<>();
    for (CommonInfraModel infraModel : model.getUnderlyingModels()) {
      if (InfraModelSearchers.doesBeanExist(infraModel, ModelSearchParameters.byClass(aClass).withInheritors())) {
        if ((infraModel instanceof InfraModel) && (fileSet = ((InfraModel) infraModel).getFileSet()) != null && fileSet.isAutodetected()) {
          return model;
        }
        models.add(infraModel);
      }
    }
    return new CombinedInfraModelImpl(models, model.getModule());
  }

  private static boolean isEmptyModel(CommonInfraModel model) {
    return model.equals(InfraModel.UNKNOWN) || ((model instanceof CombinedInfraModel) && ((CombinedInfraModel) model).getUnderlyingModels().isEmpty());
  }

  private static Set<BeanPointer<?>> excludeOverridenDefaultBeans(Set<BeanPointer<?>> pointers) {
    if (pointers.size() == 1) {
      return pointers;
    }
    Set<BeanPointer<?>> beans = new LinkedHashSet<>();
    BeanPointer[] objects = pointers.toArray(new BeanPointer[0]);
    for (BeanPointer pointer : objects) {
      CommonInfraBean bean = pointer.getBean();
      if (!isOverridden(bean, objects)) {
        beans.add(pointer);
      }
    }
    return beans;
  }

  private static Set<BeanPointer<?>> excludeTheSameIdentifyingElements(Set<? extends BeanPointer<?>> pointers) {
    Set<BeanPointer<?>> filtered = new LinkedHashSet<>();
    Set<PsiElement> identifyingElements = new HashSet<>();
    for (BeanPointer pointer : pointers) {
      PsiElement element = pointer.getBean().getIdentifyingPsiElement();
      if (!identifyingElements.contains(element)) {
        filtered.add(pointer);
        ContainerUtil.addIfNotNull(identifyingElements, element);
      }
    }
    return filtered;
  }

  private static Set<BeanPointer<?>> excludeExplicitlyRedefined(Set<BeanPointer<?>> pointers) {
    if (pointers.size() <= 1) {
      return pointers;
    }
    ExplicitRedefinitionAwareBeansCollector collector = new ExplicitRedefinitionAwareBeansCollector();
    for (BeanPointer pointer : pointers) {
      collector.process(pointer);
    }
    return (Set<BeanPointer<?>>) collector.getResult();
  }

  private static Set<BeanPointer<?>> filterByPriority(Set<BeanPointer<?>> filtered, @Nullable Module module) {
    if (filtered.isEmpty()) {
      return filtered;
    }
    Long maxPriority = null;
    MultiMap<Long, BeanPointer<?>> byPriority = new MultiMap<>();
    for (BeanPointer<?> pointer : filtered) {
      if (pointer instanceof JamBeanPointer) {
        JamPsiMemberInfraBean<?> psiMemberBean = ((JamBeanPointer) pointer).getBean();
        PsiMember psiMember = psiMemberBean.getPsiElement();
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

  private static boolean isOverridden(CommonInfraBean bean, BeanPointer[] objects) {
    if (bean instanceof InfraImplicitBeanMarker) {
      String beanName = bean.getBeanName();
      PsiType beanType = bean.getBeanType();
      if (beanType != null && beanName != null) {
        for (BeanPointer pointer : objects) {
          if (!bean.equals(pointer.getBean()) && beanName.equals(pointer.getName())) {
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

  public static Set<BeanPointer<?>> getAutowiredBeansFor(PsiModifierListOwner injectionPointOwner, PsiType psiType, CommonInfraModel infraModel) {
    PsiAnnotation resourceAnnotation = getResourceAnnotation(injectionPointOwner);
    if (resourceAnnotation != null && (injectionPointOwner instanceof PsiMember)) {
      BeanPointer<?> bean = getResourceAutowiredBean(injectionPointOwner, infraModel, resourceAnnotation);
      return bean != null ? Collections.singleton(bean) : Collections.emptySet();
    }
    PsiAnnotation qualifiedAnnotation = getEffectiveQualifiedAnnotation(injectionPointOwner);
    if (qualifiedAnnotation != null) {
      return getQualifiedAutowiredBeans(psiType, qualifiedAnnotation, infraModel);
    }
    return getByTypeAutowiredBeans((PsiNameIdentifierOwner) injectionPointOwner, psiType, infraModel);
  }

  private static Set<BeanPointer<?>> getByTypeAutowiredBeans(PsiNameIdentifierOwner psiNameIdentifierOwner, PsiType searchType, CommonInfraModel model) {
    String primaryCandidateName = psiNameIdentifierOwner.getName();
    Set<BeanPointer<?>> iterableBeanPointers = getIterableBeanPointers(searchType, model);
    return iterableBeanPointers.isEmpty() ? autowireByType(model, searchType, primaryCandidateName) : iterableBeanPointers;
  }

  private static Set<BeanPointer<?>> getQualifiedAutowiredBeans(PsiType type, PsiAnnotation annotation, CommonInfraModel model) {
    return filterPointersByAutowiredType(type, getQualifiedBeanPointers(annotation, model));
  }

  @Nullable
  private static BeanPointer<?> getResourceAutowiredBean(PsiModifierListOwner injectionPointOwner, CommonInfraModel infraModel, PsiAnnotation resourceAnnotation) {
    PsiAnnotationMemberValue attributeValue = resourceAnnotation.findDeclaredAttributeValue("name");
    if (attributeValue != null) {
      return getByNameAutowiredBean(attributeValue, infraModel);
    }
    return findBeanByImplicitInjectionPointName(injectionPointOwner, infraModel);
  }

  @Nullable
  private static BeanPointer<?> findBeanByImplicitInjectionPointName(PsiModifierListOwner injectionPointOwner, CommonInfraModel infraModel) {
    BeanPointer<?> bean;
    String name = null;
    if (injectionPointOwner instanceof PsiMethod) {
      name = PropertyUtilBase.getPropertyNameBySetter((PsiMethod) injectionPointOwner);
    }
    else if (injectionPointOwner instanceof PsiField) {
      name = ((PsiField) injectionPointOwner).getName();
    }
    if (name != null && (bean = InfraModelSearchers.findBean(infraModel, name)) != null) {
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
  private static BeanPointer<?> getByNameAutowiredBean(PsiAnnotationMemberValue annotationMemberValue, CommonInfraModel model) {
    for (PsiReference reference : annotationMemberValue.getReferences()) {
      if (reference instanceof InfraBeanReference sbReference) {
        String beanName = sbReference.getValue();
        if (StringUtil.isNotEmpty(beanName)) {
          return InfraModelSearchers.findBean(model, beanName);
        }
      }
      else if (reference instanceof PsiReferenceExpression psiReferenceExpression) {
        PsiElement resolve = psiReferenceExpression.resolve();
        if (resolve instanceof PsiField psiField) {
          PsiConstantEvaluationHelper helper = JavaPsiFacade.getInstance(resolve.getProject()).getConstantEvaluationHelper();
          Object o = helper.computeConstantExpression(psiField.getInitializer());
          if (o instanceof String) {
            return InfraModelSearchers.findBean(model, (String) o);
          }
        }
      }
    }
    return null;
  }

  public static Set<BeanPointer<?>> filterPointersByAutowiredType(PsiType searchType, Set<? extends BeanPointer<?>> beanPointers) {
    Set<BeanPointer<?>> autowiredPointers = new HashSet<>();
    for (BeanPointer bean : beanPointers) {
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

  public static Set<BeanPointer<?>> getQualifiedBeanPointers(PsiAnnotation qualifiedAnnotation, CommonInfraModel model) {
    BeanPointer<?> pointer;
    Collection<BeanPointer<?>> candidates = getQualifiedBeans(qualifiedAnnotation, model);
    String name = getQualifiedBeanName(qualifiedAnnotation);
    if (name != null && (pointer = InfraModelSearchers.findBean(model, name)) != null) {
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
    return (type instanceof PsiClassType) && "java.lang.String".equals(type.getCanonicalText());
  }

  private static boolean isJavaUtilProperties(PsiType psiType) {
    return (psiType instanceof PsiClassType) && InheritanceUtil.isInheritor(psiType, "java.util.Properties");
  }

  private static boolean isObjectFactoryEffectiveType(PsiType psiType, PsiType aType) {
    PsiType objectFactoryEffectiveType = ObjectFactoryEffectiveTypeProvider.getObjectFactoryEffectiveType(aType);
    return objectFactoryEffectiveType != null && psiType.isAssignableFrom(objectFactoryEffectiveType);
  }

  public static Set<BeanPointer<?>> getIterableBeanPointers(PsiType searchType, CommonInfraModel model) {
    Set<BeanPointer<?>> emptySet;
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
    return ObjectFactoryEffectiveTypeProvider.getObjectFactoryEffectiveType(searchType);
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
            && (InfraUtils.isBeanCandidateClass(psiClass) || InfraTestContextUtil.of().isTestContextConfigurationClass(psiClass));
  }

  public static boolean isValueAnnoInjection(@Nullable PsiModifierListOwner modifierListOwner) {
    return modifierListOwner != null && AnnotationUtil.isAnnotated(modifierListOwner, AnnotationConstant.VALUE, 1);
  }

  public static boolean hasConditional(PsiMethod method) {
    return JamService.getJamService(method.getProject()).getJamElement(InfraConditional.CONDITIONAL_JAM_ELEMENT_KEY, method) != null;
  }
}

