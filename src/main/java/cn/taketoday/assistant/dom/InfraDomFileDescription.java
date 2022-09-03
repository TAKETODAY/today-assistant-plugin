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

import com.intellij.util.xml.DomFileDescription;

import java.util.Iterator;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.xml.beans.Beans;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 17:17
 */
public class InfraDomFileDescription extends DomFileDescription<Beans> {

  private static final String[] SPRING_NAMESPACES = {
          "http://www.springframework.org/schema/beans",
          "http://www.springframework.org/dtd/spring-beans.dtd",
          "http://www.springframework.org/dtd/spring-beans-2.0.dtd"
  };

  public InfraDomFileDescription() {
    super(Beans.class, "beans");
  }

  public boolean hasStubs() {
    return true;
  }

  public int getStubVersion() {
    int customNamespacesStubVersion = 0;

    InfraCustomNamespaces customNamespaces;
    for (Iterator<InfraCustomNamespaces> var2 = InfraCustomNamespaces.EP_NAME.getExtensionList().iterator();
         var2.hasNext(); customNamespacesStubVersion += customNamespaces.getClass().getName().hashCode()) {
      customNamespaces = var2.next();
      customNamespacesStubVersion += customNamespaces.getStubVersion();
    }

    return 14 + customNamespacesStubVersion;
  }

  public Icon getFileIcon(int flags) {
    return Icons.SpringConfig;
  }

  protected void initializeFileDescription() {
    registerCoreNamespaces();
    for (InfraCustomNamespaces customNamespaces : InfraCustomNamespaces.EP_NAME.getExtensionList()) {
      customNamespaces.getNamespacePolicies().process(this);
    }

    registerReferenceInjector(new PlaceholderDomReferenceInjector());
  }

  private void registerCoreNamespaces() {
    registerNamespacePolicy(InfraConstant.BEANS_NAMESPACE_KEY, SPRING_NAMESPACES);
    registerNamespacePolicy(InfraConstant.JEE_NAMESPACE_KEY, InfraConstant.JEE_NAMESPACE);
    registerNamespacePolicy(InfraConstant.LANG_NAMESPACE_KEY, InfraConstant.LANG_NAMESPACE);
    registerNamespacePolicy(InfraConstant.UTIL_NAMESPACE_KEY, InfraConstant.UTIL_NAMESPACE);
    registerNamespacePolicy(InfraConstant.CONTEXT_NAMESPACE_KEY, InfraConstant.CONTEXT_NAMESPACE);
    registerNamespacePolicy(InfraConstant.CACHE_NAMESPACE_KEY, InfraConstant.CACHE_NAMESPACE);
    registerNamespacePolicy(InfraConstant.P_NAMESPACE_KEY, InfraConstant.P_NAMESPACE);
    registerNamespacePolicy(InfraConstant.C_NAMESPACE_KEY, InfraConstant.C_NAMESPACE);
    registerNamespacePolicy(InfraConstant.TASK_NAMESPACE_KEY, InfraConstant.TASK_NAMESPACE);
    registerNamespacePolicy(InfraConstant.JDBC_NAMESPACE_KEY, InfraConstant.JDBC_NAMESPACE);
    registerNamespacePolicy(InfraConstant.TX_NAMESPACE_KEY, InfraConstant.TX_NAMESPACE);

  }
}

