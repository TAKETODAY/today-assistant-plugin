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

package cn.taketoday.assistant.suggestion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 00:05
 */
public interface ModuleSuggestionService {
  static ModuleSuggestionService getInstance(@NotNull Module module) {
    return module.getService(ModuleSuggestionService.class);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean canProvideSuggestions();

  /**
   * @param fileType type of file requesting suggestion
   * @param element element on which search is triggered. Useful for cases like identifying chioces that were already selected incase of an enum, e.t.c
   * @param ancestralKeys hierarchy of element from where the suggestion is requested. i.e if in yml user is trying to get suggestions for `s.a` under `spring:\n\trabbitmq.listener:` element, then this value would ['spring', 'rabbitmq.listener']
   * @param queryWithDotDelimitedPrefixes query string user is trying to search for. In the above example, the value for this would be `s.a`
   * @param siblingsToExclude siblings to exclude from search
   * @return results matching query string (without the containerElementsLeafToRoot). In the above example the values would be `simple.acknowledge-mode` & `simple.auto-startup`
   */
  @Nullable
  List<LookupElement> findSuggestionsForQueryPrefix(FileType fileType, PsiElement element,
          @Nullable List<String> ancestralKeys,
          String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude);

  void reindex();
}
