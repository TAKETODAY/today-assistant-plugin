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

package cn.taketoday.assistant.model.highlighting.xml;

import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;

import cn.taketoday.assistant.model.values.PlaceholderPropertyReference;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public final class InfraPlaceholdersInspection extends BasicDomElementsInspection<Beans> {

  public InfraPlaceholdersInspection() {
    super(Beans.class);
  }

  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    XmlElement xmlElement;
    if (element instanceof GenericDomValue<?> value) {
      if (PlaceholderUtils.getInstance().isRawTextPlaceholder(value) && (xmlElement = getPlaceholderElement(value)) != null) {
        for (PsiReference psiReference : xmlElement.getReferences()) {
          if (psiReference instanceof PlaceholderPropertyReference placeholderPropertyReference) {
            String defaultValue = placeholderPropertyReference.getDefaultValue();
            if (defaultValue == null) {
              ResolveResult[] resolve = placeholderPropertyReference.multiResolve(false);
              if (resolve.length == 0) {
                holder.createResolveProblem(value, placeholderPropertyReference);
              }
            }
          }
        }
      }
    }
  }

  @Nullable
  private static XmlElement getPlaceholderElement(GenericDomValue<?> value) {
    XmlElement xmlElement = value.getXmlElement();
    if (xmlElement instanceof XmlAttribute xmlAttribute) {
      return xmlAttribute.getValueElement();
    }
    return xmlElement;
  }
}
