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
package cn.taketoday.assistant.model.jam.testContexts.converters;

import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public abstract class ResourcePathReferenceConverter<T> extends JamSimpleReferenceConverter<List<T>> {

  @Override
  public List<T> fromString(@Nullable String s, JamStringAttributeElement<List<T>> context) {
    return s == null ? null : getFiles(getReferences(s, context));
  }

  public List<T> getFiles(PsiReference[] references) {
    for (PsiReference reference : references) {
      FileReference fileReference = FileReference.findFileReference(reference);
      if (fileReference == null)
        continue;

      FileReference lastFileReference = fileReference.getLastFileReference();
      if (lastFileReference != null) {
        ResolveResult[] resolve = lastFileReference.multiResolve(false);
        return ContainerUtil.mapNotNull(resolve, getMapper());
      }
    }
    return Collections.emptyList();
  }

  protected abstract Function<ResolveResult, T> getMapper();

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<List<T>> context,
          PsiLanguageInjectionHost injectionHost) {
    return getReferences(injectionHost);
  }

  protected PsiReference[] getReferences(String s, JamStringAttributeElement<List<T>> context) {
    PsiAnnotationMemberValue value = context.getPsiElement();
    if (value instanceof PsiLiteral psiLiteral) {
      return getReferences(psiLiteral, s);
    }
    return getReferencesOnFakeElement(context.getPsiElement(), s);
  }

  private static PsiReference[] getReferences(PsiElement psiLiteral, String s, Module... modules) {
    InfraResourcesBuilder resourcesBuilder =
            InfraResourcesBuilder.create(psiLiteral, s).fromRoot(s.startsWith("/")).soft(false).modules(modules);
    return ResourcesUtil.of().getReferences(resourcesBuilder);
  }

  protected static PsiReference[] getReferencesOnFakeElement(@Nullable PsiElement element, String s, Module... modules) {
    if (element == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    try {
      PsiExpression psiExpression =
              JavaPsiFacade.getElementFactory(element.getProject()).createExpressionFromText("\"" + s + "\"", element);
      if (psiExpression instanceof PsiLiteral) {
        return getReferences(psiExpression, s, modules);
      }
    }
    catch (IncorrectOperationException e) {
      // ignore
    }
    return PsiReference.EMPTY_ARRAY;
  }

  private static PsiReference[] getReferences(PsiLanguageInjectionHost injectionHost) {
    String s = ElementManipulators.getValueText(injectionHost);
    return getReferences(injectionHost, s);
  }
}
