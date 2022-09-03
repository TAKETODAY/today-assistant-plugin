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

package cn.taketoday.assistant.model.config.jam;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;

import java.util.Collection;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class ResourceItemsConverter extends JamConverter<Collection<PsiFileSystemItem>> {

  @Nullable
  public Collection<PsiFileSystemItem> fromString(@Nullable String s, JamStringAttributeElement<Collection<PsiFileSystemItem>> context) {
    PsiLanguageInjectionHost host = context.getLanguageInjectionHost();
    if (host == null) {
      return null;
    }
    return ResourcesUtil.of().getResourceItems(createReferences(context, host), Conditions.alwaysTrue());
  }

  public PsiReference[] createReferences( JamStringAttributeElement<Collection<PsiFileSystemItem>> context,  PsiLanguageInjectionHost injectionHost) {
    String s = context.getStringValue();
    if (s == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    InfraResourcesBuilder resourcesBuilder = InfraResourcesBuilder.create(injectionHost, s).fromRoot(s.startsWith("/")).soft(true);
    return ResourcesUtil.of().getReferences(resourcesBuilder);
  }
}
