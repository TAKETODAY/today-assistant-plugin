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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.web.mvc.services.WebMvcService;

import static cn.taketoday.assistant.InfraAppBundle.message;

class ViewReference extends PsiReferenceBase.Poly<PsiElement> implements EmptyResolveMessageProvider, HighlightedReference, ViewMultiResolverReference {
  private static final Logger LOG = Logger.getInstance(ViewReference.class);
  private ViewResolver myResolver;

  private final NotNullLazyValue<Set<? extends ViewResolver>> myResolvers;

  ViewReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
    this.myResolvers = NotNullLazyValue.lazy(() -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(this.myElement);
      return module != null ? WebMvcService.getInstance().getViewResolvers(module) : Collections.emptySet();
    });
  }

  public Object[] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) {
      return EMPTY_ARRAY;
    }
    List<LookupElement> allViews = new ArrayList<>();
    for (ViewResolver resolver : this.myResolvers.get()) {
      allViews.addAll(resolver.getAllResolverViews());
    }
    return ArrayUtil.toObjectArray(allViews);
  }

  public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
    LOG.assertTrue(this.myResolver != null, "Trying to bind a non-resolved reference? Resolvers: " + this.myResolvers + ", element: " + element);
    String newName = this.myResolver.bindToElement(element);
    return newName == null ? getElement() : ElementManipulators.handleContentChange(getElement(), newName);
  }

  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return super.handleElementRename(this.myResolver.handleElementRename(newElementName));
  }

  public String getUnresolvedMessagePattern() {
    String message;
    if ((this.myResolvers.get()).isEmpty()) {
      message = message("ViewReference.no.view.resolvers.found");
    }
    else {
      message = message("ViewReference.cannot.resolve.mvc.view");
    }
    return message;
  }

  public ResolveResult[] multiResolve(boolean incompleteCode) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module != null) {
      String viewName = getCanonicalText();
      for (ViewResolver resolver : this.myResolvers.get()) {
        Set<PsiElement> resolvedViews = resolver.resolveView(viewName);
        if (resolvedViews.size() > 0) {
          this.myResolver = resolver;
          return PsiElementResolveResult.createResults(resolvedViews);
        }
      }
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  @Override
  public boolean hasResolvers() {
    return !this.myResolvers.get().isEmpty();
  }
}
