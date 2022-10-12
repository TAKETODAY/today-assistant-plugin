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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;

import org.jetbrains.uast.UCallExpression;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraPropertyUtils;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class TemplateViewResolverFactory extends ViewResolverFactory {
  private final String myBeanClass;
  private final String myConfigurerClass;
  private final String myLoaderProperty;
  private final String myDefaultSuffix;

  protected TemplateViewResolverFactory(String beanClass, String configurerClass, String loaderProperty, String defaultSuffix) {
    this.myBeanClass = beanClass;
    this.myConfigurerClass = configurerClass;
    this.myLoaderProperty = loaderProperty;
    this.myDefaultSuffix = defaultSuffix;
  }

  @Override
  protected String getBeanClass() {
    return this.myBeanClass;
  }

  @Override

  protected Set<ViewResolver> createViewResolvers(@Nullable CommonInfraBean bean, CommonInfraModel model) {
    Module module = model.getModule() != null ? model.getModule() : bean != null ? bean.getModule() : null;
    if (module == null) {
      return Collections.emptySet();
    }
    String prefix = InfraPropertyUtils.getPropertyStringValue(bean, "prefix");
    String suffix = InfraPropertyUtils.getPropertyStringValue(bean, "suffix");
    WithPrefixSuffix resolver = new UrlBasedViewResolver(module, getClass().getName(), "", prefix, suffix);
    PsiClass configurerClass = InfraUtils.findLibraryClass(module, this.myConfigurerClass);
    if (configurerClass == null) {
      return Collections.singleton(resolver);
    }
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(configurerClass).withInheritors().effectiveBeanTypes();
    List<BeanPointer<?>> configurers = InfraModelSearchers.findBeans(model, searchParameters);
    if (configurers.isEmpty()) {
      return Collections.singleton(resolver);
    }
    CommonInfraBean configurer = configurers.get(0).getBean();
    if ((configurer instanceof InfraBean) || (configurer instanceof ContextJavaBean)) {
      String value = InfraPropertyUtils.getPropertyStringValue(configurer, this.myLoaderProperty);
      if (value != null) {
        WithPrefixSuffix viewResolver = new UrlBasedViewResolver(module, getClass().getName(), value, resolver.getPrefix(),
                resolver.getSuffix());
        return Collections.singleton(viewResolver);
      }
      return Collections.singleton(resolver);
    }
    return Collections.singleton(handleCustomConfigurer(configurer, resolver));
  }

  protected ViewResolver handleCustomConfigurer(CommonInfraBean configurer, WithPrefixSuffix resolver) {
    return resolver;
  }

  @Override
  public Set<ViewResolver> handleResolversRegistry(String methodName, UCallExpression methodCallExpression, CommonInfraModel servletModel) {
    if (getViewResolverRegistryMethodName().equals(methodName)) {
      return getViewResolverRegistryViewResolvers(methodCallExpression, servletModel);
    }
    return super.handleResolversRegistry(methodName, methodCallExpression, servletModel);
  }

  protected String getViewResolverRegistryMethodName() {
    return "";
  }

  private Set<ViewResolver> getViewResolverRegistryViewResolvers(UCallExpression expression, CommonInfraModel infraModel) {
    Module module = infraModel.getModule();
    if (module == null) {
      return Collections.emptySet();
    }
    PsiClass configurerClass = InfraUtils.findLibraryClass(module, this.myConfigurerClass);
    if (configurerClass == null) {
      return Collections.emptySet();
    }
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(configurerClass).withInheritors().effectiveBeanTypes();
    List<BeanPointer<?>> configurers = InfraModelSearchers.findBeans(infraModel, searchParameters);
    if (configurers.size() != 1) {
      return Collections.emptySet();
    }
    CommonInfraBean configurer = configurers.get(0).getBean();
    String prefix = InfraPropertyUtils.getPropertyStringValue(configurer, this.myLoaderProperty);
    WithPrefixSuffix viewResolver = new UrlBasedViewResolver(module, configurer.getBeanName() + ".handleViewResolverRegistry()", "", prefix, this.myDefaultSuffix);
    return Collections.singleton(viewResolver);
  }
}
