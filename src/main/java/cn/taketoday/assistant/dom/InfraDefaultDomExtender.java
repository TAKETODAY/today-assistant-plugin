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

package cn.taketoday.assistant.dom;

import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;

import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.model.xml.beans.InfraElementsHolder;
import cn.taketoday.assistant.model.xml.beans.ListOrSet;
import cn.taketoday.assistant.model.xml.cache.Advice;
import cn.taketoday.assistant.model.xml.cache.AnnotationDriven;
import cn.taketoday.assistant.model.xml.context.AnnotationConfig;
import cn.taketoday.assistant.model.xml.context.ComponentScan;
import cn.taketoday.assistant.model.xml.context.Filter;
import cn.taketoday.assistant.model.xml.context.InfraConfigured;
import cn.taketoday.assistant.model.xml.context.LoadTimeWeaver;
import cn.taketoday.assistant.model.xml.context.MbeanExport;
import cn.taketoday.assistant.model.xml.context.MbeanServer;
import cn.taketoday.assistant.model.xml.context.PropertyOverride;
import cn.taketoday.assistant.model.xml.context.PropertyPlaceholder;
import cn.taketoday.assistant.model.xml.jdbc.EmbeddedDatabase;
import cn.taketoday.assistant.model.xml.jdbc.InitializeDatabase;
import cn.taketoday.assistant.model.xml.jee.JndiLookup;
import cn.taketoday.assistant.model.xml.jee.LocalSlsb;
import cn.taketoday.assistant.model.xml.jee.RemoteSlsb;
import cn.taketoday.assistant.model.xml.lang.BeanShellScript;
import cn.taketoday.assistant.model.xml.lang.Defaults;
import cn.taketoday.assistant.model.xml.lang.GroovyScript;
import cn.taketoday.assistant.model.xml.lang.JRubyScript;
import cn.taketoday.assistant.model.xml.task.Executor;
import cn.taketoday.assistant.model.xml.task.ScheduledTasks;
import cn.taketoday.assistant.model.xml.task.Scheduler;
import cn.taketoday.assistant.model.xml.tx.JtaTransactionManager;
import cn.taketoday.assistant.model.xml.util.InfraConstant;
import cn.taketoday.assistant.model.xml.util.PropertyPath;
import cn.taketoday.assistant.model.xml.util.UtilList;
import cn.taketoday.assistant.model.xml.util.UtilMap;
import cn.taketoday.assistant.model.xml.util.UtilProperties;
import cn.taketoday.assistant.model.xml.util.UtilSet;

import static cn.taketoday.assistant.InfraConstant.*;
import static cn.taketoday.assistant.dom.CustomNamespaceRegistrar.*;

public final class InfraDefaultDomExtender {

  public static class BeansExtender extends DomExtender<Beans> {

    public void registerExtensions(Beans element, DomExtensionsRegistrar registrar) {
      registerDefaultBeanExtensions(registrar);
      for (InfraCustomNamespaces customNamespaces : InfraCustomNamespaces.EP_NAME.getExtensions()) {
        customNamespaces.registerExtensions(registrar);
      }
    }
  }

  public static class InfraElementsHolderExtender extends DomExtender<InfraElementsHolder> {

    public void registerExtensions(InfraElementsHolder element, DomExtensionsRegistrar registrar) {
      registerDefaultBeanExtensions(registrar);
    }
  }

  public static class ListOrSetExtender extends DomExtender<ListOrSet> {

    public void registerExtensions(ListOrSet element, DomExtensionsRegistrar registrar) {
      registerDefaultBeanExtensions(registrar);
    }
  }

  private static void registerDefaultBeanExtensions(DomExtensionsRegistrar registrar) {
    create(registrar, UTIL_NAMESPACE_KEY).add("map", UtilMap.class).add("list", UtilList.class)
            .add("set", UtilSet.class)
            .add("properties", UtilProperties.class)
            .add("constant", InfraConstant.class)
            .add("property-path", PropertyPath.class);
    create(registrar, CONTEXT_NAMESPACE_KEY).add("property-placeholder", PropertyPlaceholder.class)
            .add("property-override", PropertyOverride.class)
            .add("load-time-weaver", LoadTimeWeaver.class)
            .add("component-scan", ComponentScan.class)
            .add("filter", Filter.class)
            .add("annotation-config", AnnotationConfig.class)
            .add("infra-configured", InfraConfigured.class)
            .add("mbean-server", MbeanServer.class)
            .add("mbean-export", MbeanExport.class);
    create(registrar, CACHE_NAMESPACE_KEY).add("advice", Advice.class)
            .add("annotation-driven", AnnotationDriven.class);
    create(registrar, JEE_NAMESPACE_KEY)
            .add("jndi-lookup", JndiLookup.class)
            .add("local-slsb", LocalSlsb.class)
            .add("remote-slsb", RemoteSlsb.class);
    create(registrar, LANG_NAMESPACE_KEY)
            .add("defaults", Defaults.class)
            .add("groovy", GroovyScript.class)
            .add("jruby", JRubyScript.class)
            .add("bsh", BeanShellScript.class);
    create(registrar, TASK_NAMESPACE_KEY)
            .add("annotation-driven", AnnotationDriven.class)
            .add("scheduler", Scheduler.class)
            .add("executor", Executor.class)
            .add("scheduled-tasks", ScheduledTasks.class);
    create(registrar, TX_NAMESPACE_KEY)
            .add("advice", Advice.class)
            .add("annotation-driven", AnnotationDriven.class)
            .add("jta-transaction-manager", JtaTransactionManager.class);
    create(registrar, JDBC_NAMESPACE_KEY)
            .add("initialize-database", InitializeDatabase.class)
            .add("embedded-database", EmbeddedDatabase.class);
  }
}
