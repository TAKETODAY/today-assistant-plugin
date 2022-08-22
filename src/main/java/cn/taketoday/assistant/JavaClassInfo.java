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
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringModelVisitorUtils;
import com.intellij.spring.SpringModificationTrackersManager;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.JamSpringBeanPointer;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.utils.SpringBeanUtils;
import com.intellij.spring.model.utils.SpringConstructorArgUtils;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.spring.model.xml.DomSpringBean;
import com.intellij.spring.model.xml.DomSpringBeanPointer;
import com.intellij.spring.model.xml.beans.Autowire;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.model.xml.beans.LookupMethod;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition;
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

/**
 * Caches mappings for given PsiClass.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 19:29
 */
public class JavaClassInfo {
  private static final Key<JavaClassInfo> KEY = Key.create("Java Class Info");

  private final PsiClass myPsiClass;
  private final CachedValue<Boolean> myIsMapped;
  private final CachedValue<Set<SpringBeanPointer<?>>> myBeans;
  private final CachedValue<MultiMap<String, SpringPropertyDefinition>> myProperties;
  private final CachedValue<MultiMap<PsiMethod, SpringBeanPointer<?>>> myConstructors;
  private final CachedValue<MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>>> myMethods;

  public static JavaClassInfo getSpringJavaClassInfo(PsiClass psiClass) {
    JavaClassInfo info = psiClass.getUserData(KEY);
    if (info == null) {
      info = new JavaClassInfo(psiClass);
      psiClass.putUserData(KEY, info);
    }
    return info;
  }

  private JavaClassInfo(PsiClass psiClass) {
    myPsiClass = psiClass;
    final Project project = psiClass.getProject();

    CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(project);
    myIsMapped = cachedValuesManager.createCachedValue(() -> {
      CommonSpringModel model = SpringModelUtils.getInstance().getPsiClassSpringModel(myPsiClass);

      boolean exists = SpringModelSearchers.doesBeanExist(model, myPsiClass);
      return new CachedValueProvider.Result<>(exists, getDependencies(project, model));
    }, false);

    myBeans = cachedValuesManager.createCachedValue(() -> {
      CommonSpringModel model = SpringModelUtils.getInstance().getPsiClassSpringModel(myPsiClass);
      List<SpringBeanPointer<?>> byInheritance =
              SpringModelSearchers.findBeans(model, SpringModelSearchParameters.byClass(myPsiClass).withInheritors().effectiveBeanTypes());
      return new CachedValueProvider.Result<>(new LinkedHashSet<>(byInheritance),
              getDependencies(project, model));
    }, false);

    myProperties = cachedValuesManager.createCachedValue(() -> {
      List<DomSpringBeanPointer> list = getMappedDomBeans();
      MultiMap<String, SpringPropertyDefinition> map = MultiMap.createConcurrent();
      for (DomSpringBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomSpringBean bean = beanPointer.getSpringBean();
        if (bean instanceof SpringBean) {
          List<SpringPropertyDefinition> properties = ((SpringBean) bean).getAllProperties();
          for (SpringPropertyDefinition property : properties) {
            String propertyName = property.getPropertyName();
            if (propertyName != null) {
              map.putValue(propertyName, property);
            }
          }
        }
      }
      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project));
    }, false);
    myConstructors = cachedValuesManager.createCachedValue(() -> {
      List<DomSpringBeanPointer> list = getMappedDomBeans();
      MultiMap<PsiMethod, SpringBeanPointer<?>> map = MultiMap.createConcurrent();
      for (DomSpringBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomSpringBean bean = beanPointer.getSpringBean();
        if (bean instanceof SpringBean) {
          CommonSpringModel model = SpringModelUtils.getInstance().getPsiClassSpringModel(myPsiClass);
          PsiMethod constructor = SpringConstructorArgUtils.getInstance().getSpringBeanConstructor((SpringBean) bean, model);
          if (constructor != null) {
            map.putValue(constructor, beanPointer);
          }
        }
      }
      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project));
    }, false);
    myMethods = cachedValuesManager.createCachedValue(() -> {
      List<PsiMethod> psiMethods = Arrays.asList(psiClass.getMethods());

      List<DomSpringBeanPointer> list = getMappedDomBeans();
      MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>> map = MultiMap.createConcurrent();

      for (JamSpringBeanPointer pointer : getStereotypeMappedBeans()) {
        JamPsiMemberSpringBean bean = pointer.getSpringBean();
        if (bean instanceof ContextJavaBean) {
          addStereotypeBeanMethod(map, ((ContextJavaBean) bean).getInitMethodAttributeElement(), JavaClassInfo.SpringMethodType.INIT);
          addStereotypeBeanMethod(map, ((ContextJavaBean) bean).getDestroyMethodAttributeElement(), JavaClassInfo.SpringMethodType.DESTROY);
        }
      }

      for (DomSpringBeanPointer beanPointer : list) {
        if (!beanPointer.isValid())
          continue;
        DomSpringBean bean = beanPointer.getSpringBean();
        if (bean instanceof SpringBean springBean) {
          Beans beans = DomUtil.getParentOfType(bean, Beans.class, false);
          if (beans != null) {
            addSpringBeanMethods(map, psiMethods, JavaClassInfo.SpringMethodType.INIT, beans.getDefaultInitMethod());
            addSpringBeanMethods(map, psiMethods, JavaClassInfo.SpringMethodType.DESTROY, beans.getDefaultDestroyMethod());
          }

          addSpringBeanMethod(map, psiMethods, JavaClassInfo.SpringMethodType.INIT, springBean.getInitMethod());
          addSpringBeanMethod(map, psiMethods, JavaClassInfo.SpringMethodType.DESTROY, springBean.getDestroyMethod());

          for (LookupMethod lookupMethod : springBean.getLookupMethods()) {
            addSpringBeanMethod(map, psiMethods, JavaClassInfo.SpringMethodType.LOOKUP, lookupMethod.getName());
          }
        }
      }

      CommonProcessors.CollectProcessor<SpringBeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>();
      SpringBeanUtils.getInstance().processXmlFactoryBeans(myPsiClass.getProject(), myPsiClass.getResolveScope(), processor);
      for (SpringBeanPointer pointer : processor.getResults()) {
        if (!pointer.isValid())
          continue;
        CommonSpringBean commonSpringBean = pointer.getSpringBean();
        if (commonSpringBean instanceof SpringBean springBean && commonSpringBean.isValid()) {
          GenericAttributeValue<PsiMethod> domFactoryMethod = springBean.getFactoryMethod();
          if (DomUtil.hasXml(domFactoryMethod)) {
            PsiMethod factoryMethod = domFactoryMethod.getValue();
            if (factoryMethod != null && psiMethods.contains(factoryMethod)) {
              addSpringBeanMethod(map, psiMethods, JavaClassInfo.SpringMethodType.FACTORY, domFactoryMethod);
            }
          }
        }
      }

      return new CachedValueProvider.Result<>(map, DomManager.getDomManager(project), myPsiClass);
    }, false);
  }

  private static void addStereotypeBeanMethod(MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>> map,
          JamStringAttributeElement<PsiMethod> attributeElement,
          JavaClassInfo.SpringMethodType type) {
    PsiMethod psiMethod = attributeElement.getValue();
    if (psiMethod != null) {
      PsiAnnotationMemberValue identifyingElement = attributeElement.getPsiElement();
      if (identifyingElement != null) {
        map.putValue(psiMethod, Pair.create(identifyingElement.getParent(), type));
      }
    }
  }

  private Object[] getDependencies(Project project, CommonSpringModel model) {
    Set<Object> dependencies = new LinkedHashSet<>();
    ContainerUtil.addIfNotNull(dependencies, myPsiClass.getContainingFile());

    ContainerUtil.addAll(dependencies, SpringModificationTrackersManager.getInstance(project).getOuterModelsDependencies());
    dependencies.addAll(SpringModelVisitorUtils.getConfigFiles(model).stream().filter(file -> !(file instanceof ClsFileImpl))
            .collect(Collectors.toSet()));

    return ArrayUtil.toObjectArray(dependencies);
  }

  private static void addSpringBeanMethod(MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>> map,
          List<PsiMethod> psiMethods,
          JavaClassInfo.SpringMethodType type,
          GenericAttributeValue<PsiMethod> genericAttributeValue) {
    if (!DomUtil.hasXml(genericAttributeValue))
      return;

    PsiMethod psiMethod = genericAttributeValue.getValue();
    if (psiMethod != null && psiMethods.contains(psiMethod)) {
      map.putValue(psiMethod, Pair.create(genericAttributeValue.getXmlAttribute(), type));
    }
  }

  private static void addSpringBeanMethods(MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>> map,
          List<PsiMethod> psiMethods,
          JavaClassInfo.SpringMethodType type,
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
    return myIsMapped.getValue();
  }

  public boolean isCalculatedMapped() {
    Supplier<Boolean> getter = myIsMapped.getUpToDateOrNull();
    return getter != null && getter.get();
  }

  public boolean isMappedDomBean() {
    return isMapped() && ContainerUtil.findInstance(myBeans.getValue(), DomSpringBeanPointer.class) != null;
  }

  public List<DomSpringBeanPointer> getMappedDomBeans() {
    return !isMapped()
           ? Collections.emptyList()
           : ContainerUtil.findAll(myBeans.getValue(), DomSpringBeanPointer.class);
  }

  public boolean isStereotypeJavaBean() {
    return isMapped() && ContainerUtil.findInstance(myBeans.getValue(), JamSpringBeanPointer.class) != null;
  }

  public List<JamSpringBeanPointer> getStereotypeMappedBeans() {
    return !isMapped()
           ? Collections.emptyList()
           : ContainerUtil.findAll(myBeans.getValue(), JamSpringBeanPointer.class);
  }

  public boolean isAutowired() {
    List<DomSpringBeanPointer> pointers = getMappedDomBeans();
    for (DomSpringBeanPointer pointer : pointers) {
      DomSpringBean springBean = pointer.getSpringBean();
      if (springBean instanceof SpringBean) {
        Autowire autowire = ((SpringBean) springBean).getBeanAutowire();
        if (autowire.isAutowired()) {
          return true;
        }
      }
    }
    return false;
  }

  public Set<Autowire> getAutowires() {
    Set<Autowire> autowires = EnumSet.noneOf(Autowire.class);
    for (DomSpringBeanPointer pointer : getMappedDomBeans()) {
      DomSpringBean springBean = pointer.getSpringBean();
      if (springBean instanceof SpringBean) {
        Autowire autowire = ((SpringBean) springBean).getBeanAutowire();
        if (autowire.isAutowired()) {
          autowires.add(autowire);
        }
      }
    }
    return autowires;
  }

  public Collection<SpringPropertyDefinition> getMappedProperties(String propertyName) {
    MultiMap<String, SpringPropertyDefinition> value = myProperties.getValue();
    if (value == null) {
      return Collections.emptyList();
    }
    return value.get(propertyName);
  }

  public Collection<SpringBeanPointer<?>> getMappedConstructorDefinitions(PsiMethod psiMethod) {
    MultiMap<PsiMethod, SpringBeanPointer<?>> value = myConstructors.getValue();
    if (value == null) {
      return Collections.emptyList();
    }
    return value.get(psiMethod);
  }

  public Collection<Pair<PsiElement, JavaClassInfo.SpringMethodType>> getMethodTypes(PsiMethod psiMethod) {
    MultiMap<PsiMethod, Pair<PsiElement, JavaClassInfo.SpringMethodType>> value = myMethods.getValue();
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

    return myConstructors.getValue().containsKey(method);
  }

  public enum SpringMethodType {
    INIT("init"), DESTROY("destroy"),
    FACTORY("factory"), LOOKUP("lookup");
    private final String myName;

    SpringMethodType(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }

  }
}
