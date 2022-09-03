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
package cn.taketoday.assistant.model.converters;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.Function;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;

import java.util.Collection;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class InfraResourceConverter extends Converter<PsiFileSystemItem> implements CustomReferenceConverter<PsiFileSystemItem> {

  @Override
  public PsiFileSystemItem fromString(@Nullable String s, ConvertContext context) {
    if (s != null) {
      DomElement domElement = context.getInvocationElement();
      if (domElement instanceof final GenericAttributeValue attributeValue) {
        PsiReference[] references = createReferences(attributeValue.getXmlAttributeValue(), attributeValue.getStringValue(),
                isEndingSlashNotAllowed());
        if (references.length > 0) {
          for (int i = references.length - 1; i >= 0; i--) {
            PsiReference reference = references[i];
            if (!(reference instanceof FileReference)) {
              continue;
            }

            PsiElement result = reference.resolve();
            if (result instanceof PsiFileSystemItem) {
              return (PsiFileSystemItem) result;
            }
          }
        }
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable PsiFileSystemItem psiFile, ConvertContext context) {
    return null;
  }

  @Override
  public PsiReference[] createReferences(GenericDomValue<PsiFileSystemItem> genericDomValue,
          PsiElement element,
          ConvertContext context) {
    return createReferences(element, genericDomValue.getStringValue(), isEndingSlashNotAllowed());
  }

  protected boolean isEndingSlashNotAllowed() {
    return true;
  }

  protected Condition<PsiFileSystemItem> getCondition() {
    return Conditions.alwaysTrue();
  }

  @Nullable
  protected Function<PsiFile, Collection<PsiFileSystemItem>> getCustomDefaultPathEvaluator(PsiElement element, String s) {
    return null;
  }

  private PsiReference[] createReferences(@Nullable PsiElement element, @Nullable String s, boolean endingSlashNotAllowed) {
    if (s == null || element == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, s).
            fromRoot(StringUtil.startsWithChar(s, '/')).
            filter(getCondition()).
            endingSlashNotAllowed(endingSlashNotAllowed).
            customDefaultPathEvaluator(getCustomDefaultPathEvaluator(element, s));

    return ResourcesUtil.of().getReferences(builder);
  }

  public static class Allowed extends InfraResourceConverter {
    @Override
    protected boolean isEndingSlashNotAllowed() {
      return false;
    }
  }

  public static class Directory extends InfraResourceConverter {

    @Override
    protected Condition<PsiFileSystemItem> getCondition() {
      return FileReferenceSet.DIRECTORY_FILTER;
    }
  }
}
