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

package cn.taketoday.assistant.injection;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.GenericAttributeValue;

import org.intellij.lang.regexp.RegExpLanguage;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.model.xml.context.Filter;
import cn.taketoday.assistant.model.xml.context.Type;

public class InfraContextSchemaInjector implements MultiHostInjector {
  private static final String EXPRESSION_ATTR_NAME = "expression";

  public void getLanguagesToInject(MultiHostRegistrar registrar, PsiElement host) {
    PsiFile file = host.getContainingFile();
    if (JamCommonUtil.isPlainXmlFile(file)) {
      if (file instanceof XmlFile) {
        if (InfraDomUtils.isInfraXml((XmlFile) file)) {
          XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(host, XmlAttribute.class);
          if (xmlAttribute != null && EXPRESSION_ATTR_NAME.equals(xmlAttribute.getLocalName())) {
            GenericAttributeValue value = DomManager.getDomManager(host.getProject()).getDomElement(xmlAttribute);
            if (value != null) {
              Filter filter = value.getParentOfType(Filter.class, true);
              if (filter != null && Type.REGEX.equals(filter.getType().getValue())) {
                injectRegexpLanguage(registrar, (PsiLanguageInjectionHost) host);
              }
            }
          }
        }
      }
    }
  }

  private static void injectRegexpLanguage(MultiHostRegistrar registrar, PsiLanguageInjectionHost host) {
    registrar.startInjecting(RegExpLanguage.INSTANCE)
            .addPlace(null, null, host, ElementManipulators.getValueTextRange(host))
            .doneInjecting();
  }

  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(XmlAttributeValue.class);
  }
}
