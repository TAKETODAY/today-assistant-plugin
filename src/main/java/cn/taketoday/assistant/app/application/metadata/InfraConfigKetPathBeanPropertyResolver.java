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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.microservices.jvm.config.ConfigKeyPathBeanPropertyReference;
import com.intellij.microservices.jvm.config.ConfigKeyPathBeanPropertyResolver;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.RelaxedNames;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.BeanProperty;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.app.InfraClassesConstants;
import cn.taketoday.lang.Nullable;

public class InfraConfigKetPathBeanPropertyResolver implements ConfigKeyPathBeanPropertyResolver {
  private final Module myModule;

  public InfraConfigKetPathBeanPropertyResolver(Module module) {
    this.myModule = module;
  }

  @Nullable
  public PsiElement resolveProperty(MetaConfigKey configKey, PsiType psiType, String propertyName) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
    if (psiClass == null) {
      return null;
    }
    PsiMethod constructor = getBindingConstructor(psiClass, this.myModule, configKey);
    if (constructor == null) {
      return DEFAULT_RESOLVER.resolveProperty(configKey, psiType, propertyName);
    }
    for (PsiParameter psiParameter : constructor.getParameterList().getParameters()) {
      String name = psiParameter.getName();
      if (name.equals(propertyName) || name.equals(RelaxedNames.separatedPropertyNameToCamelCase(propertyName))) {
        return psiParameter;
      }
    }
    return null;
  }

  public List<BeanProperty> getAllBeanProperties(MetaConfigKey configKey, PsiType psiType) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiType);
    if (psiClass == null || ConfigKeyPathBeanPropertyReference.stopResolvingProperty(psiClass)) {
      return Collections.emptyList();
    }
    PsiMethod constructor = getBindingConstructor(psiClass, this.myModule, configKey);
    if (constructor == null) {
      return DEFAULT_RESOLVER.getAllBeanProperties(configKey, psiType);
    }
    return ContainerUtil.map(constructor.getParameterList().getParameters(),
            parameter -> new ParameterBeanProperty(constructor, parameter));
  }

  @Nullable
  public static PsiMethod getBindingConstructor(PsiClass psiClass, @Nullable Module module, @Nullable MetaConfigKey configKey) {
    boolean version3 = true;
    if (!version3) {
      if (configKey != null) {
        hasConfigurationPropertiesConstructorBinding(configKey);
      }
    }
    Supplier<List<String>> excludedAnnotations = () -> ContainerUtil.mapNotNull(
            MetaAnnotationUtil.getAnnotationTypesWithChildren(module, AnnotationConstant.AUTOWIRED, false),
            PsiClass::getQualifiedName
    );
    return getBindingConstructor(psiClass, true, excludedAnnotations);
  }

  private static boolean hasConfigurationPropertiesConstructorBinding(MetaConfigKey configKey) {
    if (configKey.getDeclarationResolveResult() != MetaConfigKey.DeclarationResolveResult.PROPERTY) {
      return false;
    }
    PsiElement declaration = configKey.getDeclaration();
    if (!(declaration instanceof InfraConfigKeyDeclarationPsiElement)) {
      return false;
    }
    PsiElement parent = declaration.getParent();
    return (parent instanceof PsiClass psiClass) && getBindingConstructor(psiClass, false, null) != null;
  }

  @Nullable
  private static PsiMethod getBindingConstructor(PsiClass psiClass, boolean keyBinding, Supplier<List<String>> excludedAnnotations) {
    if (!keyBinding && !psiClass.isRecord() && getClassBinding(psiClass) == null) {
      return getBindingConstructor(getConstructorsWithParameters(psiClass));
    }
    PsiMethod[] constructors = psiClass.getConstructors();
    if (constructors.length == 1
            && constructors[0].hasParameters()
            && (excludedAnnotations == null || !AnnotationUtil.isAnnotated(constructors[0], excludedAnnotations.get(), 0))) {
      return constructors[0];
    }
    return getBindingConstructor(getConstructorsWithParameters(psiClass));
  }

  private static PsiAnnotation getClassBinding(PsiClass psiClass) {
    PsiAnnotation psiAnnotation = null;
    while (psiAnnotation == null && psiClass != null) {
      psiAnnotation = AnnotationUtil.findAnnotation(psiClass, true, InfraClassesConstants.CONSTRUCTOR_BINDING);
      psiClass = psiClass.getContainingClass();
    }
    return psiAnnotation;
  }

  private static List<PsiMethod> getConstructorsWithParameters(PsiClass psiClass) {
    return ContainerUtil.filter(psiClass.getConstructors(), PsiMethod::hasParameters);
  }

  private static PsiMethod getBindingConstructor(List<PsiMethod> constructors) {
    for (PsiMethod constructor : constructors) {
      PsiAnnotation annotation = AnnotationUtil.findAnnotation(constructor, true, InfraClassesConstants.CONSTRUCTOR_BINDING);
      if (annotation != null) {
        return constructor;
      }
    }
    return null;
  }

  private static class ParameterBeanProperty extends BeanProperty {
    private final PsiParameter myParameter;

    protected ParameterBeanProperty(PsiMethod method, PsiParameter parameter) {
      super(method);
      this.myParameter = parameter;
    }

    public PsiNamedElement getPsiElement() {
      return this.myParameter;
    }

    public String getName() {
      return this.myParameter.getName();
    }

    public PsiType getPropertyType() {
      return this.myParameter.getType();
    }
  }
}
