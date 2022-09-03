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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.beanProperties.CreateBeanPropertyFixes;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.xml.XmlUndefinedElementFixProvider;

import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraBeanUtils;

public class PNamespaceUndefinedElementFixProvider extends XmlUndefinedElementFixProvider {

  public IntentionAction[] createFixes(XmlAttribute element) {
    BeanPointer<?> pointer;
    if (!InfraConstant.P_NAMESPACE.equals(element.getNamespace())) {
      return null;
    }
    String localName = element.getLocalName();
    PsiClass psiClass = AbstractBeanNamespaceDescriptor.getClass(element.getParent());
    if (psiClass != null) {
      PsiType type = null;
      if (localName.endsWith("-ref")) {
        InfraModel model = InfraManager.from(element.getProject()).getInfraModelByFile(element.getContainingFile());
        String value = element.getDisplayValue();
        if (model != null && value != null && (pointer = InfraBeanUtils.of().findBean(model, value)) != null) {
          PsiType[] types = pointer.getEffectiveBeanTypes();
          if (types.length > 0) {
            type = types[0];
          }
        }
      }
      String name = StringUtil.trimEnd(localName, "-ref");
      return CreateBeanPropertyFixes.createActions(name, psiClass, type, true);
    }
    return null;
  }
}
