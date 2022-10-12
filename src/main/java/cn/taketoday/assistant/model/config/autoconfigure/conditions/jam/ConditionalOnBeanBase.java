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

package cn.taketoday.assistant.model.config.autoconfigure.conditions.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.config.jam.StringLiteralPsiClassConverter;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.converters.InfraBeanReferenceJamConverter;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;

abstract class ConditionalOnBeanBase extends JamBaseElement<PsiModifierListOwner> {
  private static final JamClassAttributeMeta.Collection VALUE_ATTRIBUTE = JamAttributeMeta.classCollection("value");
  private static final JamStringAttributeMeta.Collection<PsiClass> TYPE_ATTRIBUTE = JamAttributeMeta.collectionString(InfraMetadataConstant.TYPE, new StringLiteralPsiClassConverter());
  private static final JamClassAttributeMeta.Collection ANNOTATION_ATTRIBUTE = JamAttributeMeta.classCollection("annotation");
  private static final JamStringAttributeMeta.Collection<BeanPointer<?>> NAME_ATTRIBUTE = JamAttributeMeta.collectionString(InfraMetadataConstant.NAME,
          new InfraBeanReferenceJamConverter(null));
  private static final JamClassAttributeMeta.Collection PARAMETRIZED_CONTAINER_ATTRIBUTE = JamAttributeMeta.classCollection("parameterizedContainer");
  private static final JamEnumAttributeMeta.Single<SearchStrategy> SEARCH_STRATEGY_ATTRIBUTE = JamAttributeMeta.singleEnum("search", SearchStrategy.class);
  protected static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype().addAttribute(VALUE_ATTRIBUTE).addAttribute(TYPE_ATTRIBUTE).addAttribute(ANNOTATION_ATTRIBUTE)
          .addAttribute(NAME_ATTRIBUTE).addAttribute(PARAMETRIZED_CONTAINER_ATTRIBUTE).addAttribute(SEARCH_STRATEGY_ATTRIBUTE);

  enum SearchStrategy {
    CURRENT,
    ANCESTORS,
    ALL
  }

  protected abstract JamAnnotationMeta getAnnotationMeta();

  protected ConditionalOnBeanBase(PsiElementRef<?> ref) {
    super(ref);
  }

  public Collection<PsiClass> getValue() {
    List<JamClassAttributeElement> attribute = getValueElements();
    return ContainerUtil.map(attribute, JamClassAttributeElement::getValue);
  }

  public Collection<PsiClass> getType() {
    List<JamStringAttributeElement<PsiClass>> attribute = getTypeElements();
    return ContainerUtil.map(attribute, JamStringAttributeElement::getValue);
  }

  public Collection<PsiClass> getAnnotation() {
    List<JamClassAttributeElement> attribute = getAnnotationElements();
    return ContainerUtil.map(attribute, JamClassAttributeElement::getValue);
  }

  public Collection<String> getName() {
    List<JamStringAttributeElement<BeanPointer<?>>> attribute = getAnnotationMeta().getAttribute(getPsiElement(), NAME_ATTRIBUTE);
    return ContainerUtil.map(attribute, JamStringAttributeElement::getStringValue);
  }

  public Collection<PsiClass> getParametrizedContainer() {
    List<JamClassAttributeElement> attribute = getAnnotationMeta().getAttribute(getPsiElement(), PARAMETRIZED_CONTAINER_ATTRIBUTE);
    return ContainerUtil.map(attribute, JamClassAttributeElement::getValue);
  }

  public SearchStrategy getSearch() {
    SearchStrategy searchStrategy = (getAnnotationMeta().getAttribute(getPsiElement(), SEARCH_STRATEGY_ATTRIBUTE)).getValue();
    return searchStrategy != null ? searchStrategy : SearchStrategy.ALL;
  }

  protected List<JamClassAttributeElement> getValueElements() {
    return getAnnotationMeta().getAttribute(getPsiElement(), VALUE_ATTRIBUTE);
  }

  protected List<JamStringAttributeElement<PsiClass>> getTypeElements() {
    return getAnnotationMeta().getAttribute(getPsiElement(), TYPE_ATTRIBUTE);
  }

  protected List<JamClassAttributeElement> getAnnotationElements() {
    return getAnnotationMeta().getAttribute(getPsiElement(), ANNOTATION_ATTRIBUTE);
  }

  protected Collection<PsiClass> getValidParametrizedContainers() {
    return ContainerUtil.filter(getParametrizedContainer(), container -> {
      return container != null && container.getTypeParameters().length == 1;
    });
  }

  protected void matchBeansByType(CommonInfraModel infraModel, Collection<PsiType> types, Collection<PsiClass> containers, List<CommonInfraBean> ignoredBeans,
          Processor<? super Boolean> matchingProcessor) {
    Iterator var6 = types.iterator();

    label44:
    do {
      while (var6.hasNext()) {
        PsiType psiType = (PsiType) var6.next();
        if (psiType != null) {
          List<CommonInfraBean> beansByType = ContainerUtil.map(
                  ConditionalOnBeanUtils.findBeansByType(infraModel, psiType), BeanPointer.TO_BEAN);
          beansByType.removeAll(ignoredBeans);
          if (!beansByType.isEmpty()) {
            continue label44;
          }

          boolean matched = false;

          for (PsiClass container : containers) {
            PsiClassType containerType = ConditionalOnBeanUtils.getContainerType(container, psiType);
            beansByType = ContainerUtil.map(
                    ConditionalOnBeanUtils.findBeansByType(infraModel, containerType),
                    BeanPointer.TO_BEAN);
            beansByType.removeAll(ignoredBeans);
            if (!beansByType.isEmpty()) {
              if (!matchingProcessor.process(Boolean.TRUE)) {
                return;
              }

              matched = true;
              break;
            }
          }

          if (matched) {
            continue;
          }
        }

        if (!matchingProcessor.process(Boolean.FALSE)) {
          return;
        }
      }

      return;
    }
    while (matchingProcessor.process(Boolean.TRUE));

  }

  protected Collection<PsiType> getTypesToMatch() {
    Collection<PsiClass> valueClasses = getValue();
    Collection<PsiClass> typeClasses = getType();
    if (valueClasses.isEmpty() && typeClasses.isEmpty() && getName().isEmpty()) {
      PsiModifierListOwner psiMethod = getPsiElement();
      PsiType beanType = psiMethod instanceof PsiMethod method ? ConditionalOnBeanUtils.getBeanType(method) : null;
      return beanType == null ? Collections.emptyList() : new SmartList<>(beanType);
    }
    return ContainerUtil.map(ContainerUtil.concat(getValue(), getType()), psiClass -> {
      if (psiClass == null) {
        return null;
      }
      return PsiTypesUtil.getClassType(psiClass);
    });
  }

  protected void matchBeansByAnnotation(CommonInfraModel infraModel, List<CommonInfraBean> ignoredBeans, Processor<? super Boolean> matchingProcessor) {
    Collection<PsiClass> annotations = getAnnotation();
    if (annotations.isEmpty()) {
      return;
    }
    Module module = infraModel.getModule();
    if (module == null) {
      matchingProcessor.process(false);
      return;
    }
    GlobalSearchScope scope = module.getModuleWithDependenciesScope();
    JamService jamService = JamService.getJamService(module.getProject());
    for (PsiClass annotationClass : annotations) {
      if (annotationClass != null && annotationClass.isAnnotationType()) {
        Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope);
        boolean matched = matchAnnotatedBean(infraModel, ignoredBeans, psiMethods, psiElement -> {
          PsiMethod psiMethod = (PsiMethod) psiElement;
          if (psiMethod.isConstructor() || psiMethod.hasModifier(JvmModifier.STATIC) || psiMethod.hasModifier(JvmModifier.PRIVATE)) {
            return null;
          }
          return jamService.getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiElement);
        });
        if (matched) {
          if (!matchingProcessor.process(Boolean.TRUE)) {
            return;
          }
        }
        else {
          Query<PsiClass> psiClasses = AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope);
          boolean matched2 = matchAnnotatedBean(infraModel, ignoredBeans, psiClasses, psiElement2 -> {
            if (!InfraUtils.isBeanCandidateClass((PsiClass) psiElement2)) {
              return null;
            }
            return jamService.getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiElement2);
          });
          if (matched2) {
            if (!matchingProcessor.process(Boolean.TRUE)) {
              return;
            }
          }
        }
      }
      if (!matchingProcessor.process(Boolean.FALSE)) {
        return;
      }
    }
  }

  private static boolean matchAnnotatedBean(CommonInfraModel infraModel, List<CommonInfraBean> ignoredBeans, Query<? extends PsiElement> query, Function<PsiElement, CommonInfraBean> mapper) {
    return query.anyMatch(psiElement -> {
      PsiType psiType;
      CommonInfraBean bean = mapper.apply(psiElement);
      if (bean == null || ignoredBeans.contains(bean) || (psiType = bean.getBeanType()) == null) {
        return false;
      }
      List<BeanPointer<?>> pointers = InfraModelSearchers.findBeans(infraModel, ModelSearchParameters.byType(psiType));
      for (BeanPointer<?> pointer : pointers) {
        if (pointer.isValid() && bean.equals(pointer.getBean())) {
          return true;
        }
      }
      return false;
    });
  }

  protected void matchBeansByName(CommonInfraModel infraModel, List<CommonInfraBean> ignoredBeans, Processor<? super Boolean> matchingProcessor) {
    Collection<String> names = getName();
    if (names.isEmpty()) {
      return;
    }
    List<String> ignoredNames = ContainerUtil.mapNotNull(ignoredBeans, CommonInfraBean::getBeanName);
    for (String name : names) {
      boolean matched = !ignoredNames.contains(name) && !InfraModelSearchers.findBeans(infraModel, name).isEmpty();
      if (!matchingProcessor.process(matched)) {
        return;
      }
    }
  }
}
