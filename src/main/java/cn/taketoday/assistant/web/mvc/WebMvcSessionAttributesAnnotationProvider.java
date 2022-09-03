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

import com.intellij.psi.PsiReference;

import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import kotlin.collections.CollectionsKt;

public final class WebMvcSessionAttributesAnnotationProvider extends AnnotationAttributeVariableProvider {

  public static final WebMvcSessionAttributesAnnotationProvider INSTANCE = new WebMvcSessionAttributesAnnotationProvider();

  private WebMvcSessionAttributesAnnotationProvider() {
    super(InfraMvcConstant.SESSION_ATTRIBUTES, CollectionsKt.listOf(RequestMapping.VALUE_ATTRIBUTE, "names"), SessionAttribute::new);
  }

  @Override
  public Object[] getCompletionVariants(PsiReference reference) {
    return InfraMvcVariablesReferenceContributorKt.getAllSessionVariables(reference);
  }
}
