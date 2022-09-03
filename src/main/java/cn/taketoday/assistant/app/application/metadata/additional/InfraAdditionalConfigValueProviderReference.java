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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.InfraValueProvider;
import cn.taketoday.lang.Nullable;

class InfraAdditionalConfigValueProviderReference extends PsiReferenceBase<PsiElement> implements EmptyResolveMessageProvider {

  InfraAdditionalConfigValueProviderReference(PsiElement element) {
    super(element, true);
  }

  @Nullable
  public PsiElement resolve() {
    InfraValueProvider valueProvider = getValueProvider();
    if (valueProvider != null) {
      return getElement();
    }
    return null;
  }

  @Nullable
  InfraValueProvider getValueProvider() {
    String id = getValue();
    return InfraValueProvider.findById(id);
  }

  public Object[] getVariants() {
    Function<InfraValueProvider, LookupElement> mapper = provider -> {
      return LookupElementBuilder.create(provider.getId()).appendTailText(" (" + provider.getDescription() + ")", true);
    };
    return ContainerUtil.map2Array(InfraValueProvider.values(), LookupElement.class, mapper);
  }

  public String getUnresolvedMessagePattern() {
    return InfraAppBundle.message("additional.config.unresolved.provider", getValue());
  }
}
