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

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.spring.contexts.model.LocalModel;
import com.intellij.spring.contexts.model.LocalXmlModel;
import com.intellij.spring.index.SpringXmlBeansIndex;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.SpringModelSearchParameters;
import com.intellij.spring.model.custom.CustomLocalComponentsDiscoverer;
import com.intellij.spring.model.jam.stereotype.CustomSpringComponent;
import com.intellij.spring.model.utils.SpringCommonUtils;
import com.intellij.spring.model.utils.SpringPropertyUtils;
import com.intellij.spring.model.utils.search.SpringBeanSearchParameters;
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition;
import com.intellij.util.CommonProcessors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import cn.taketoday.lang.Nullable;

public class BeansProvider extends CustomLocalComponentsDiscoverer {
  private static final String MAPPER_FACTORY_BEAN = "org.mybatis.spring.mapper.MapperFactoryBean";
  private static final String MAPPER_SCANNER_CONFIGURER = "org.mybatis.spring.mapper.MapperScannerConfigurer";

  @Override
  public Collection<CommonSpringBean> getCustomComponents(LocalModel springModel) {
    System.out.println("getCustomComponents");
    Module module = springModel.getModule();
    if (module == null || !(springModel instanceof LocalXmlModel) || DumbService.isDumb(module.getProject())) {
      return Collections.emptyList();
    }
    Collection<CommonSpringBean> myBatisMappers = new HashSet<>();
    collectMappers((LocalXmlModel) springModel, module, myBatisMappers, MAPPER_FACTORY_BEAN);
    collectMappers((LocalXmlModel) springModel, module, myBatisMappers, MAPPER_SCANNER_CONFIGURER);
    return myBatisMappers;
  }

  public void collectMappers(LocalXmlModel springModel, Module module, Collection<CommonSpringBean> myBatisMappers, String className) {
    VirtualFile configFile;
    PsiClass mapperFactoryBeanClass = SpringCommonUtils.findLibraryClass(module, className);
    if (mapperFactoryBeanClass == null || (configFile = springModel.getConfig().getVirtualFile()) == null) {
      return;
    }
    Project project = module.getProject();
    SpringBeanSearchParameters.BeanClass params = SpringBeanSearchParameters.byClass(project, SpringModelSearchParameters.byClass(mapperFactoryBeanClass));
    params.setVirtualFile(configFile);
    CommonProcessors.CollectProcessor<SpringBeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>();
    SpringXmlBeansIndex.processBeansByClass(params, processor);
    boolean includeTests = ProjectRootsUtil.isInTestSource(configFile, project);
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    for (SpringBeanPointer springBaseBeanPointer : processor.getResults()) {
      processBasePackages(myBatisMappers, facade, scope, springBaseBeanPointer);
      processMarkerInterface(myBatisMappers, facade, scope, springBaseBeanPointer);
      processCustomAnnotations(myBatisMappers, facade, scope, springBaseBeanPointer);
    }
  }

  private static void processMarkerInterface(Collection<CommonSpringBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope, SpringBeanPointer<?> pointer) {
    processMarkerInterface(mappers, facade, scope, getPropertyNameByName(pointer, "markerInterface"));
    processMarkerInterface(mappers, facade, scope, getPropertyNameByName(pointer, "mapperInterface"));
  }

  private static void processMarkerInterface(Collection<CommonSpringBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope,
          @Nullable SpringPropertyDefinition markerInterface) {
    String value;
    PsiClass aClass;
    if (markerInterface != null && (value = markerInterface.getValueAsString()) != null && (aClass = facade.findClass(value, scope)) != null) {
      mappers.add(new CustomSpringComponent(aClass));
      for (PsiClass psiClass : ClassInheritorsSearch.search(aClass, scope, true).findAll()) {
        mappers.add(new CustomSpringComponent(psiClass));
      }
    }
  }

  private static void processCustomAnnotations(Collection<CommonSpringBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope, SpringBeanPointer<?> pointer) {
    String value;
    PsiClass aClass;
    SpringPropertyDefinition annotationClass = getPropertyNameByName(pointer, "annotationClass");
    if (annotationClass != null && (value = annotationClass.getValueAsString()) != null && (aClass = facade.findClass(value, scope)) != null && aClass.isAnnotationType()) {
      for (PsiClass annotatedClass : AnnotatedElementsSearch.searchPsiClasses(aClass, scope).findAll()) {
        mappers.add(new CustomSpringComponent(annotatedClass));
      }
    }
  }

  private static void processBasePackages(final Collection<CommonSpringBean> myBatisMappers, final JavaPsiFacade facade, final GlobalSearchScope scope,
          SpringBeanPointer<?> springBaseBeanPointer) {
    final String value;

    SpringPropertyDefinition basePackages = getPropertyNameByName(springBaseBeanPointer, "basePackage");
    if (basePackages != null && (value = basePackages.getValueAsString()) != null) {
      new DelimitedListProcessor(" ,") {
        protected void processToken(int start, int end, boolean delimitersOnly) {
          String packageName = value.substring(start, end);
          PsiPackage aPackage = facade.findPackage(packageName.trim());
          if (aPackage != null) {
            BeansProvider.processBasePackage(scope, aPackage, myBatisMappers);
          }
        }
      }.processText(value);
    }
  }

  @Nullable
  private static SpringPropertyDefinition getPropertyNameByName(SpringBeanPointer<?> springBaseBeanPointer, String propertyName) {
    for (SpringPropertyDefinition property : SpringPropertyUtils.getProperties(springBaseBeanPointer.getSpringBean())) {
      if (propertyName.equals(property.getPropertyName())) {
        return property;
      }
    }
    return null;
  }

  private static void processBasePackage(GlobalSearchScope scope, PsiPackage aPackage, Collection<CommonSpringBean> myBatisMappers) {
    for (PsiClass aClass : aPackage.getClasses(scope)) {
      if (aClass.isInterface()) {
        myBatisMappers.add(new CustomSpringComponent(aClass));
      }
    }
    for (PsiPackage psiPackage : aPackage.getSubPackages(scope)) {
      processBasePackage(scope, psiPackage, myBatisMappers);
    }
  }
}
