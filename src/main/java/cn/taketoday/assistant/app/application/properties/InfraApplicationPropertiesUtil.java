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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;

final class InfraApplicationPropertiesUtil {
  private static final String DOCUMENT_SEPARATOR = "#---";

  @Nullable
  static PropertyKeyImpl getPropertyKey(PropertyImpl property) {
    ASTNode keyNode = property.getKeyNode();
    if (keyNode == null) {
      return null;
    }
    else {
      PsiElement keyElement = keyNode.getPsi();
      return !(keyElement instanceof PropertyKeyImpl) ? null : (PropertyKeyImpl) keyElement;
    }
  }

  @Nullable
  static PropertyValueImpl getPropertyValue(PropertyImpl property) {
    ASTNode valueNode = property.getValueNode();
    if (valueNode == null) {
      return null;
    }
    else {
      PsiElement valueElement = valueNode.getPsi();
      return !(valueElement instanceof PropertyValueImpl) ? null : (PropertyValueImpl) valueElement;
    }
  }

  static boolean isSupportMultiDocuments(Module module) {
    return true;
  }

  static List<IProperty> getDocument(Property anchor) {
    List<IProperty> result = new SmartList<>();
    List<IProperty> before = new SmartList<>();

    PsiElement prev;
    for (PsiElement element = anchor.getPrevSibling(); element != null; element = prev) {
      prev = element.getPrevSibling();
      if (element instanceof Property) {
        before.add((Property) element);
      }
      else if (isDocumentSeparator(element, prev)) {
        break;
      }
    }

    result.addAll(ContainerUtil.reverse(before));
    collectDocument(anchor, result);
    return result;
  }

  static List<List<IProperty>> getDocuments(PropertiesFile file) {
    Property property = (Property) ContainerUtil.getFirstItem(file.getProperties());
    if (property == null) {
      return Collections.emptyList();
    }
    else {
      List<List<IProperty>> result = new SmartList<>();

      while (property != null) {
        List<IProperty> document = new SmartList<>();
        property = collectDocument(property, document);
        result.add(document);
      }

      return result;
    }
  }

  private static Property collectDocument(Property property, List<IProperty> document) {
    document.add(property);
    PsiElement current = property;

    PsiElement next;
    for (next = property.getNextSibling(); next != null; next = next.getNextSibling()) {
      if (next instanceof Property) {
        document.add((Property) next);
      }
      else if (isDocumentSeparator(next, current)) {
        break;
      }

      current = next;
    }

    do {
      if (next == null) {
        return null;
      }

      next = next.getNextSibling();
    }
    while (!(next instanceof Property));

    return (Property) next;
  }

  private static boolean isDocumentSeparator(PsiElement candidate, PsiElement prevSibling) {
    return candidate instanceof PsiComment && candidate.getText().trim().equals(DOCUMENT_SEPARATOR)
            && "\n".equals(StringUtil.convertLineSeparators(prevSibling.getText()));
  }
}
