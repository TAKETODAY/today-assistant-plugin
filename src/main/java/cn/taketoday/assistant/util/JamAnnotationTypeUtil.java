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

package cn.taketoday.assistant.util;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.spring.SpringManager;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.utils.SpringModelSearchers;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.xml.beans.SpringProperty;
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.beans.SemContributorUtil;

public abstract class JamAnnotationTypeUtil {

  public static List<PsiClass> getQualifierAnnotationTypesWithChildren(Module module) {
    SmartList<PsiClass> smartList = new SmartList<>();
    smartList.addAll(getAnnotationTypesWithChildren(module, AnnotationConstant.QUALIFIER));
    smartList.addAll(getAnnotationTypesWithChildren(module, JavaeeConstant.JAVAX_INJECT_QUALIFIER));
    smartList.addAll(getImplicitQualifierAnnotations(module));
    return smartList;
  }

  private static Set<PsiClass> getImplicitQualifierAnnotations(Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      PsiClass customAutowireConfigurerClass = CommonUtils.findLibraryClass(module, AnnotationConstant.CUSTOM_AUTOWIRE_CONFIGURER_CLASS);
      if (customAutowireConfigurerClass == null) {
        return CachedValueProvider.Result.create(Collections.emptySet(), new Object[] { PsiModificationTracker.MODIFICATION_COUNT });
      }
      GlobalSearchScope moduleSearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      SpringModelSearchParameters.BeanClass searchParameters = SpringModelSearchParameters.byClass(customAutowireConfigurerClass).withInheritors();
      JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
      Set<PsiClass> types = new HashSet<>();
      PsiClass psiClass;
      for (SpringModel model : SpringManager.getInstance(module.getProject()).getAllModels(module)) {
        List<SpringBeanPointer<?>> beanPointers = SpringModelSearchers.findBeans(model, searchParameters);
        for (SpringBeanPointer beanPointer : beanPointers) {
          CommonSpringBean bean = beanPointer.getSpringBean();
          SpringPropertyDefinition propertyDefinition = SpringPropertyUtils.findPropertyByName(bean, "customQualifierTypes");
          if (propertyDefinition instanceof SpringProperty) {
            for (String value : SpringPropertyUtils.getListOrSetValues((SpringProperty) propertyDefinition)) {
              if (!StringUtil.isEmptyOrSpaces(value)
                      && (psiClass = facade.findClass(value, moduleSearchScope)) != null && psiClass.isAnnotationType()) {
                types.add(psiClass);
              }
            }
          }
        }
      }
      return CachedValueProvider.Result.create(types, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static List<String> getUserDefinedCustomComponentAnnotations(Module module) {
    List<String> annotations = getCustomComponentAnnotations(module);
    for (String defaultAnno : SemContributorUtil.DEFAULT_COMPONENTS) {
      if (defaultAnno.equals(AnnotationConstant.COMPONENT)) {
        annotations.remove(defaultAnno);
      }
      else {
        for (PsiClass annoClass : getAnnotationTypesWithChildren(module, defaultAnno)) {
          String qualifiedName = annoClass.getQualifiedName();
          if (qualifiedName != null) {
            annotations.remove(qualifiedName);
          }
        }
      }
    }
    return annotations;
  }

  private static List<String> getCustomComponentAnnotations(Module module) {
    Collection<PsiClass> classes = getAnnotationTypesWithChildrenIncludingTests(module, AnnotationConstant.COMPONENT);
    return ContainerUtil.mapNotNull(classes, PsiClass::getQualifiedName);
  }

  public static Collection<PsiClass> getAnnotationTypesWithChildren(Module module, String annotationName) {
    return getAnnotationTypes(module, annotationName, false);
  }

  public static Collection<PsiClass> getAnnotationTypesWithChildrenIncludingTests(Module module, String annotationName) {
    return getAnnotationTypes(module, annotationName, true);
  }

  private static Collection<PsiClass> getAnnotationTypes(Module myModule, String annotationName, boolean inTests) {
    return myModule.isDisposed()
           ? Collections.emptyList()
           : MetaAnnotationUtil.getAnnotationTypesWithChildren(myModule, annotationName, inTests);
  }
}
