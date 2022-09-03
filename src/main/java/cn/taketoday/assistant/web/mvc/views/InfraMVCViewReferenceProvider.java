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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.paths.GlobalPathReferenceProvider;
import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.assistant.web.mvc.WebMVCReference;

public class InfraMVCViewReferenceProvider extends PsiReferenceProvider {
  private final boolean mySoft;
  private static final List<String> OUR_PREFIXES = Arrays.asList("redirect:", "forward:");

  public InfraMVCViewReferenceProvider() {
    this(true);
  }

  public InfraMVCViewReferenceProvider(boolean soft) {
    this.mySoft = soft;
  }

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    TextRange range = ElementManipulators.getValueTextRange(element);
    String text = range.substring(element.getText());
    SmartList<PsiReference> smartList = new SmartList<>();
    boolean prefixed = isPrefixed(text);
    if (prefixed) {
      range = adjustRange(range, text, OUR_PREFIXES);
      if (GlobalPathReferenceProvider.isWebReferenceUrl(range.substring(element.getText()))) {
        return new PsiReference[] { new WebReference(element, range) };
      }
    }
    if (prefixed) {
      smartList.add(new WebMVCReference(element, range, range.substring(element.getText()), this.mySoft));
    }
    if (!prefixed) {
      smartList.add(new ViewReference(element, range, this.mySoft));
    }
    return smartList.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static boolean isPrefixed(String text) {
    return OUR_PREFIXES.stream().anyMatch(text::startsWith);
  }

  private static TextRange adjustRange(TextRange range, String text, List<String> prefixes) {
    return prefixes.stream()
            .filter(text::startsWith)
            .findAny()
            .map(prefix2 -> new TextRange(range.getStartOffset() + prefix2.length(), range.getEndOffset()))
            .orElse(range);
  }
}
