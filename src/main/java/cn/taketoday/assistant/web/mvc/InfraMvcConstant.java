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

package cn.taketoday.assistant.web.mvc;

import com.intellij.psi.ReferenceProviderType;

public interface InfraMvcConstant {

  String WEB_INF = "/WEB-INF/";
  String MVC_FORM_TLD = "http://www.springframework.org/tags/form";

  String MVC_SPRING_TLD = "http://www.springframework.org/tags";

  String MVC_NAMESPACE = "http://www.springframework.org/schema/mvc";

  String MVC_NAMESPACE_KEY = "Infra MVC namespace key";

  String CONFIGURABLE_WEB_APPLICATION_CONTEXT_CLASS = "cn.taketoday.web.context.ConfigurableWebApplicationContext";

  String ANNOTATION_CONFIG_CLASS = "cn.taketoday.web.context.support.AnnotationConfigWebApplicationContext";

  String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

  String CONTEXT_CLASS_PARAM_NAME = "contextClass";

  String APPLICATION_CONTEXT_XML = "applicationContext.xml";

  String DISPATCHER_SERVLET_CLASS = "cn.taketoday.web.servlet.DispatcherServlet";

  String CONTEXT_LISTENER_CLASS = "cn.taketoday.web.context.ContextLoaderListener";

  String DELEGATING_FILTER_PROXY = "cn.taketoday.web.filter.DelegatingFilterProxy";

  String WEB_APPLICATION_INITIALIZER = "cn.taketoday.web.WebApplicationInitializer";

  String WEB_MVC_CONFIGURATION_SUPPORT = "cn.taketoday.web.servlet.config.annotation.WebMvcConfigurationSupport";

  String DELEGATING_WEB_MVC_CONFIGURATION = "cn.taketoday.web.servlet.config.annotation.DelegatingWebMvcConfiguration";

  String WEB_MVC_CONFIGURER = "cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer";

  String VIEW_RESOLVER_REGISTRY = "cn.taketoday.web.servlet.config.annotation.ViewResolverRegistry";

  String VIEW_CONTROLLER_REGISTRY = "cn.taketoday.web.servlet.config.annotation.ViewControllerRegistry";

  String VIEW_CONTROLLER_REGISTRATION = "cn.taketoday.web.servlet.config.annotation.ViewControllerRegistration";

  String SIMPLE_URL_HANDLER_MAPPING = "cn.taketoday.web.servlet.handler.SimpleUrlHandlerMapping";

  String SERVLET_MVC_CONTROLLER = "cn.taketoday.web.servlet.mvc.Controller";

  String PARAMETERIZABLE_VIEW_CONTROLLER = "cn.taketoday.web.servlet.mvc.ParameterizableViewController";

  String VIEW = "cn.taketoday.web.servlet.View";

  String REQUEST_CONTEXT = "cn.taketoday.web.servlet.support.RequestContext";

  String MODEL_AND_VIEW = "cn.taketoday.web.servlet.ModelAndView";

  String REQUEST_MAPPING = "cn.taketoday.web.annotation.ActionMapping";

  String PATH_VARIABLE = "cn.taketoday.web.annotation.PathVariable";

  String REQUEST_PARAM = "cn.taketoday.web.annotation.RequestParam";

  String REQUEST_BODY = "cn.taketoday.web.annotation.RequestBody";

  String REQUEST_PART = "cn.taketoday.web.annotation.RequestPart";

  String COOKIE_VALUE = "cn.taketoday.web.annotation.CookieValue";

  String MODEL_ATTRIBUTE = "cn.taketoday.web.annotation.ModelAttribute";

  String REQUEST_HEADER = "cn.taketoday.web.annotation.RequestHeader";

  String REQUEST_ATTRIBUTE = "cn.taketoday.web.annotation.RequestAttribute";

  String RESPONSE_BODY = "cn.taketoday.web.annotation.ResponseBody";

  String MATRIX_VARIABLE = "cn.taketoday.web.annotation.MatrixVariable";

  String RESPONSE_STATUS = "cn.taketoday.web.annotation.ResponseStatus";

  String ASYNC_HANDLER_INTERCEPTOR = "cn.taketoday.web.servlet.AsyncHandlerInterceptor";

  String INIT_BINDER = "cn.taketoday.web.annotation.InitBinder";

  String SESSION_ATTRIBUTES = "cn.taketoday.web.annotation.SessionAttributes";

  String SESSION_ATTRIBUTE = "cn.taketoday.web.annotation.SessionAttribute";

  String EXCEPTION_HANDLER = "cn.taketoday.web.annotation.ExceptionHandler";

  String PATH_MATCHER = "cn.taketoday.util.PathMatcher";

  String MOCK_MVC_REQUEST_BUILDERS = "cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders";

  String MOCK_MVC = "cn.taketoday.test.web.servlet.MockMvc";

  String REST_OPERATIONS = "cn.taketoday.web.client.RestOperations";

  String REST_TEMPLATE = "cn.taketoday.web.client.RestTemplate";

  String TEST_REST_TEMPLATE = "cn.taketoday.boot.test.web.client.TestRestTemplate";

  String ASYNC_REST_OPERATIONS = "cn.taketoday.web.client.AsyncRestOperations";

  String TILES_3_CONFIGURER = "cn.taketoday.web.servlet.view.tiles3.TilesConfigurer";

  String TILES_3_VIEW_RESOLVER_CLASS = "cn.taketoday.web.servlet.view.tiles3.TilesViewResolver";

  String FREEMARKER_CONFIGURER = "cn.taketoday.web.servlet.view.freemarker.FreeMarkerConfigurer";

  String FREEMARKER_VIEW_RESOLVER = "cn.taketoday.web.servlet.view.freemarker.FreeMarkerViewResolver";

  String FREEMARKER_REACTIVE_CONFIGURER = "cn.taketoday.web.reactive.result.view.freemarker.FreeMarkerConfigurer";

  String VELOCITY_CONFIGURER = "cn.taketoday.web.servlet.view.velocity.VelocityConfigurer";

  String VELOCITY_VIEW_RESOLVER = "cn.taketoday.web.servlet.view.velocity.VelocityViewResolver";

  String VIEW_RESOLVER = "cn.taketoday.web.servlet.ViewResolver";

  String REACTIVE_VIEW_RESOLVER = "cn.taketoday.web.reactive.result.view.ViewResolver";

  String INTERNAL_RESOURCE_VIEW_RESOLVER = "cn.taketoday.web.servlet.view.InternalResourceViewResolver";

  String VIEW_RESOLVER_COMPOSITE = "cn.taketoday.web.servlet.view.ViewResolverComposite";

  String URL_BASED_VIEW_RESOLVER = "cn.taketoday.web.servlet.view.UrlBasedViewResolver";

  String GROOVY_MARKUP_CONFIGURER = "cn.taketoday.web.servlet.view.groovy.GroovyMarkupConfigurer";

  String GROOVY_VIEW_RESOLVER = "cn.taketoday.web.servlet.view.groovy.GroovyMarkupViewResolver";
  ReferenceProviderType MVC_VIEW_PROVIDER = new ReferenceProviderType("Infra MVC View");

  String MOCK_HTTP_SERVLET_REQUEST_BUILDER = "cn.taketoday.test.web.servlet.request.MockHttpServletRequestBuilder";
}
