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

package cn.taketoday.assistant.model.xml.beans.impl;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.AutowireUtil;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ResolvedConstructorArgs;
import cn.taketoday.assistant.model.converters.InfraBeanFactoryMethodConverterImpl;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgumentValues;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.lang.Nullable;

class ResolvedConstructorArgsImpl implements ResolvedConstructorArgs {
  private static final Comparator<PsiMethod> CTOR_COMPARATOR = (o1, o2) -> {
    boolean p1 = o1.hasModifierProperty("public");
    boolean p2 = o2.hasModifierProperty("public");
    if (p1 != p2) {
      return p1 ? -1 : 1;
    }
    int c1pl = o1.getParameterList().getParametersCount();
    int c2pl = o2.getParameterList().getParametersCount();
    return Integer.compare(c2pl, c1pl);
  };
  private static final List<String> AUTOWIRE_ANNOTATIONS = List.of(
          AnnotationConstant.AUTOWIRED,
          JavaeeConstant.JAVAX_INJECT,
          JavaeeConstant.JAKARTA_INJECT
  );
  @Nullable
  private PsiMethod myResolvedMethod;
  private final boolean myResolved;
  private final List<PsiMethod> myCandidates;
  private final Map<PsiMethod, Map<ConstructorArgDefinition, PsiParameter>> myResolvedArgs;
  private final Map<PsiMethod, Map<PsiParameter, Collection<BeanPointer<?>>>> myAutowiredParams;
  private final List<PsiMethod> myCheckedMethods;

  public ResolvedConstructorArgsImpl(InfraBean bean) {
    this.myCandidates = new SmartList();
    this.myResolvedArgs = new HashMap();
    this.myAutowiredParams = new HashMap();
    this.myCheckedMethods = new SmartList();
    this.myResolved = resolve(bean);
  }

  @Override
  public boolean isResolved() {
    return this.myResolved;
  }

  @Override
  @Nullable
  public PsiMethod getResolvedMethod() {
    return this.myResolvedMethod;
  }

  @Override

  public List<PsiMethod> getCheckedMethods() {
    List<PsiMethod> list = this.myCheckedMethods;
    return list;
  }

  @Override
  @Nullable
  public Map<ConstructorArgDefinition, PsiParameter> getResolvedArgs() {
    if (this.myResolvedMethod == null) {
      return null;
    }
    return getResolvedArgs(this.myResolvedMethod);
  }

  @Override
  public Map<ConstructorArgDefinition, PsiParameter> getResolvedArgs(PsiMethod method) {
    return this.myResolvedArgs.get(method);
  }

  @Override
  public Map<PsiParameter, Collection<BeanPointer<?>>> getAutowiredParams(PsiMethod method) {
    return this.myAutowiredParams.get(method);
  }

  @Override
  public List<PsiMethod> getCandidates() {
    return this.myCandidates;
  }

  private boolean resolve(InfraBean bean) {
    List<PsiMethod> methods;
    String factoryMethod = bean.getFactoryMethod().getStringValue();
    if (factoryMethod != null) {
      methods = InfraBeanFactoryMethodConverterImpl.getFactoryMethodCandidates(bean, factoryMethod);
      if (methods.isEmpty()) {
        return false;
      }
    }
    else {
      PsiClass beanClass = PsiTypesUtil.getPsiClass(bean.getBeanType());
      if (beanClass == null) {
        return false;
      }
      PsiMethod[] constructors = beanClass.getConstructors();
      if (constructors.length == 0) {
        return bean.getAllConstructorArgs().isEmpty();
      }
      methods = ContainerUtil.newArrayList(constructors);
    }
    int constructorArgsCount = bean.getAllConstructorArgs().size();
    boolean constructorAutowire = AutowireUtil.isConstructorAutowire(bean);
    ConstructorArgumentValues values = new ConstructorArgumentValues();
    int minNrOfArgs = values.init(bean);
    NotNullLazyValue<CommonInfraModel> springModel = NotNullLazyValue.lazy(() -> InfraModelService.of().getModelByBean(bean));
    methods.sort(CTOR_COMPARATOR);
    for (PsiMethod method : methods) {
      PsiParameter[] params = method.getParameterList().getParameters();
      if (this.myResolvedMethod != null && params.length < this.myResolvedMethod.getParameterList().getParametersCount()) {
        return true;
      }
      List<Pair<PsiParameter, Integer>> allParams = new ArrayList<>(params.length);
      List<Pair<PsiParameter, Integer>> multiResolveParams = new ArrayList<>(params.length);
      int paramsLength = params.length;
      for (int i = 0; i < paramsLength; i++) {
        Pair<PsiParameter, Integer> pair = Pair.create(params[i], i);
        PsiType psiType = params[i].getType();
        if (isStringOrStringArray(psiType) || (psiType instanceof PsiEllipsisType)) {
          multiResolveParams.add(pair);
        }
        else {
          allParams.add(pair);
        }
      }
      allParams.addAll(multiResolveParams);
      Map<ConstructorArgDefinition, PsiParameter> resolvedArgs = new HashMap<>(params.length);
      this.myResolvedArgs.put(method, resolvedArgs);
      boolean isAutowired = constructorAutowire || AnnotationUtil.isAnnotated(method, AUTOWIRE_ANNOTATIONS, 0) || (methods.size() == 1 && method.isConstructor() && InfraLibraryUtil.isAtLeastVersion(
              ModuleUtilCore.findModuleForPsiElement(method), InfraLibraryUtil.TodayVersion.V_4_0));
      Map<PsiParameter, Collection<BeanPointer<?>>> autowiredParams = new HashMap<>();
      this.myAutowiredParams.put(method, autowiredParams);
      int autowiredCount = 0;
      Set<ConstructorArgDefinition> usedArgs = new LinkedHashSet<>(constructorArgsCount);
      for (Pair<PsiParameter, Integer> pair2 : allParams) {
        PsiParameter param = pair2.getFirst();
        ConstructorArgDefinition arg = values.resolve(pair2.getSecond(), param, usedArgs);
        ConstructorArgDefinition arg2 = arg;
        if (arg == null) {
          if (!isAutowired) {
            arg2 = values.resolveGeneric(null, usedArgs);
          }
        }
        if (arg2 != null) {
          usedArgs.add(arg2);
        }
        if (AutowireUtil.isValueAnnoInjection(param)) {
          autowiredCount++;
          autowiredParams.put(param, Collections.emptyList());
        }
        else if (arg2 == null && isAutowired) {
          PsiType paramType = param.getType();
          Collection<BeanPointer<?>> beans = AutowireUtil.autowireByType(springModel.getValue(), paramType, null, false);
          if (beans.isEmpty()) {
            PsiType effectiveBeanType = AutowireUtil.getAutowiredEffectiveBeanTypes(paramType);
            if (!effectiveBeanType.equals(paramType)) {
              beans = AutowireUtil.autowireByType(springModel.getValue(), effectiveBeanType, null, false);
            }
          }
          if (!beans.isEmpty() || AutowireUtil.isAutowiredByDefault(paramType)) {
            autowiredCount++;
          }
          autowiredParams.put(param, beans);
        }
        if (arg2 != null) {
          resolvedArgs.put(arg2, param);
        }
      }
      if (resolvedArgs.size() + autowiredCount == params.length && minNrOfArgs <= params.length) {
        this.myResolvedMethod = method;
        this.myCandidates.add(method);
      }
      this.myCheckedMethods.add(method);
    }
    return this.myResolvedMethod != null;
  }

  public static boolean isStringOrStringArray(PsiType type) {
    if (!(type instanceof PsiClassType) || !"java.lang.String".equals(type.getCanonicalText())) {
      return (type instanceof PsiArrayType) && "java.lang.String".equals(((PsiArrayType) type).getComponentType().getCanonicalText());
    }
    return true;
  }
}
