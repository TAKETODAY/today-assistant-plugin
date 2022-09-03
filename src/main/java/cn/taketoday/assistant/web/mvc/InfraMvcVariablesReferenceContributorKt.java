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
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;

import java.util.Collection;

import cn.taketoday.assistant.web.mvc.model.WebMvcVariablesService;
import kotlin.collections.CollectionsKt;

public final class InfraMvcVariablesReferenceContributorKt {

  public static Object[] getAllSessionVariables(PsiReference reference) {
    WebMvcVariablesService mvcVariablesService = WebMvcVariablesService.of();
    PsiElement element = reference.getElement();
    Iterable<PsiVariable> sessionVariables = mvcVariablesService.getSessionVariables(element.getProject());
    Collection<PsiVariable> toArray = CollectionsKt.toSet(sessionVariables);
    return toArray.toArray(new PsiVariable[0]);
  }

  public static Object[] getAllRequestVariables(PsiReference reference) {
    WebMvcVariablesService mvcVariablesService = WebMvcVariablesService.of();
    PsiElement element = reference.getElement();
    Iterable<PsiVariable> requestVariables = mvcVariablesService.getRequestVariables(element.getProject());
    Collection array = CollectionsKt.toSet(requestVariables);
    return array.toArray(new Object[0]);
  }
}
