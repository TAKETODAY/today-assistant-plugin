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
package cn.taketoday.assistant.app.application.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;

import java.util.List;

/**
 * Resolves replacement tokens ("@my.property@") in configuration files to external definitions, e.g. build files.
 */
public abstract class InfraReplacementTokenResolver {

  public static final ExtensionPointName<InfraReplacementTokenResolver> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.replacementTokenResolver");

  public abstract List<PsiElement> resolve(PsiReference reference);

  public abstract List<LookupElement> getVariants(PsiReference reference);

  public static PsiReference createReference(PsiElement element, TextRange range) {
    return new ReplacementTokenReference(element, range);
  }

  public static final class ReplacementTokenReference extends PsiReferenceBase.Poly<PsiElement> {

    private ReplacementTokenReference(PsiElement element, TextRange rangeInElement) {
      super(element, rangeInElement, true);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
      List<PsiElement> allResults = new SmartList<>();
      for (InfraReplacementTokenResolver replacementTokenResolver : EP_NAME.getExtensions()) {
        allResults.addAll(replacementTokenResolver.resolve(this));
      }
      return PsiElementResolveResult.createResults(allResults);
    }

    @Override
    public Object[] getVariants() {
      List<LookupElement> variants = new SmartList<>();
      for (InfraReplacementTokenResolver replacementTokenResolver : EP_NAME.getExtensions()) {
        variants.addAll(replacementTokenResolver.getVariants(this));
      }
      return ArrayUtil.toObjectArray(variants);
    }
  }
}
