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

import com.intellij.ide.presentation.Presentation;
import com.intellij.microservices.utils.CommonFakeNavigatablePomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;

import java.util.Collection;

import cn.taketoday.assistant.web.mvc.model.WebMvcVariablesService;
import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;
import kotlin.collections.CollectionsKt;

public abstract class MvcServletContextAttributesProvider extends ServletsAttributeHolderProvider {

  public MvcServletContextAttributesProvider(String className) {
    super(className, ServletContextAttribute::new);
  }

  @Override
  public Object[] getCompletionVariants(PsiReference reference) {
    WebMvcVariablesService webMvcVariablesService = WebMvcVariablesService.of();
    PsiElement element = reference.getElement();
    Iterable<PsiVariable> applicationVariables = webMvcVariablesService.getApplicationVariables(element.getProject());
    Collection<PsiVariable> $this$toTypedArray$iv = CollectionsKt.toSet(applicationVariables);
    return $this$toTypedArray$iv.toArray(new Object[0]);
  }

  @Presentation(typeName = WebMvcPresentationConstant.SERVLET_CONTEXT_ATTRIBUTE)
  private static final class ServletContextAttribute extends CommonFakeNavigatablePomTarget.SimpleNamePomTarget {
    public ServletContextAttribute(String name) {
      super(name);
    }
  }
}
