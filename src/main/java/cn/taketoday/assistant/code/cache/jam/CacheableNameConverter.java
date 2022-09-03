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

package cn.taketoday.assistant.code.cache.jam;

import com.intellij.codeInsight.completion.CompletionUtilCoreImpl;
import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.utils.CommonFakeNavigatablePomTarget;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.HashSet;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.code.CacheableAnnotator;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:20
 */
public final class CacheableNameConverter extends JamConverter<String> {

  @Override
  public PsiReference[] createReferences(JamStringAttributeElement<String> jamStringAttributeElement, PsiLanguageInjectionHost injectionHost) {
    return new PsiReference[] {
            new CacheableNameReference(jamStringAttributeElement, injectionHost)
    };
  }

  @Override
  @Nullable
  public String fromString(@Nullable String s, @Nullable JamStringAttributeElement<String> jamStringAttributeElement) {
    return s;
  }

  public static final class CacheableNameReference
          extends PsiReferenceBase<PsiLanguageInjectionHost> implements HighlightedReference {
    private final JamStringAttributeElement<String> context;

    public CacheableNameReference(JamStringAttributeElement<String> jamStringAttributeElement, PsiLanguageInjectionHost psiElement) {
      super(psiElement);
      this.context = jamStringAttributeElement;
    }

    @Override
    public Object[] getVariants() {
      String it;
      Iterable<CacheableElement> findAllCacheable = CacheableAnnotator.findAllCacheable(this.context.getPsiElement());
      HashSet<String> existingCacheableNames = new HashSet<>();
      for (CacheableElement<?> element : findAllCacheable) {
        CollectionsKt.addAll(existingCacheableNames, element.getCacheNames());
      }
      it = getCurrentContextCacheableName(this.context);
      if (it != null) {
        existingCacheableNames.remove(it);
      }
      ArrayList<LookupElementBuilder> ret = new ArrayList<>(Math.max(existingCacheableNames.size(), 10));
      for (String item$iv$iv : existingCacheableNames) {
        ret.add(LookupElementBuilder.create(item$iv$iv).withIcon(Icons.ShowCacheable));
      }
      return ret.toArray(new Object[0]);
    }

    @Override
    @Nullable
    public PsiElement resolve() {
      String name = this.context.getStringValue();
      if (name != null) {
        PsiManager psiManager = this.context.getPsiManager();
        Project project = psiManager.getProject();
        return new CommonFakeNavigatablePomTarget(project, new CacheableNameTarget(name));
      }
      else {
        return null;
      }
    }
  }

  public static String getCurrentContextCacheableName(JamStringAttributeElement<String> jamStringAttributeElement) {
    UElement uElement = UastContextKt.toUElement(jamStringAttributeElement.getPsiElement());
    PsiElement sourcePsi = uElement != null ? uElement.getSourcePsi() : null;
    PsiElement sourcePsiInOriginalFile = sourcePsi != null ? CompletionUtilCoreImpl.getOriginalElement(sourcePsi) : null;
    UExpression uLiteralInOriginalFile = UastContextKt.toUElement(sourcePsiInOriginalFile, UExpression.class);
    if (uLiteralInOriginalFile != null) {
      return UastUtils.evaluateString(uLiteralInOriginalFile);
    }
    return null;
  }
}
