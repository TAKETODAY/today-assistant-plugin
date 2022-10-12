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

package cn.taketoday.assistant.model.extensions.myBatis;

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
import com.intellij.util.CommonProcessors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.index.InfraXmlBeansIndex;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.custom.CustomLocalComponentsDiscoverer;
import cn.taketoday.assistant.model.jam.stereotype.CustomInfraComponent;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.utils.search.BeanSearchParameters;
import cn.taketoday.assistant.model.xml.beans.InfraPropertyDefinition;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraMyBatisBeansProvider extends CustomLocalComponentsDiscoverer {

  private static final String MAPPER_FACTORY_BEAN = "org.mybatis.spring.mapper.MapperFactoryBean";
  private static final String MAPPER_SCANNER_CONFIGURER = "org.mybatis.spring.mapper.MapperScannerConfigurer";

  @Override
  public Collection<CommonInfraBean> getCustomComponents(LocalModel infraModel) {
    Module module = infraModel.getModule();
    if (module == null || !(infraModel instanceof LocalXmlModel) || DumbService.isDumb(module.getProject())) {
      return Collections.emptyList();
    }
    Collection<CommonInfraBean> myBatisMappers = new HashSet<>();
    collectMappers((LocalXmlModel) infraModel, module, myBatisMappers, MAPPER_FACTORY_BEAN);
    collectMappers((LocalXmlModel) infraModel, module, myBatisMappers, MAPPER_SCANNER_CONFIGURER);
    return myBatisMappers;
  }

  public void collectMappers(LocalXmlModel infraModel, Module module, Collection<CommonInfraBean> myBatisMappers, String className) {
    VirtualFile configFile;
    PsiClass mapperFactoryBeanClass = InfraUtils.findLibraryClass(module, className);
    if (mapperFactoryBeanClass == null || (configFile = infraModel.getConfig().getVirtualFile()) == null) {
      return;
    }
    Project project = module.getProject();
    BeanSearchParameters.BeanClass params = BeanSearchParameters.byClass(project, ModelSearchParameters.byClass(mapperFactoryBeanClass));
    params.setVirtualFile(configFile);
    CommonProcessors.CollectProcessor<BeanPointer<?>> processor = new CommonProcessors.CollectProcessor<>();
    InfraXmlBeansIndex.processBeansByClass(params, processor);
    boolean includeTests = ProjectRootsUtil.isInTestSource(configFile, project);
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, includeTests);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    for (BeanPointer springBaseBeanPointer : processor.getResults()) {
      processBasePackages(myBatisMappers, facade, scope, springBaseBeanPointer);
      processMarkerInterface(myBatisMappers, facade, scope, springBaseBeanPointer);
      processCustomAnnotations(myBatisMappers, facade, scope, springBaseBeanPointer);
    }
  }

  private static void processMarkerInterface(Collection<CommonInfraBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope, BeanPointer<?> pointer) {
    processMarkerInterface(mappers, facade, scope, getPropertyNameByName(pointer, "markerInterface"));
    processMarkerInterface(mappers, facade, scope, getPropertyNameByName(pointer, "mapperInterface"));
  }

  private static void processMarkerInterface(Collection<CommonInfraBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope, @Nullable InfraPropertyDefinition markerInterface) {
    String value;
    PsiClass aClass;
    if (markerInterface != null && (value = markerInterface.getValueAsString()) != null && (aClass = facade.findClass(value, scope)) != null) {
      mappers.add(new CustomInfraComponent(aClass));
      for (PsiClass psiClass : ClassInheritorsSearch.search(aClass, scope, true).findAll()) {
        mappers.add(new CustomInfraComponent(psiClass));
      }
    }
  }

  private static void processCustomAnnotations(Collection<CommonInfraBean> mappers, JavaPsiFacade facade, GlobalSearchScope scope, BeanPointer<?> pointer) {
    String value;
    PsiClass aClass;
    InfraPropertyDefinition annotationClass = getPropertyNameByName(pointer, "annotationClass");
    if (annotationClass != null && (value = annotationClass.getValueAsString()) != null && (aClass = facade.findClass(value, scope)) != null && aClass.isAnnotationType()) {
      for (PsiClass annotatedClass : AnnotatedElementsSearch.searchPsiClasses(aClass, scope).findAll()) {
        mappers.add(new CustomInfraComponent(annotatedClass));
      }
    }
  }

  private static void processBasePackages(Collection<CommonInfraBean> myBatisMappers, JavaPsiFacade facade, GlobalSearchScope scope, BeanPointer<?> springBaseBeanPointer) {
    String value;
    InfraPropertyDefinition basePackages = getPropertyNameByName(springBaseBeanPointer, "basePackage");
    if (basePackages != null && (value = basePackages.getValueAsString()) != null) {
      new DelimitedListProcessor(" ,") {
        protected void processToken(int start, int end, boolean delimitersOnly) {
          String packageName = value.substring(start, end);
          PsiPackage aPackage = facade.findPackage(packageName.trim());
          if (aPackage != null) {
            processBasePackage(scope, aPackage, myBatisMappers);
          }
        }
      }.processText(value);
    }
  }

  @Nullable
  private static InfraPropertyDefinition getPropertyNameByName(BeanPointer<?> springBaseBeanPointer, String propertyName) {
    for (InfraPropertyDefinition property : InfraPropertyUtils.getProperties(springBaseBeanPointer.getBean())) {
      if (propertyName.equals(property.getPropertyName())) {
        return property;
      }
    }
    return null;
  }

  private static void processBasePackage(GlobalSearchScope scope, PsiPackage aPackage, Collection<CommonInfraBean> myBatisMappers) {
    for (PsiClass aClass : aPackage.getClasses(scope)) {
      if (aClass.isInterface()) {
        myBatisMappers.add(new CustomInfraComponent(aClass));
      }
    }
    for (PsiPackage psiPackage : aPackage.getSubPackages(scope)) {
      processBasePackage(scope, psiPackage, myBatisMappers);
    }
  }
}
