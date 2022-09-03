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

package cn.taketoday.assistant.web.mvc.config.webXml;

import com.intellij.javaee.model.converters.ContextParamsProvider;
import com.intellij.javaee.model.xml.Listener;
import com.intellij.javaee.model.xml.ParamValue;
import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.Filter;
import com.intellij.javaee.web.model.xml.Servlet;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.PsiClassConverter;
import com.intellij.util.xml.converters.values.ClassArrayConverter;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.model.converters.InfraBeanResolveConverterForDefiniteClasses;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public class WebContextParamsProvider extends ContextParamsProvider {
  private static final String CONTEXT_INITIALIZER_CLASSES = "contextInitializerClasses";
  private static final Set<String> SERVLET_PARAM_NAMES = ContainerUtil.immutableSet(
          InfraMvcConstant.CONTEXT_CONFIG_LOCATION, InfraMvcConstant.CONTEXT_CLASS_PARAM_NAME, CONTEXT_INITIALIZER_CLASSES, "namespace");
  private static final String WEB_APP_ROOT_KEY = "webAppRootKey";
  private static final Set<String> INIT_PARAM_NAMES = ContainerUtil.immutableSet(InfraMvcConstant.CONTEXT_CONFIG_LOCATION, WEB_APP_ROOT_KEY);
  private static final Set<String> INIT_PARAM_NAMES_WITH_CONTEXT_LOADER = ContainerUtil.immutableSet(
          InfraMvcConstant.CONTEXT_CONFIG_LOCATION, CONTEXT_INITIALIZER_CLASSES, WEB_APP_ROOT_KEY);
  private static final InfraBeanResolveConverterForDefiniteClasses SPRING_BEAN_FILTER_CONVERTER = new InfraBeanResolveConverterForDefiniteClasses() {
    @Nullable
    protected String[] getClassNames(ConvertContext context) {
      return WebCommonClassNames.SERVLET_FILTER.all();
    }

    @Nullable
    protected CommonInfraModel getSpringModel(ConvertContext context) {
      Module module = context.getModule();
      if (module == null) {
        return null;
      }
      return InfraManager.from(module.getProject()).getCombinedModel(module);
    }
  };

  public Set<String> getContextParamNames(Module module, ConvertContext convertContext) {
    if (!isRelevantModule(module)) {
      return Collections.emptySet();
    }
    else if (isFrameworkServlet(convertContext)) {
      return SERVLET_PARAM_NAMES;
    }
    else if (hasContextLoaderListener(convertContext)) {
      return INIT_PARAM_NAMES_WITH_CONTEXT_LOADER;
    }
    else {
      return INIT_PARAM_NAMES;
    }
  }

  private static boolean isRelevantModule(Module module) {
    return (InfraUtils.hasFacet(module) || InfraModelService.of().hasAutoConfiguredModels(module)) && !WebFacet.getInstances(module)
            .isEmpty() && InfraUtils.findLibraryClass(module, InfraMvcConstant.CONTEXT_LISTENER_CLASS) != null;
  }

  private static boolean isFrameworkServlet(ConvertContext convertContext) {
    Servlet servlet = convertContext.getInvocationElement().getParentOfType(Servlet.class, true);
    return servlet != null && InheritanceUtil.isInheritor(servlet.getServletClass().getValue(), "cn.taketoday.web.servlet.DispatcherServlet");
  }

  private static boolean hasContextLoaderListener(ConvertContext convertContext) {
    WebApp webApp = convertContext.getInvocationElement().getParentOfType(WebApp.class, true);
    if (webApp == null) {
      return false;
    }
    for (Listener listener : webApp.getListeners()) {
      if (InheritanceUtil.isInheritor(listener.getListenerClass().getValue(), "cn.taketoday.web.context.ContextLoader")) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public Converter getContextParamValueConverter(@Nullable Module module, ParamValue paramValue, String paramName) {
    if (InfraMvcConstant.CONTEXT_CLASS_PARAM_NAME.equals(paramName)) {
      return createClassConverter(InfraMvcConstant.CONFIGURABLE_WEB_APPLICATION_CONTEXT_CLASS);
    }
    if (CONTEXT_INITIALIZER_CLASSES.equals(paramName)) {
      return ClassArrayConverter.getClassArrayConverter();
    }
    if ("targetBeanName".equals(paramName) && isInDelegatingFilterProxy(paramValue)) {
      return SPRING_BEAN_FILTER_CONVERTER;
    }
    return null;
  }

  private static boolean isInDelegatingFilterProxy(ParamValue paramValue) {
    Filter filter = paramValue.getParentOfType(Filter.class, true);
    if (filter == null) {
      return false;
    }
    return InheritanceUtil.isInheritor(filter.getFilterClass().getValue(), InfraMvcConstant.DELEGATING_FILTER_PROXY);
  }

  private static Converter createClassConverter(String... extendClasses) {
    return new PsiClassConverter() {
      protected JavaClassReferenceProvider createClassReferenceProvider(GenericDomValue<PsiClass> genericDomValue, ConvertContext context, ExtendClass extendClass) {
        JavaClassReferenceProvider provider = super.createClassReferenceProvider(genericDomValue, context, extendClass);
        provider.setOption(JavaClassReferenceProvider.SUPER_CLASSES, ContainerUtil.immutableList(extendClasses));
        provider.setOption(JavaClassReferenceProvider.INSTANTIATABLE, Boolean.TRUE);
        provider.setOption(JavaClassReferenceProvider.CONCRETE, Boolean.TRUE);
        return provider;
      }
    };
  }
}
