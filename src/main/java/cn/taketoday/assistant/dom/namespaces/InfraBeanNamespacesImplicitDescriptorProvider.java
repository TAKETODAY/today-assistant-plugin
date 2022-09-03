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

package cn.taketoday.assistant.dom.namespaces;

import com.intellij.javaee.ImplicitNamespaceDescriptorProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.xml.XmlNSDescriptor;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.lang.Nullable;

public class InfraBeanNamespacesImplicitDescriptorProvider implements ImplicitNamespaceDescriptorProvider {

  @Nullable
  public XmlNSDescriptor getNamespaceDescriptor(@Nullable Module module, String ns, PsiFile file) {
    if (ns.equals(InfraConstant.P_NAMESPACE)) {
      return new PNamespaceDescriptor();
    }
    if (!ns.equals(InfraConstant.C_NAMESPACE)) {
      return null;
    }
    return new CNamespaceDescriptor();
  }
}
