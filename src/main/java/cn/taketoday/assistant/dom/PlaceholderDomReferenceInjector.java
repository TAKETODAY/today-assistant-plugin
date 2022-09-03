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

import com.intellij.codeInsight.daemon.impl.analysis.XmlHighlightVisitor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomReferenceInjector;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.Set;

import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/25 16:00
 */
public class PlaceholderDomReferenceInjector implements DomReferenceInjector {
  public static final ThreadLocal<Boolean> IS_COMPUTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

  private static final Set<Pair<String, String>> SKIP_ATTRS = Set.of(
          Pair.pair("bean", "parent"),
          Pair.pair("bean", "factory-bean"),
          Pair.pair("import", "resource"),
          Pair.pair("component-scan", "base-package")
  );

  @Nullable
  public String resolveString(@Nullable String unresolvedText, ConvertContext context) {
    if (unresolvedText == null) {
      return null;
    }
    Boolean isComputing = IS_COMPUTING.get();
    if (isComputing == Boolean.TRUE) {
      return unresolvedText;
    }
    if (!DumbService.isDumb(context.getProject())) {
      DomElement invocationElement = context.getInvocationElement();
      if ((invocationElement instanceof GenericDomValue<?> genericDomValue)
              && !skipAttributes(genericDomValue) && PlaceholderUtils.getInstance().containsDefaultPlaceholderDefinitions(genericDomValue)) {
        return PlaceholderUtils.getInstance().resolvePlaceholders(genericDomValue);
      }
    }
    return unresolvedText;
  }

  private static boolean skipAttributes(GenericDomValue<?> domElement) {
    if (!(domElement instanceof GenericAttributeValue)) {
      return false;
    }
    String attributeName = domElement.getXmlElementName();
    NotNullLazyValue<String> tagName = NotNullLazyValue.lazy(() -> domElement.getParent().getXmlElementName());
    for (Pair<String, String> pair : SKIP_ATTRS) {
      if (pair.second.equals(attributeName) && pair.first.equals(tagName.getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public PsiReference[] inject(@Nullable String unresolvedText, PsiElement element, ConvertContext context) {
    DomElement invocationElement = context.getInvocationElement();
    if (invocationElement instanceof GenericDomValue<?> genericDomValue) {
      if (PlaceholderUtils.getInstance().isPlaceholder(genericDomValue, genericDomValue.getRawText())) {
        XmlHighlightVisitor.setSkipValidation(element);
        return PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(genericDomValue);
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }

}

