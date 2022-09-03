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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Nullable;

public class CompositeViewResolver extends ViewResolver {
  private final List<? extends ViewResolver> myViewResolvers;

  public CompositeViewResolver(List<? extends ViewResolver> viewResolvers) {
    this.myViewResolvers = viewResolvers;
  }

  @Override
  @TestOnly

  public String getID() {
    return "CompositeViewResolver[" + StringUtil.join(this.myViewResolvers, ViewResolver::getID, "|") + "]";
  }

  @Override

  public Set<PsiElement> resolveView(String viewName) {
    Set<PsiElement> elements = new HashSet<>();
    for (ViewResolver resolver : this.myViewResolvers) {
      ContainerUtil.addAllNotNull(elements, resolver.resolveView(viewName));
    }
    return elements;
  }

  @Override

  public List<LookupElement> getAllResolverViews() {
    List<LookupElement> allViews = new ArrayList<>();
    for (ViewResolver resolver : this.myViewResolvers) {
      allViews.addAll(resolver.getAllResolverViews());
    }
    return allViews;
  }

  @Override
  @Nullable
  public String bindToElement(PsiElement element) {
    throw new UnsupportedOperationException("not supported " + element);
  }

  @Override

  public String handleElementRename(String newElementName) {
    ViewResolver viewResolver = ContainerUtil.getFirstItem(this.myViewResolvers);
    if (viewResolver != null) {
      return viewResolver.handleElementRename(newElementName);
    }
    return newElementName;
  }

  public String toString() {
    return "CompositeViewResolver{myViewResolvers=" + this.myViewResolvers.size() + "}";
  }
}
