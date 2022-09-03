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

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.xml.beans.CNamespaceRefValue;
import cn.taketoday.assistant.model.xml.beans.CNamespaceValue;
import cn.taketoday.assistant.model.xml.beans.InfraBean;
import cn.taketoday.assistant.model.xml.beans.PNamespaceRefValue;
import cn.taketoday.assistant.model.xml.beans.PNamespaceValue;

public abstract class InfraXNamespaceDomExtender extends DomExtender<InfraBean> {
  private final String myNamespace;
  private final String myNamespaceKey;
  private final Class<?> myValueClass;
  private final Class<?> myRefValueClass;

  protected InfraXNamespaceDomExtender(String namespace, String namespaceKey, Class<?> valueClass, Class<?> refValueClass) {
    this.myNamespace = namespace;
    this.myNamespaceKey = namespaceKey;
    this.myValueClass = valueClass;
    this.myRefValueClass = refValueClass;
  }

  @Override
  public final void registerExtensions(InfraBean bean, DomExtensionsRegistrar registrar) {
    XmlTag tag = bean.getXmlTag();
    if (tag == null || tag.getPrefixByNamespace(this.myNamespace) == null) {
      return;
    }
    XmlAttribute[] attributes = tag.getAttributes();
    for (XmlAttribute attribute : attributes) {
      if (this.myNamespace.equals(attribute.getNamespace())) {
        String name = attribute.getLocalName();
        registrar.registerAttributeChildExtension(new XmlName(name, this.myNamespaceKey), name.endsWith("-ref") ? this.myRefValueClass : this.myValueClass);
      }
    }
  }

  public static class P extends InfraXNamespaceDomExtender {

    public P() {
      super(InfraConstant.P_NAMESPACE, InfraConstant.P_NAMESPACE_KEY, PNamespaceValue.class, PNamespaceRefValue.class);
    }
  }

  public static class C extends InfraXNamespaceDomExtender {

    public C() {
      super(InfraConstant.C_NAMESPACE, InfraConstant.C_NAMESPACE_KEY, CNamespaceValue.class, CNamespaceRefValue.class);
    }
  }
}
