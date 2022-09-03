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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.model.xml.DomInfraBeanImpl;

/**
 * Allows registration of custom namespace(s) in Spring DOM.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 17:18
 */
public abstract class InfraCustomNamespaces {

  public static final ExtensionPointName<InfraCustomNamespaces> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.customNamespaces");

  public abstract InfraCustomNamespaces.NamespacePolicies getNamespacePolicies();

  /**
   * Convenience method to register namespace DOM elements.
   * <p/>
   * Alternatively, one can use custom {@link com.intellij.util.xml.reflect.DomExtender}.
   *
   * @param registrar Registrar instance.
   * @see CustomNamespaceRegistrar
   */
  public void registerExtensions(DomExtensionsRegistrar registrar) {

  }

  /**
   * Returns DOM model version for all namespaces registered in this EP.
   * <p/>
   * Must be incremented on any Spring model related change
   * (e.g. changing {@link cn.taketoday.assistant.model.xml.BeanType} or
   * linked {@link cn.taketoday.assistant.model.xml.BeanTypeProvider},
   * custom implementation of {@link DomInfraBeanImpl#getBeanName()})
   * to update all related indexes.
   *
   * @return Model version.
   */
  public int getModelVersion() {
    return 0;
  }

  /**
   * Returns DOM stub version for all namespaces registered in this EP.
   * <p/>
   * Must be incremented on any change of using {@link com.intellij.util.xml.Stubbed} in related DOM.
   *
   * @return 0 if no stubs are used (default).
   * @see com.intellij.util.xml.Stubbed
   */
  public int getStubVersion() {
    return 0;
  }

  /**
   * @see DomFileDescription#registerNamespacePolicy(String, String...)
   */
  public static class NamespacePolicies {
    private final Map<String, List<String>> policies = new HashMap<>();

    public static InfraCustomNamespaces.NamespacePolicies simple(String key, String namespace) {
      InfraCustomNamespaces.NamespacePolicies policies = new InfraCustomNamespaces.NamespacePolicies();
      return policies.add(key, namespace);
    }

    public InfraCustomNamespaces.NamespacePolicies add(String key, String... namespaces) {
      policies.put(key, Arrays.asList(namespaces));
      return this;
    }

    /**
     * Checks whether given namespace is registered with this policy.
     *
     * @return true if registered.
     */
    public boolean isRegistered(String namespace) {
      for (List<String> namespaces : policies.values()) {
        if (namespaces.contains(namespace)) {
          return true;
        }
      }
      return false;
    }

    void process(DomFileDescription domFileDescription) {
      for (Map.Entry<String, List<String>> entry : policies.entrySet()) {
        domFileDescription.registerNamespacePolicy(entry.getKey(), ArrayUtilRt.toStringArray(entry.getValue()));
      }
    }
  }
}
