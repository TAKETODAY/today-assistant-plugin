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

package cn.taketoday.assistant;

import com.intellij.psi.CommonClassNames;

import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 11:47
 */
public interface InfraConstant {

  String AOP_NAMESPACE = "http://www.springframework.org/schema/aop";
  String JEE_NAMESPACE = "http://www.springframework.org/schema/jee";
  String LANG_NAMESPACE = "http://www.springframework.org/schema/lang";
  String TOOL_NAMESPACE = "http://www.springframework.org/schema/tool";
  String TX_NAMESPACE = "http://www.springframework.org/schema/tx";
  String UTIL_NAMESPACE = "http://www.springframework.org/schema/util";
  String CONTEXT_NAMESPACE = "http://www.springframework.org/schema/context";
  String CACHE_NAMESPACE = "http://www.springframework.org/schema/cache";
  String P_NAMESPACE = "http://www.springframework.org/schema/p";
  String C_NAMESPACE = "http://www.springframework.org/schema/c";
  String TASK_NAMESPACE = "http://www.springframework.org/schema/task";
  String JDBC_NAMESPACE = "http://www.springframework.org/schema/jdbc";

  String BEANS_XSD = "http://www.springframework.org/schema/beans";

  String BEANS_DTD_1 = "http://www.springframework.org/dtd/spring-beans.dtd";
  String BEANS_DTD_2 = "http://www.springframework.org/dtd/spring-beans-2.0.dtd";
  String BEANS_NAMESPACE_KEY = "Infra beans namespace key";

  String AOP_NAMESPACE_KEY = "Infra AOP namespace key";
  String JEE_NAMESPACE_KEY = "Infra JEE namespace key";
  String LANG_NAMESPACE_KEY = "Infra Lang namespace key";
  String TX_NAMESPACE_KEY = "Infra TX namespace key";
  String UTIL_NAMESPACE_KEY = "Infra Util namespace key";
  String CONTEXT_NAMESPACE_KEY = "Infra Context namespace key";
  String CACHE_NAMESPACE_KEY = "Infra Cache namespace key";
  String P_NAMESPACE_KEY = "Infra p-namespace";
  String C_NAMESPACE_KEY = "Infra c-namespace";
  String TASK_NAMESPACE_KEY = "Infra task namespace key";
  String JDBC_NAMESPACE_KEY = "Infra JDBC namespace key";

  String INFRA_VERSION_CLASS = "cn.taketoday.core.Version";

  String ASPECTJ_AUTOPROXY = "aspectj-autoproxy";
  String ASPECTJ_AUTOPROXY_BEAN_CLASS = "cn.taketoday.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator";
  String AOP_ALLIANCE_ADVICE_CLASS = "org.aopalliance.aop.Advice";

  String BEAN_FACTORY_CLASS = "cn.taketoday.beans.factory.BeanFactory";
  String OBJECT_FACTORY_CLASS = "java.util.function.Supplier";
  String BEAN_FACTORY_AWARE = "cn.taketoday.beans.factory.BeanFactoryAware";
  String BEAN_FACTORY_POST_PROCESSOR = "cn.taketoday.beans.factory.config.BeanFactoryPostProcessor";
  String CONFIGURABLE_BEAN_FACTORY = "cn.taketoday.beans.factory.config.ConfigurableBeanFactory";
  String METHOD_INVOKING_FACTORY_BEAN_CLASS = "cn.taketoday.beans.factory.config.MethodInvokingFactoryBean";
  String JNDI_OBJECT_FACTORY_BEAN = "cn.taketoday.jndi.JndiObjectFactoryBean";
  String MAP_FACTORY_BEAN = "cn.taketoday.beans.factory.config.MapFactoryBean";
  String BEAN_NAME_GENERATOR = "cn.taketoday.beans.factory.support.BeanNameGenerator";

  String CLASS_PATH_XML_APP_CONTEXT = "cn.taketoday.context.support.ClassPathXmlApplicationContext";
  String IO_RESOURCE = "cn.taketoday.core.io.Resource";
  String CLASS_PATH_RESOURCE = "cn.taketoday.core.io.ClassPathResource";
  String AUTOWIRED_ANNO_POST_PROCESSOR_CLASS = "cn.taketoday.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor";

  String ANNOTATION_CONFIG_APPLICATION_CONTEXT = "cn.taketoday.context.annotation.AnnotationConfigApplicationContext";

  String SCOPE_METADATA_RESOLVER = "cn.taketoday.context.annotation.ScopeMetadataResolver";
  String ANNOTATION_SCOPE_METADATA_RESOLVER = "cn.taketoday.context.annotation.AnnotationScopeMetadataResolver";

  String CUSTOM_EDITOR_CONFIGURER_CLASS = "cn.taketoday.beans.factory.config.CustomEditorConfigurer";
  String CONFIGURABLE_ENVIRONMENT = "cn.taketoday.core.env.ConfigurableEnvironment";
  String ENVIRONMENT_CLASS = "cn.taketoday.core.env.Environment";
  String PROPERTY_RESOLVER_CLASS = "cn.taketoday.core.env.PropertyResolver";
  String CONVERSION_SERVICE_CLASS = "cn.taketoday.core.convert.ConversionService";

  String PROPERTY_OVERRIDE_CONFIGURER = "cn.taketoday.beans.factory.config.PropertyOverrideConfigurer";
  String PROPERTIES_FACTORY_BEAN = "cn.taketoday.beans.factory.config.PropertiesFactoryBean";
  String PROPERTIES_LOADER_SUPPORT = "cn.taketoday.core.io.support.PropertiesLoaderSupport";

  String JAVAX_SQL_DATA_SOURCE = "javax.sql.DataSource";

  String INFRA_UTIL_CONCURRENT_LISTENABLE_FUTURE = "cn.taketoday.util.concurrent.ListenableFuture";
  String JAVA_UTIL_CONCURRENT_COMPLETABLE_FUTURE = CommonClassNames.JAVA_UTIL_CONCURRENT_COMPLETABLE_FUTURE;

  String INFRA_ASYNC_RESULT = "cn.taketoday.scheduling.annotation.AsyncResult";

  // JSR-330
  String JAVAX_INJECT_PROVIDER_CLASS = "javax.inject.Provider";
  String JAKARTA_INJECT_PROVIDER_CLASS = "jakarta.inject.Provider";

  String FACTORY_BEAN = "cn.taketoday.beans.factory.FactoryBean";

  String CACHE_MANAGER = "cn.taketoday.cache.CacheManager";

  String APPLICATION_EVENT_PUBLISHER = "cn.taketoday.context.ApplicationEventPublisher";
  String APPLICATION_EVENT_MULTICASTER = "cn.taketoday.context.event.ApplicationEventMulticaster";
  String APPLICATION_LISTENER = "cn.taketoday.context.ApplicationListener";
  String ON_APPLICATION_EVENT_METHOD = "onApplicationEvent";
  String APPLICATION_EVENT = "cn.taketoday.context.ApplicationEvent";
  String APPLICATION_EVENT_SHORT_NAME = "ApplicationEvent";

  String MESSAGE_SOURCE = "cn.taketoday.context.MessageSource";
  String APPLICATION_CONTEXT = "cn.taketoday.context.ApplicationContext";
  String RESOURCE_LOADER = "cn.taketoday.core.io.ResourceLoader";

  String INJECTION_POINT = "cn.taketoday.beans.factory.InjectionPoint";

  String DATA_SIZE = "cn.taketoday.util.DataSize";
  String DATA_UNIT = "cn.taketoday.util.DataUnit";

  List<String> EVENT_WRAPPER_CLASSES = List.of(
          CommonClassNames.JAVA_UTIL_COLLECTION,
          CommonClassNames.JAVA_UTIL_LIST,
          CommonClassNames.JAVA_UTIL_SET
  );

  // reactive event wrappers, should have single <T> type parameter
  List<String> ASYNC_EVENT_WRAPPER_CLASSES = List.of(
          CommonClassNames.JAVA_UTIL_CONCURRENT_COMPLETION_STAGE,
          JAVA_UTIL_CONCURRENT_COMPLETABLE_FUTURE,
          INFRA_UTIL_CONCURRENT_LISTENABLE_FUTURE,
          "cn.taketoday.util.concurrent.ListenableFutureTask"
  );
}
