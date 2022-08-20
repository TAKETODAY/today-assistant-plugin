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

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 20:53
 */
public interface AnnotationConstant {

  String JAVA_SPRING_CONFIGURATION = "cn.taketoday.context.annotation.Configuration";
  String JAVA_SPRING_BEAN = "cn.taketoday.context.annotation.Bean";

  String CONTEXT_IMPORT = "cn.taketoday.context.annotation.Import";
  String CONTEXT_IMPORT_RESOURCE = "cn.taketoday.context.annotation.ImportResource";

  String SCOPE = "cn.taketoday.context.annotation.Scope";

  String PRIMARY = "cn.taketoday.context.annotation.Primary";
  String ORDER = "cn.taketoday.core.annotation.Order";

  String CONDITIONAL = "cn.taketoday.context.annotation.Conditional";
  String CONDITION = "cn.taketoday.context.annotation.Condition";

  // stereotypes
  String COMPONENT = "cn.taketoday.stereotype.Component";
  String CONTROLLER = "cn.taketoday.stereotype.Controller";
  String REPOSITORY = "cn.taketoday.stereotype.Repository";
  String SERVICE = "cn.taketoday.stereotype.Service";

  // autowired annotations
  String AUTOWIRED = "cn.taketoday.beans.factory.annotation.Autowired";
  String VALUE = "cn.taketoday.beans.factory.annotation.Value";

  String PROFILE = "cn.taketoday.context.annotation.Profile";
  String COMPONENT_SCAN = "cn.taketoday.context.annotation.ComponentScan";
  String COMPONENT_SCAN_FILTER = "cn.taketoday.context.annotation.ComponentScan.Filter";
  String COMPONENT_SCAN_FILTER_TYPE = "cn.taketoday.context.annotation.FilterType";
  String ACTIVE_PROFILES = "cn.taketoday.test.context.ActiveProfiles";
  String PROPERTY_SOURCE = "cn.taketoday.context.annotation.PropertySource";
  String PROPERTY_SOURCES = "cn.taketoday.context.annotation.PropertySources";
  String DEPENDS_ON = "cn.taketoday.context.annotation.DependsOn";

  String CONTEXT_DESCRIPTION = "cn.taketoday.context.annotation.Description";

  String EVENT_LISTENER = "cn.taketoday.context.event.EventListener";
  String TRANSACTIONAL_EVENT_LISTENER = "cn.taketoday.transaction.event.TransactionalEventListener";
  String ROOT_OBJECT_CLASS = "cn.taketoday.context.event.EventExpressionRootObject";
  String ALIAS_FOR = "cn.taketoday.core.annotation.AliasFor";

  String COMPONENT_SCANS = "cn.taketoday.context.annotation.ComponentScans";

  String CONFIGURABLE = "cn.taketoday.beans.factory.annotation.Configurable";
  String QUALIFIER = "cn.taketoday.beans.factory.annotation.Qualifier";
  String REQUIRED = "cn.taketoday.beans.factory.annotation.Required";

  String NULLABLE = "cn.taketoday.lang.Nullable";

  String CUSTOM_AUTOWIRE_CONFIGURER_CLASS = "cn.taketoday.beans.factory.annotation.CustomAutowireConfigurer";

  String LOOKUP_ANNOTATION = "cn.taketoday.beans.factory.annotation.Lookup";

  // tests
  String CONTEXT_HIERARCHY = "cn.taketoday.test.context.ContextHierarchy";
  String CONTEXT_CONFIGURATION = "cn.taketoday.test.context.ContextConfiguration";
  String TRANSACTION_CONFIGURATION = "cn.taketoday.test.context.transaction.TransactionConfiguration";
  String TEST_PROPERTY_SOURCE = "cn.taketoday.test.context.TestPropertySource";
  String TEST_SQL = "cn.taketoday.test.context.jdbc.Sql";
  String TEST_SQL_GROUP = "cn.taketoday.test.context.jdbc.SqlGroup";
  String TEST_SQL_CONFIG = "cn.taketoday.test.context.jdbc.SqlConfig";
  String DIRTIES_CONTEXT = "cn.taketoday.test.annotation.DirtiesContext";

  String PLATFORM_TRANSACTION_MANAGER = "cn.taketoday.transaction.PlatformTransactionManager";

  // jmx
  String JMX_MANAGED_OPERATION = "cn.taketoday.jmx.export.annotation.ManagedOperation";
  String JMX_MANAGED_ATTRIBUTE = "cn.taketoday.jmx.export.annotation.ManagedAttribute";

  // scheduling
  String SCHEDULED = "cn.taketoday.scheduling.annotation.Scheduled";
  String SCHEDULES = "cn.taketoday.scheduling.annotation.Schedules";
  String ASYNC = "cn.taketoday.scheduling.annotation.Async";

  // test transactions
  String TEST_BEFORE_TRANSACTION = "cn.taketoday.test.context.transaction.BeforeTransaction";
  String TEST_AFTER_TRANSACTION = "cn.taketoday.test.context.transaction.AfterTransaction";

  String ENABLE_TRANSACTION_MANAGEMENT = "cn.taketoday.transaction.annotation.EnableTransactionManagement";

  String DYNAMIC_PROPERTY_SOURCE = "cn.taketoday.test.context.DynamicPropertySource";

  String JSON_COMPONENT = "cn.taketoday.boot.jackson.JsonComponent";

  // transaction
  String TRANSACTIONAL = "cn.taketoday.transaction.annotation.Transactional";
}
