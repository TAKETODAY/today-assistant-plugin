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

package cn.taketoday.assistant.web.mvc.model.xml;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class ResourcesLocationConverter extends DelimitedListConverter<PsiFileSystemItem> {
  private static final Condition<PsiFileSystemItem> DIRECTORY_CONDITION = item -> {
    return item.isDirectory();
  };

  public ResourcesLocationConverter() {
    super(", ");
  }

  @Nullable
  public PsiFileSystemItem convertString(@Nullable String string, ConvertContext context) {
    if (string == null) {
      return null;
    }
    PsiReference[] references = getReferences(string, context);
    if (references.length == 0) {
      return null;
    }
    for (int i = references.length - 1; i >= 0; i--) {
      FileReference fileReference = FileReference.findFileReference(references[i]);
      if (fileReference != null) {
        FileReference lastFileReference = fileReference.getLastFileReference();
        if (lastFileReference == null) {
          return null;
        }
        PsiFileSystemItem result = lastFileReference.resolve();
        if (result != null) {
          if (!DIRECTORY_CONDITION.value(result)) {
            return null;
          }
          return result;
        }
      }
    }
    return null;
  }

  @Nullable
  public String toString(@Nullable PsiFileSystemItem item) {
    if (item == null) {
      return null;
    }
    return item.getName();
  }

  protected Object[] getReferenceVariants(ConvertContext context, GenericDomValue<? extends List<PsiFileSystemItem>> genericDomValue) {
    String text = genericDomValue.getStringValue();
    List<Object> variants = new ArrayList<>();
    for (PsiReference reference : getReferences(text, context)) {
      ContainerUtil.addAll(variants, reference.getVariants());
    }
    return ArrayUtil.toObjectArray(variants);
  }

  @Nullable
  public PsiElement resolveReference(@Nullable PsiFileSystemItem item, ConvertContext context) {
    return item;
  }

  protected String getUnresolvedMessage(String value) {
    return InfraAppBundle.message("resource.location.unresolved.message", value);
  }

  private static PsiReference[] getReferences(@Nullable String string, ConvertContext context) {
    if (string == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    XmlElement xmlElement = context.getXmlElement();
    if (xmlElement == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    InfraResourcesBuilder builder = InfraResourcesBuilder.create(xmlElement, string).filter(DIRECTORY_CONDITION).endingSlashNotAllowed(false);
    return ResourcesUtil.of().getReferences(builder);
  }
}
