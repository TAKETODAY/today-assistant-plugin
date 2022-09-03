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

import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.utils.InfraReferenceUtils;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;
import cn.taketoday.lang.Nullable;

public class PropertiesFileConverter extends JamSimpleReferenceConverter<Set<PropertiesFile>> {

  @Override
  public Set<PropertiesFile> fromString(@Nullable String s, JamStringAttributeElement<Set<PropertiesFile>> context) {
    PsiAnnotationMemberValue psiElement = context.getPsiElement();
    if (s != null) {
      PsiLiteral psiLiteral = (psiElement instanceof PsiLiteral) ? (PsiLiteral) psiElement : getFakePsiLiteralElement(psiElement, s);
      if (psiLiteral != null) {
        return new LinkedHashSet<>(getResourceFiles(psiLiteral, s, ",",
                item -> item instanceof PropertiesFileImpl));
      }
    }
    return Collections.emptySet();
  }

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<Set<PropertiesFile>> context,
          PsiLanguageInjectionHost injectionHost) {
    return getReferences(context, injectionHost);
  }

  private static PsiReference[] getReferences(JamStringAttributeElement<Set<PropertiesFile>> context,
          PsiLanguageInjectionHost injectionHost) {
    String s = ElementManipulators.getValueText(injectionHost);

    return ResourcesUtil.of()
            .getReferences(InfraResourcesBuilder.create(injectionHost, s).fromRoot(s.startsWith("/")).soft(false));
  }

  private static List<PropertiesFile> getResourceFiles(PsiLiteral element,
          String s,
          String delimiter,
          Condition<PsiFileSystemItem> filter) {
    List<PsiReference> references = new ArrayList<>();
    int startInElement = ElementManipulators.getOffsetInElement(element);

    InfraReferenceUtils.processSeparatedString(s, delimiter, (separatedString, offset) -> {
      InfraResourcesBuilder builder =
              InfraResourcesBuilder.create(element, separatedString).offset(offset + startInElement);
      ContainerUtil.addAll(references, ResourcesUtil.of().getReferences(builder));
      return true;
    });

    Collection<PsiFile> files = ResourcesUtil.of().getResourceItems(
            references.toArray(PsiReference.EMPTY_ARRAY), filter);

    return ContainerUtil.mapNotNull(files, PropertiesImplUtil::getPropertiesFile);
  }

  @Nullable
  private static PsiLiteral getFakePsiLiteralElement(PsiElement element,
          String s) {
    if (element == null)
      return null;
    try {
      PsiExpression psiExpression =
              JavaPsiFacade.getElementFactory(element.getProject()).createExpressionFromText("\"" + s + "\"", element);
      if (psiExpression instanceof PsiLiteral) {
        return (PsiLiteral) psiExpression;
      }
    }
    catch (IncorrectOperationException ignored) {
    }
    return null;
  }
}
