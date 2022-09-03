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
package cn.taketoday.assistant.model.jam.converters;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.assistant.model.converters.InfraConverterUtil;
import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.lang.Nullable;

public class PackageListJamConverter extends JamConverter<Collection<PsiPackage>> {

  @Override
  public Collection<PsiPackage> fromString(@Nullable String s,
          JamStringAttributeElement<Collection<PsiPackage>> context) {
    PsiLanguageInjectionHost host = context.getLanguageInjectionHost();
    if (host == null || StringUtil.isEmptyOrSpaces(s))
      return Collections.emptyList();

    if (InfraConverterUtil.containsPatternReferences(s)) {
      return InfraConverterUtil.getPsiPackages(createReferences(context, host));
    }

    return InfraConverterUtil.getPackages(s, context.getPsiManager().getProject());
  }

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<Collection<PsiPackage>> context,
          PsiLanguageInjectionHost injectionHost) {
    return InfraReferenceUtils.getPsiPackagesReferences(injectionHost,
            context.getStringValue(),
            ElementManipulators.getOffsetInElement(injectionHost));
  }
}
