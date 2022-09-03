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

package cn.taketoday.assistant;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;
import cn.taketoday.assistant.model.utils.InfraConstructorArgUtils;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.DomBeanPointer;
import cn.taketoday.assistant.model.xml.DomInfraBean;
import cn.taketoday.assistant.model.xml.beans.Autowire;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.model.xml.beans.LookupMethod;

/**
 * Caches mappings for given PsiClass.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 19:29
 */
public class JavaClassInfo {
  private static final Key<JavaClassInfo> KEY = Key.create("Java Class Info");

  private final PsiClass psiClass;
  private final CachedValue<Boolean> isMapped;
  private final CachedValue<Set<BeanPointer<?>>> beans;
  private final CachedValue<MultiMap<String, InfraPropertyDefinition>> properties;
  private final CachedValue<MultiMap<PsiMethod, BeanPointer<?>>> constructors;
  private final CachedValue<MultiMap<PsiMethod, Pair<PsiElement, MethodType>>> methods;

  public static JavaClassInfo from(PsiClass psiClass) {
    JavaClassInfo info = psiClass.getUserData(KEY);
    if (info == null) {
      info = new JavaClassInfo(psiClass);
      psiClass.putUserData(KEY, info);
    }
    return info;
  }

  private JavaClassInfo(PsiClass psiClass) {
    this.psiClass = psiClass;
    Project project = psiClass.getProject();

    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(project);
    isMapped = cachedValuesManager.createCachedValue(() -> {
      CommonInfraModel model = InfraModelService.of().getPsiClassModel(this.psiClass);

      boolean exists = InfraModelSearchers.doesBeanExist(model, this.psiClass);
      return new CachedValueProvider.Result<>(exists, getDependencies(project, model));
    }, false);

    beans = cachedValuesManager.createCachedValue(() -> {
      CommonInfraModel model = InfraModelService.of().getPsiClassModel(this.psiClass);
      List<BeanPointer<?>> byInheritance =
              InfraModelSearchers.findBeans(model, ModelSearchParameters.byClass(this.psiClass).withInheritors().effectiveBeanTypes());
      return new CachedValueProvider.Result<>(new LinkedHashSet<>(byInheritance),
              getDependencies(project, model));
    }, false);

    properties = cachedValuesManager.createCachedValue(() -> {
      List<DomBeanPointer> list = getMappedDomBeans();
      MultiMap<String, InfraPropertyDefinition> map = MultiMap.createConcurrent();
      for (DomBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomInfraBean bean = beanPointer.getBean();
        if (bean instanceof InfraBean) {
          List<InfraPropertyDefinition> properties = ((InfraBean) bean).getAllProperties();
          for (InfraPropertyDefinition property : properties) {
            String propertyName = property.getPropertyName();
            if (propertyName != null) {
              map.putValue(propertyName, property);
            }
          }
        }
      }
      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project));
    }, false);
    constructors = cachedValuesManager.createCachedValue(() -> {
      List<DomBeanPointer> list = getMappedDomBeans();
      MultiMap<PsiMethod, BeanPointer<?>> map = MultiMap.createConcurrent();
      for (DomBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomInfraBean bean = beanPointer.getBean();
        if (bean instanceof InfraBean) {
          CommonInfraModel model = InfraModelService.of().getPsiClassModel(this.psiClass);
          PsiMethod constructor = InfraConstructorArgUtils.of().getInfraBeanConstructor((InfraBean) bean, model);
          if (constructor != null) {
            map.putValue(constructor, beanPointer);
          }
        }
      }
      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project));
    }, false);
    methods = cachedValuesManager.createCachedValue(() -> {
      List<PsiMethod> psiMethods = Arrays.asList(psiClass.getMethods());

      List<DomBeanPointer> list = getMappedDomBeans();
      MultiMap<PsiMethod, Pair<PsiElement, MethodType>> map = MultiMap.createConcurrent();

      for (JamBeanPointer pointer : getStereotypeMappedBeans()) {
        JamPsiMemberInfraBean bean = pointer.getBean();
        if (bean instanceof ContextJavaBean) {
          addStereotypeBeanMethod(map, ((ContextJavaBean) bean).getInitMethodAttributeElement(), MethodType.INIT);
          addStereotypeBeanMethod(map, ((ContextJavaBean) bean).getDestroyMethodAttributeElement(), MethodType.DESTROY);
        }
      }

      for (DomBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomInfraBean bean = beanPointer.getBean();
        if (bean instanceof InfraBean infraBean) {
          Beans beans = DomUtil.getParentOfType(bean, Beans.class, false);
          if (beans != null) {
            addInfraBeanMethods(map, psiMethods, MethodType.INIT, beans.getDefaultInitMethod());
            addInfraBeanMethods(map, psiMethods, MethodType.DESTROY, beans.getDefaultDestroyMethod());
          }

          addInfraBeanMethod(map, psiMethods, MethodType.INIT, infraBean.getInitMethod());
          addInfraBeanMethod(map, psiMethods, MethodType.DESTROY, infraBean.getDestroyMethod());

          for (LookupMethod lookupMethod : infraBean.getLookupMethods()) {
            addInfraBeanMethod(map, psiMethods, MethodType.LOOKUP, lookupMethod.getName());
          }
        }
      }

      var processor = new CommonProcessors.CollectProcessor<BeanPointer<?>>();
      InfraBeanUtils.of().processXmlFactoryBeans(this.psiClass.getProject(), this.psiClass.getResolveScope(), processor);
      for (BeanPointer pointer : processor.getResults()) {
        if (!pointer.isValid())
          continue;
        CommonInfraBean commonInfraBean = pointer.getBean();
        if (commonInfraBean instanceof InfraBean infraBean && commonInfraBean.isValid()) {
          GenericAttributeValue<PsiMethod> domFactoryMethod = infraBean.getFactoryMethod();
          if (DomUtil.hasXml(domFactoryMethod)) {
            PsiMethod factoryMethod = domFactoryMethod.getValue();
            if (factoryMethod != null && psiMethods.contains(factoryMethod)) {
              addInfraBeanMethod(map, psiMethods, MethodType.FACTORY, domFactoryMethod);
            }
          }
        }
      }

      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project), this.psiClass);
    }, false);
  }

  private static void addStereotypeBeanMethod(MultiMap<PsiMethod, Pair<PsiElement, MethodType>> map,
          JamStringAttributeElement<PsiMethod> attributeElement,
          MethodType type) {
    PsiMethod psiMethod = attributeElement.getValue();
    if (psiMethod != null) {
      PsiAnnotationMemberValue identifyingElement = attributeElement.getPsiElement();
      if (identifyingElement != null) {
        map.putValue(psiMethod, Pair.create(identifyingElement.getParent(), type));
      }
    }
  }

  private Object[] getDependencies(Project project, CommonInfraModel model) {
    Set<Object> dependencies = new LinkedHashSet<>();
    ContainerUtil.addIfNotNull(dependencies, psiClass.getContainingFile());

    ContainerUtil.addAll(dependencies, InfraModificationTrackersManager.from(project).getOuterModelsDependencies());
    dependencies.addAll(InfraModelVisitorUtils.getConfigFiles(model).stream().filter(file -> !(file instanceof ClsFileImpl))
            .collect(Collectors.toSet()));

    return ArrayUtil.toObjectArray(dependencies);
  }

  private static void addInfraBeanMethod(MultiMap<PsiMethod, Pair<PsiElement, MethodType>> map,
          List<PsiMethod> psiMethods,
          MethodType type,
          GenericAttributeValue<PsiMethod> genericAttributeValue) {
    if (!DomUtil.hasXml(genericAttributeValue))
      return;

    PsiMethod psiMethod = genericAttributeValue.getValue();
    if (psiMethod != null && psiMethods.contains(psiMethod)) {
      map.putValue(psiMethod, Pair.create(genericAttributeValue.getXmlAttribute(), type));
    }
  }

  private static void addInfraBeanMethods(MultiMap<PsiMethod, Pair<PsiElement, MethodType>> map,
          List<PsiMethod> psiMethods,
          MethodType type,
          GenericAttributeValue<Set<PsiMethod>> genericAttributeValue) {
    if (!DomUtil.hasXml(genericAttributeValue))
      return;

    Set<PsiMethod> methods = genericAttributeValue.getValue();
    if (methods != null) {
      for (PsiMethod psiMethod : methods) {
        if (psiMethods.contains(psiMethod)) {
          map.putValue(psiMethod, Pair.create(genericAttributeValue.getXmlAttribute(), type));
        }
      }
    }
  }

  public boolean isMapped() {
    return isMapped.getValue();
  }

  public boolean isCalculatedMapped() {
    Supplier<Boolean> getter = isMapped.getUpToDateOrNull();
    return getter != null && getter.get();
  }

  public boolean isMappedDomBean() {
    return isMapped() && ContainerUtil.findInstance(beans.getValue(), DomBeanPointer.class) != null;
  }

  public List<DomBeanPointer> getMappedDomBeans() {
    return !isMapped()
           ? Collections.emptyList()
           : ContainerUtil.findAll(beans.getValue(), DomBeanPointer.class);
  }

  public boolean isStereotypeJavaBean() {
    return isMapped() && ContainerUtil.findInstance(beans.getValue(), JamBeanPointer.class) != null;
  }

  public List<JamBeanPointer> getStereotypeMappedBeans() {
    return !isMapped()
           ? Collections.emptyList()
           : ContainerUtil.findAll(beans.getValue(), JamBeanPointer.class);
  }

  public boolean isAutowired() {
    List<DomBeanPointer> pointers = getMappedDomBeans();
    for (DomBeanPointer pointer : pointers) {
      DomInfraBean infraBean = pointer.getBean();
      if (infraBean instanceof InfraBean) {
        Autowire autowire = ((InfraBean) infraBean).getBeanAutowire();
        if (autowire.isAutowired()) {
          return true;
        }
      }
    }
    return false;
  }

  public Set<Autowire> getAutowires() {
    Set<Autowire> autowires = EnumSet.noneOf(Autowire.class);
    for (DomBeanPointer pointer : getMappedDomBeans()) {
      DomInfraBean bean = pointer.getBean();
      if (bean instanceof InfraBean infraBean) {
        Autowire autowire = infraBean.getBeanAutowire();
        if (autowire.isAutowired()) {
          autowires.add(autowire);
        }
      }
    }
    return autowires;
  }

  public Collection<InfraPropertyDefinition> getMappedProperties(String propertyName) {
    MultiMap<String, InfraPropertyDefinition> value = properties.getValue();
    if (value == null) {
      return Collections.emptyList();
    }
    return value.get(propertyName);
  }

  public Collection<BeanPointer<?>> getMappedConstructorDefinitions(PsiMethod psiMethod) {
    MultiMap<PsiMethod, BeanPointer<?>> value = constructors.getValue();
    if (value == null) {
      return Collections.emptyList();
    }
    return value.get(psiMethod);
  }

  public Collection<Pair<PsiElement, MethodType>> getMethodTypes(PsiMethod psiMethod) {
    MultiMap<PsiMethod, Pair<PsiElement, MethodType>> value = methods.getValue();
    if (value == null) {
      return Collections.emptyList();
    }
    return value.get(psiMethod);
  }

  public boolean isMappedProperty(PsiMethod method) {
    String propertyName = StringUtil.getPropertyName(method.getName());
    if (propertyName == null)
      return false;
    return !getMappedProperties(propertyName).isEmpty();
  }

  public boolean isMappedConstructor(PsiMethod method) {
    if (!method.isConstructor())
      return false;

    return constructors.getValue().containsKey(method);
  }

  public enum MethodType {
    INIT("init"),
    DESTROY("destroy"),
    FACTORY("factory"),
    LOOKUP("lookup");
    private final String myName;

    MethodType(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }

  }
}
