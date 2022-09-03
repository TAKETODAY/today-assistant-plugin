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

package cn.taketoday.assistant.web.mvc;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceProvider;
import com.intellij.psi.UastReferenceRegistrar;

import org.jetbrains.uast.expressions.UInjectionHost;

import java.util.List;

public final class WebMvcVariablesReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    for (WebMvcVariableReferenceProvider variablesProvider : List.of(
            MvcJavaxSessionAttributesProvider.INSTANCE,
            MvcJakartaSessionAttributesProvider.INSTANCE,
            WebMvcSessionAttributesAnnotationProvider.INSTANCE,
            WebMvcSessionAttributeAnnotationProvider.INSTANCE,
            WebMvcJavaxRequestAttributesProvider.INSTANCE,
            WebMvcJakartaRequestAttributesProvider.INSTANCE,
            WebMvcRequestAttributeAnnotationProvider.INSTANCE,
            MvcServletJavaxContextAttributesProvider.INSTANCE,
            MvcServletJakartaContextAttributesProvider.INSTANCE)
    ) {
      UastReferenceRegistrar.registerUastReferenceProvider(registrar, variablesProvider.getPattern(), getVariablesReferenceProvider(variablesProvider), 0.0d);
    }
  }

  private UastReferenceProvider getVariablesReferenceProvider(WebMvcVariableReferenceProvider contributor) {
    return UastReferenceRegistrar.uastReferenceProvider(UInjectionHost.class, (host, element) -> {
      PsiElement mo0getResolveTarget = contributor.getResolveTarget(host.getPsiLanguageInjectionHost());
      if (mo0getResolveTarget != null) {
        return new PsiReference[] {
                new VariablesReference(contributor, host, mo0getResolveTarget)
        };
      }
      return PsiReference.EMPTY_ARRAY;
    });
  }

  private static final class VariablesReference extends PsiReferenceBase<PsiLanguageInjectionHost> {

    private final WebMvcVariableReferenceProvider contributor;
    private final PsiElement target;

    public WebMvcVariableReferenceProvider getContributor() {
      return this.contributor;
    }

    public VariablesReference(WebMvcVariableReferenceProvider contributor, UInjectionHost host, PsiElement target) {
      super(host.getPsiLanguageInjectionHost(), false);
      this.contributor = contributor;
      this.target = target;
    }

    public PsiElement resolve() {
      return this.target;
    }

    public Object[] getVariants() {
      return this.contributor.getCompletionVariants(this);
    }
  }
}
