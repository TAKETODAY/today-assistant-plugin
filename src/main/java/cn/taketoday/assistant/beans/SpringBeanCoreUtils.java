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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.SpringManager;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.factories.SpringFactoryBeansManager;
import com.intellij.spring.model.BeanService;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.DomSpringBean;
import com.intellij.spring.model.xml.beans.Beans;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.model.xml.beans.SpringEntry;
import com.intellij.spring.model.xml.beans.SpringProperty;
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.reflect.AbstractDomChildrenDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.util.CommonUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 12:06
 */
public final class SpringBeanCoreUtils {
  static final boolean $assertionsDisabled = !SpringBeanCoreUtils.class.desiredAssertionStatus();

  public static String getReferencedName(SpringBeanPointer<?> bean, Collection<? extends SpringBeanPointer<?>> allBeans) {
    String beanName = bean.getName();
    if (beanName != null) {
      return beanName;
    }
    for (PsiType psiClassType : bean.getEffectiveBeanTypes()) {
      String className = null;
      if (psiClassType instanceof PsiClassType type) {
        PsiClass psiClass = type.resolve();
        if (psiClass == null) {
          continue;
        }
        else {
          className = psiClass.getQualifiedName();
        }
      }
      if (className != null) {
        List<SpringBeanPointer<?>> list = findBeansByClassName(allBeans, className);
        if (list.size() == 1) {
          return className;
        }
      }
    }
    return null;
  }

  public static boolean isEffectiveClassType(List<? extends PsiType> psiTypes, CommonSpringBean context) {
    for (PsiType psiType : psiTypes) {
      if (isEffectiveClassType(context, psiType, context.getModule())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isEffectiveClassType(CommonSpringBean context, PsiType requiredType, Module module) {
    PsiType iterableType;
    if (context instanceof SpringBean springBean) {
      PsiMethod factoryMethod = springBean.getFactoryMethod().getValue();
      if (factoryMethod != null) {
        if (SpringFactoryBeansManager.getInstance().isValidFactoryMethod(factoryMethod, springBean.getFactoryBean().getValue() != null)) {
          PsiType returnType = factoryMethod.getReturnType();
          if (returnType != null && requiredType.isAssignableFrom(returnType)) {
            return true;
          }
          if (returnType instanceof PsiArrayType arrayType
                  && (iterableType = PsiUtil.extractIterableTypeParameter(requiredType, false)) != null) {
            return TypeConversionUtil.isAssignable(arrayType.getComponentType(), iterableType);
          }
        }
      }
    }
    PsiType[] effectiveBeanTypes = BeanService.getInstance().getEffectiveBeanTypes(context);
    if (effectiveBeanTypes.length > 0) {
      for (PsiType beanType : effectiveBeanTypes) {
        if (TypeConversionUtil.isAssignable(requiredType, beanType)) {
          return true;
        }
        PsiClass aClass = PsiTypesUtil.getPsiClass(beanType);
        if (aClass != null) {
          for (PsiClass psiClass : getRequiredClasses(requiredType, module)) {
            if (InheritanceUtil.isInheritorOrSelf(aClass, psiClass, true)) {
              return true;
            }
          }
          if ("java.lang.String".equals(aClass.getQualifiedName()) && isCustomEditorRegistered(requiredType, module)) {
            return true;
          }
        }
      }
      return false;
    }
    return isUnusualBeanFactoriesUsed(context);
  }

  private static Collection<PsiClass> getRequiredClasses(PsiType requiredType, Module module) {
    PsiClass psiClass = resolvePsiClass(requiredType, module);
    if (psiClass == null) {
      return Collections.emptySet();
    }
    else if (psiClass instanceof PsiTypeParameter) {
      return Arrays.stream(psiClass.getSuperTypes())
              .map(PsiClassType::resolve)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
    }
    else {
      return Collections.singleton(psiClass);
    }
  }

  private static boolean isCustomEditorRegistered(PsiType requiredType, Module module) {
    for (PsiType psiType : getCustomEditorConvertableClasses(module)) {
      if (psiType.isAssignableFrom(requiredType)) {
        return true;
      }
    }
    return false;
  }

  private static Set<PsiType> getCustomEditorConvertableClasses(Module module) {
    PsiClass customEditorConfigurer = CommonUtils.findLibraryClass(module, InfraConstant.CUSTOM_EDITOR_CONFIGURER_CLASS);
    if (customEditorConfigurer == null) {
      return Collections.emptySet();
    }
    Set<PsiType> customEditorConvertableClasses = new HashSet<>();
    SpringModelSearchParameters.BeanClass searchParameters = SpringModelSearchParameters.byClass(customEditorConfigurer).withInheritors();
    SpringModel springModel = SpringManager.getInstance(module.getProject()).getCombinedModel(module);
    for (SpringBeanPointer configurer : SpringModelSearchers.findBeans(springModel, searchParameters)) {
      SpringPropertyDefinition property = SpringPropertyUtils.findPropertyByName(configurer.getSpringBean(), "customEditors");
      if (property instanceof SpringProperty) {
        for (SpringEntry entry : ((SpringProperty) property).getMap().getEntries()) {
          addPsiClass(module, customEditorConvertableClasses, entry.getKeyAttr().getStringValue());
          addPsiClass(module, customEditorConvertableClasses, entry.getKey().getValueAsString());
        }
      }
    }
    return customEditorConvertableClasses;
  }

  private static boolean addPsiClass(Module module, Set<? super PsiType> customEditorClasses, @Nullable String fqn) {
    JavaPsiFacade facade;
    PsiClass aClass;
    if (!StringUtil.isEmptyOrSpaces(fqn) && (aClass = (facade = JavaPsiFacade.getInstance(module.getProject())).findClass(fqn, GlobalSearchScope.moduleWithLibrariesScope(module))) != null) {
      return customEditorClasses.add(facade.getElementFactory().createType(aClass));
    }
    return false;
  }

  private static boolean isUnusualBeanFactoriesUsed(CommonSpringBean context) {
    PsiClass beanClass;
    PsiType beanType = context.getBeanType();
    if (beanType != null && (beanClass = PsiTypesUtil.getPsiClass(beanType)) != null && SpringFactoryBeansManager.getInstance().isFactoryBeanClass(beanClass)) {
      SpringFactoryBeansManager manager = SpringFactoryBeansManager.getInstance();
      return !manager.isKnownBeanFactory(beanClass.getQualifiedName()) || manager.getObjectTypes(beanType, context).length == 0;
    }
    return false;
  }

  @Nullable
  private static PsiClass resolvePsiClass(PsiType psiType, Module module) {
    if (psiType instanceof PsiClassType) {
      return ((PsiClassType) psiType).resolve();
    }
    if ((psiType instanceof PsiPrimitiveType) && module != null) {
      GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
      PsiManager psiManager = PsiManager.getInstance(module.getProject());
      PsiClassType boxedType = ((PsiPrimitiveType) psiType).getBoxedType(psiManager, scope);
      if (boxedType != null) {
        return boxedType.resolve();
      }
      return null;
    }
    return null;
  }

  public static String[] suggestBeanNames(@Nullable CommonSpringBean springBean) {
    if (springBean == null) {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }
    PsiClass beanClass = PsiTypesUtil.getPsiClass(springBean.getBeanType());
    if (beanClass == null) {
      return ArrayUtilRt.EMPTY_STRING_ARRAY;
    }
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(beanClass.getProject());
    Set<String> initialNames = new LinkedHashSet<>();
    PsiType[] productClasses = BeanService.getInstance().getEffectiveBeanTypes(springBean);
    for (PsiType productType : productClasses) {
      Set<String> suggestions = getSanitizedBeanNameSuggestions(codeStyleManager, productType);
      initialNames.addAll(suggestions);
    }
    CommonSpringModel model = SpringModelUtils.getInstance().getSpringModelByBean(springBean);
    List<String> uniqueNames = new ArrayList<>();
    for (String name : initialNames) {
      String suggestedName = name;

      for (int i = 1; uniqueNames.contains(suggestedName)
              || SpringModelSearchers.findBean(model, suggestedName) != null; suggestedName = name + i) {
        ++i;
      }

      uniqueNames.add(suggestedName);
    }
    return ArrayUtilRt.toStringArray(uniqueNames);
  }

  public static Set<String> getSanitizedBeanNameSuggestions(JavaCodeStyleManager codeStyleManager, PsiType beanType) {
    SuggestedNameInfo nameInfo = codeStyleManager.suggestVariableName(VariableKind.PARAMETER, null, null, beanType);
    String prefix = codeStyleManager.getPrefixByVariableKind(VariableKind.PARAMETER);
    String suffix = codeStyleManager.getSuffixByVariableKind(VariableKind.PARAMETER);
    Set<String> sanitizedNames = new LinkedHashSet<>();
    for (String name : nameInfo.names) {
      String stripPrefix = StringUtil.trimStart(name, prefix);
      String stripSuffix = StringUtil.trimEnd(stripPrefix, suffix);
      String lowercaseStrip = StringUtil.decapitalize(stripSuffix);
      sanitizedNames.add(lowercaseStrip);
    }
    return sanitizedNames;
  }

  @Nullable
  public static DomSpringBean getSpringBeanForCurrentCaretPosition(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    return findBeanByPsiElement(element);
  }

  @Nullable
  public static DomSpringBean findBeanByPsiElement(PsiElement element) {
    return DomUtil.findDomElement(element, DomSpringBean.class);
  }

  public static DomSpringBean getTopLevelBean(DomSpringBean bean) {
    DomElement parent = bean.getParent();
    if (parent instanceof Beans) {
      return bean;
    }
    SpringBean parentBean = bean.getParentOfType(SpringBean.class, true);
    if (!$assertionsDisabled && parentBean == null) {
      throw new AssertionError(bean);
    }
    return getTopLevelBean(parentBean);
  }

  public static boolean visitParents(SpringBean springBean, boolean strict, Processor<? super SpringBean> processor) {
    SpringBeanPointer<?> parent;

    if (!processor.process(springBean)) {
      return false;
    }
    GenericAttributeValue<SpringBeanPointer<?>> parentBean = springBean.getParentBean();
    if (!DomUtil.hasXml(parentBean) || (parent = parentBean.getValue()) == null) {
      return true;
    }
    Set<CommonSpringBean> visited = new HashSet<>(3);
    if (!strict) {
      visited.add(springBean);
    }
    CommonSpringBean bean = parent.getSpringBean();
    SpringBeanPointer<?> parent2;
    while (bean instanceof SpringBean) {
      if (!processor.process((SpringBean) bean)) {
        return false;
      }
      GenericAttributeValue<SpringBeanPointer<?>> nextParentBean = ((SpringBean) bean).getParentBean();
      if (!DomUtil.hasXml(nextParentBean) || (parent2 = nextParentBean.getValue()) == null) {
        return true;
      }
      bean = parent2.getSpringBean();
      if (visited.contains(bean)) {
        return true;
      }
      visited.add(bean);
    }
    return true;
  }

  public static <T extends GenericDomValue<?>> T getMergedValue(SpringBean springBean, T value) {
    AbstractDomChildrenDescription description = value.getChildDescription();
    Ref<GenericDomValue<?>> ref = new Ref<>(value);
    visitParents(springBean, false, parentSpringBean -> {
      List<? extends DomElement> list = description.getValues(parentSpringBean);
      if (list.size() == 1) {
        GenericDomValue genericDomValue = (GenericDomValue) list.get(0);
        if (DomUtil.hasXml(genericDomValue)) {
          ref.set(genericDomValue);
          return false;
        }
        return true;
      }
      return true;
    });
    return (T) ref.get();
  }

  public static <T extends DomElement> Set<T> getMergedSet(SpringBean springBean, Function<? super SpringBean, ? extends Collection<T>> getter) {
    Set<T> set = new LinkedHashSet<>();
    visitParents(springBean, false, parentSpringBean -> {
      set.addAll(getter.fun(parentSpringBean));
      return true;
    });
    return set;
  }

  public static List<SpringBeanPointer<?>> getBeansByType(PsiType type, CommonSpringModel model) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
    if (CommonUtils.isBeanCandidateClass(psiClass)) {
      return SpringModelSearchers.findBeans(model, SpringModelSearchParameters.byClass(psiClass).withInheritors().effectiveBeanTypes());
    }
    return Collections.emptyList();
  }

  public static List<SpringBeanPointer<?>> findBeansByClassName(Collection<? extends SpringBeanPointer<?>> beans, String className) {
    SmartList smartList = new SmartList();
    for (SpringBeanPointer bean : beans) {
      PsiClass beanClass = bean.getBeanClass();
      if (beanClass != null && className.equals(beanClass.getQualifiedName())) {
        smartList.add(bean);
      }
    }
    return smartList;
  }

  public static Set<PsiType> getFactoryBeanTypes(PsiType factoryBeanType, @Nullable CommonSpringBean bean) {
    PsiClass psiClass;
    Set<PsiType> types = new LinkedHashSet<>();
    if ((factoryBeanType instanceof PsiClassType) && (psiClass = ((PsiClassType) factoryBeanType).resolve()) != null && SpringFactoryBeansManager.getInstance().isFactoryBeanClass(psiClass)) {
      if (InfraConstant.FACTORY_BEAN.equals(psiClass.getQualifiedName())) {
        ContainerUtil.addIfNotNull(types, PsiUtil.substituteTypeParameter(factoryBeanType, InfraConstant.FACTORY_BEAN, 0, false));
      }
      else {
        ContainerUtil.addAllNotNull(types, SpringFactoryBeansManager.getInstance().getObjectTypes(factoryBeanType, bean));
      }
    }
    return types;
  }

  public static Set<PsiType> convertToNonNullTypes(String fqn, @Nullable CommonSpringBean context) {
    return context == null ? Collections.emptySet() : convertToNonNullTypes(Collections.singleton(fqn), context);
  }

  public static Set<PsiType> convertToNonNullTypes(Set<String> names, CommonSpringBean context) {
    PsiElement identifyingPsiElement = context.getIdentifyingPsiElement();
    if (identifyingPsiElement == null || !identifyingPsiElement.isValid()) {
      return Collections.emptySet();
    }
    Project project = identifyingPsiElement.getProject();
    return names.stream().map(s -> {
      if (StringUtil.isEmptyOrSpaces(s)) {
        return null;
      }
      PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(s, identifyingPsiElement.getContainingFile().getResolveScope());
      if (aClass != null) {
        return PsiTypesUtil.getClassType(aClass);
      }
      try {
        return JavaPsiFacade.getElementFactory(project).createTypeFromText(s, identifyingPsiElement);
      }
      catch (IncorrectOperationException e) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet());
  }
}
