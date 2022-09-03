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
package cn.taketoday.assistant.model.xml;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.xml.DomJavaUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.converters.values.ClassValueConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.factories.CustomFactoryMethodTypeHandler;
import cn.taketoday.assistant.factories.FactoryBeansManager;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.InfraProfile;
import cn.taketoday.assistant.model.InfraQualifier;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;
import cn.taketoday.assistant.model.xml.beans.ConstructorArgumentValues;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.InfraDomProfile;
import cn.taketoday.lang.Nullable;

import static com.intellij.psi.util.CachedValueProvider.Result;

/**
 * @author peter
 */
public abstract class AbstractDomInfraBean implements CommonInfraBean {
  private CachedValue<PsiClass> myClassCachedValue;
  private CachedValue<PsiClass> myFactoriesClassCachedValue;

  @Nullable
  @Override
  @NameValue
  public abstract String getBeanName();

  @Nullable
  public GenericValue<PsiMethod> getFactoryMethod() {
    return null;
  }

  @Nullable
  public GenericValue<BeanPointer<?>> getFactoryBean() {
    return null;
  }

  @Nullable
  public abstract String getClassName();

  @Override
  @Nullable
  public final PsiType getBeanType() {
    final PsiClass beanClass = getBeanClass(null, true);
    return beanClass != null ? PsiTypesUtil.getClassType(beanClass) : null;
  }

  @Override
  @Nullable
  public PsiType getBeanType(boolean considerFactories) {
    final PsiClass beanClass = getBeanClass(null, considerFactories);
    return beanClass != null ? PsiTypesUtil.getClassType(beanClass) : null;
  }

  @Override
  @Nullable
  public abstract Module getModule();

  @Override

  public abstract PsiFile getContainingFile();

  @Nullable
  public PsiClass getBeanClass(@Nullable Set<AbstractDomInfraBean> visited, boolean considerFactories) {
    if (visited != null && visited.contains(this))
      return null;

    if (considerFactories) {
      if (myFactoriesClassCachedValue == null) {
        final Project project = getContainingFile().getProject();
        myFactoriesClassCachedValue = CachedValuesManager.getManager(project)
                .createCachedValue(() -> {
                  PsiClass beanClass = calculateFactoriesBeanClass(this);
                  return Result.create(beanClass, PsiModificationTracker.MODIFICATION_COUNT, ProjectRootManager.getInstance(project));
                });
      }
      return myFactoriesClassCachedValue.getValue();
    }

    return getClassAttributeValue();
  }

  public PsiClass getClassAttributeValue() {
    if (myClassCachedValue == null) {
      final Project project = getContainingFile().getProject();
      myClassCachedValue = CachedValuesManager.getManager(project)
              .createCachedValue(() -> {
                PsiClass beanClass = calculateBeanClass(this);
                return Result.create(beanClass, PsiModificationTracker.MODIFICATION_COUNT, ProjectRootManager.getInstance(project));
              });
    }
    return myClassCachedValue.getValue();
  }

  @Nullable
  private static PsiClass calculateFactoriesBeanClass(AbstractDomInfraBean springBean) {
    final GenericValue<PsiMethod> value = springBean.getFactoryMethod();
    final PsiMethod factoryMethod = value == null ? null : value.getValue();
    if (factoryMethod == null) {
      return calculateBeanClass(springBean);
    }

    final GenericValue<BeanPointer<?>> factoryBean = springBean.getFactoryBean();
    if (!FactoryBeansManager.of()
            .isValidFactoryMethod(factoryMethod, factoryBean != null && factoryBean.getValue() != null)) {
      return calculateBeanClass(springBean);
    }

    for (CustomFactoryMethodTypeHandler typeHandler : CustomFactoryMethodTypeHandler.EP_NAME.getExtensions()) {
      PsiType factoryMethodType = typeHandler.getFactoryMethodType(factoryMethod, springBean);
      if (factoryMethodType instanceof PsiClassType)
        return ((PsiClassType) factoryMethodType).resolve();
    }

    final PsiType returnType = factoryMethod.getReturnType();
    if (returnType instanceof PsiClassType) {
      PsiTypeParameter[] typeParameters = factoryMethod.getTypeParameters();
      PsiClass resolve = ((PsiClassType) returnType).resolve();
      if (resolve == null)
        return null;

      if (!(springBean instanceof InfraBean))
        return resolve;

      for (PsiTypeParameter typeParameter : typeParameters) {
        if (typeParameter.equals(resolve)) {
          // trying to find Class<T> parameter
          String text = "java.lang.Class<" + typeParameter.getName() + ">";
          PsiParameter[] parameters = factoryMethod.getParameterList().getParameters();
          for (int i = 0, length = parameters.length; i < length; i++) {
            final PsiParameter psiParameter = parameters[i];
            PsiType type = psiParameter.getType();
            if (type.getCanonicalText().equals(text)) {
              ConstructorArgumentValues values = new ConstructorArgumentValues();
              values.init((InfraBean) springBean);
              ConstructorArgDefinition definition = values.resolve(i, psiParameter, null);
              if (definition != null) {
                final GenericDomValue<?> valueElement = definition.getValueElement();
                String rawText = valueElement == null ? null : valueElement.getRawText();
                if (rawText != null) {
                  return DomJavaUtil.findClass(rawText, springBean.getContainingFile(), springBean.getModule(), null);
                }
              }
            }
          }
          return resolve;
        }
      }
      return resolve;
    }

    return calculateBeanClass(springBean);
  }

  @Nullable
  private static PsiClass calculateBeanClass(AbstractDomInfraBean springBean) {
    String className = springBean.getClassName();
    if (className == null) {
      return null;
    }

    final PsiFile containingFile = springBean.getContainingFile();
    final GlobalSearchScope scope = ClassValueConverter.getScope(containingFile.getProject(), springBean.getModule(), containingFile);
    return DomJavaUtil.findClass(className.trim(), containingFile, null, scope);
  }

  @Override

  public Collection<InfraQualifier> getInfraQualifiers() {
    return Collections.emptySet();
  }

  @Override
  public InfraProfile getProfile() {
    final Beans beans = getBeansParent();

    if (beans != null) {
      final InfraDomProfile profile = beans.getProfile();

      if (DomUtil.hasXml(profile))
        return profile;
    }
    return InfraProfile.DEFAULT;
  }

  @Nullable
  public abstract Beans getBeansParent();

  @Override
  public boolean isPrimary() {
    return false;
  }
}
