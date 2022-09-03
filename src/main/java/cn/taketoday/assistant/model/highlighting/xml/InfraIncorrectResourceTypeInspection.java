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

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomHighlightingHelper;
import com.intellij.util.xml.impl.GenericValueReferenceProvider;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.values.converters.resources.InfraResourceTypeProvider;
import cn.taketoday.assistant.model.values.converters.resources.ResourceTypeCondition;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public final class InfraIncorrectResourceTypeInspection extends BasicDomElementsInspection<Beans> {
  private static final GenericValueReferenceProvider ourProvider = new GenericValueReferenceProvider();

  public InfraIncorrectResourceTypeInspection() {
    super(Beans.class);
  }

  protected boolean shouldCheckResolveProblems(GenericDomValue value) {
    return false;
  }

  protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
    GenericDomValue value;
    XmlElement valueElement;
    Condition<PsiFileSystemItem> filter;
    PsiFileSystemItem psiFileSystemItem;
    String message;
    if (!(element instanceof GenericDomValue) || (valueElement = DomUtil.getValueElement((value = (GenericDomValue) element))) == null || (filter = findCondition(value)) == null) {
      return;
    }
    for (PsiReference fileReference : ourProvider.getReferencesByElement(valueElement, new ProcessingContext())) {
      if ((fileReference instanceof FileReference psrRef)
              && (psiFileSystemItem = psrRef.resolve()) != null
              && !psiFileSystemItem.isDirectory()
              && !filter.value(psiFileSystemItem)) {
        String expectedExtensions = null;
        if (filter instanceof ResourceTypeCondition) {
          expectedExtensions = StringUtil.join(((ResourceTypeCondition) filter).getExpectedExtensions(), ",");
        }
        if (expectedExtensions == null) {
          message = InfraBundle.message("IncorrectResourceTypeInspection.incorrect.resource.type");
        }
        else {
          message = InfraBundle.message("IncorrectResourceTypeInspection.expected.resource.types", expectedExtensions);
        }
        holder.createProblem(element, message);
      }
    }
  }

  @Nullable
  private static Condition<PsiFileSystemItem> findCondition(GenericDomValue value) {
    for (InfraResourceTypeProvider provider : InfraResourceTypeProvider.EP_NAME.getExtensionList()) {
      Condition<PsiFileSystemItem> filter = provider.getResourceFilter(value);
      if (filter != null) {
        return filter;
      }
    }
    return null;
  }
}
