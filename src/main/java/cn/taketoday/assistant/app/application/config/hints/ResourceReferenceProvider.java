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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.microservices.jvm.config.hints.HintReferenceProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;

import java.util.List;

import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;

class ResourceReferenceProvider implements HintReferenceProvider {

  public PsiReference[] getReferences(PsiElement element, List<TextRange> textRanges, ProcessingContext context) {
    String text = element.getText();
    PsiReference[] result = PsiReference.EMPTY_ARRAY;
    for (TextRange range : textRanges) {
      String nextValue = range.substring(text);
      InfraResourcesBuilder builder = InfraResourcesBuilder.create(element, nextValue, range.getStartOffset()).fromRoot(nextValue.startsWith("/")).soft(true);
      result = ArrayUtil.mergeArrays(result, ResourcesUtil.of().getReferences(builder));
    }
    return result;
  }
}
