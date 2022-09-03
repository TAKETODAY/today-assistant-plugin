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
package cn.taketoday.assistant.model.utils;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairProcessor;
import com.intellij.util.SmartList;

import java.util.List;

import cn.taketoday.assistant.model.jam.converters.InfraAntPatternPackageReferenceSet;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import cn.taketoday.lang.Nullable;

public final class InfraReferenceUtils {

  public static PsiReference[] getPsiPackagesReferences(PsiElement element,
          @Nullable String text, int startInElement) {
    return getPsiPackagesReferences(element, text, startInElement, ",; \n\t");
  }

  public static PsiReference[] getPsiPackagesReferences(PsiElement element,
          @Nullable String text, int startInElement, String delimiters) {
    return getPsiPackagesReferences(element, text, startInElement, delimiters, getResolveScope(element));
  }

  public static PsiReference[] getPsiPackagesReferences(PsiElement element,
          @Nullable String text, int startInElement, String delimiters, GlobalSearchScope scope) {
    if (text == null || PlaceholderUtils.getInstance().isDefaultPlaceholder(text)) {
      return PsiReference.EMPTY_ARRAY;
    }

    List<PsiReference> list = new SmartList<>();
    new DelimitedListProcessor(delimiters) {
      @Override
      protected void processToken(int start, int end, boolean delimitersOnly) {
        String packageName = text.substring(start, end);
        if (PlaceholderUtils.getInstance().isDefaultPlaceholder(packageName))
          return;
        String trimmedPackageName = packageName.trim();
        PackageReferenceSet referenceSet;
        int offset = start + packageName.indexOf(trimmedPackageName) + startInElement;
        if (trimmedPackageName.equals(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)) {
          referenceSet = new InfraAntPatternPackageReferenceSet(CompletionUtilCore.DUMMY_IDENTIFIER, element, offset, scope);
        }
        else {
          referenceSet = new InfraAntPatternPackageReferenceSet(trimmedPackageName, element, offset, scope);
        }
        list.addAll(referenceSet.getReferences());
      }
    }.processText(text);
    return list.isEmpty() ? PsiReference.EMPTY_ARRAY : list.toArray(PsiReference.EMPTY_ARRAY);
  }

  public static GlobalSearchScope getResolveScope(PsiElement element) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return GlobalSearchScope.allScope(element.getProject());
    }

    boolean inTestSource = ProjectRootsUtil.isInTestSource(element.getContainingFile());
    return module.getModuleRuntimeScope(inTestSource);
  }

  public static boolean processSeparatedString(String str, String delimiter, PairProcessor<? super String, ? super Integer> processor) {
    if (str == null)
      return true;
    if (StringUtil.isEmptyOrSpaces(str))
      return processor.process(str, 0);

    int pos = 0;
    while (true) {
      int index = str.indexOf(delimiter, pos);
      if (index == -1)
        break;
      int nextPos = index + delimiter.length();
      String token = str.substring(pos, index);
      if (token.length() != 0) {
        if (!processor.process(token.trim(), pos + token.indexOf(token.trim())))
          return false;
      }
      pos = nextPos;
    }
    if (pos < str.length()) {
      String s = str.substring(pos);
      return processor.process(s.trim(), pos + s.indexOf(s.trim()));
    }
    return true;
  }
}
