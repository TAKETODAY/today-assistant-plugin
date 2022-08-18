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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.psi.PsiElement;

import org.apache.commons.lang.time.StopWatch;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

/**
 * A Module level service which holds the index of today,
 * provides facilities of creating, updating and querying the index.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/19 00:05
 */
public class ModuleSuggestionServiceImpl implements ModuleSuggestionService {
  private static final Logger log = Logger.getInstance(ModuleSuggestionServiceImpl.class);

  private final Module module;

  /**
   * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hyphens, underscores, e.t.c in the keys themselves
   */
  private Future<?> indexExecution;
  private boolean indexAvailable = false;

  ModuleSuggestionServiceImpl(Module module) {
    this.module = module;
  }

  @Override
  public boolean canProvideSuggestions() {
    return indexAvailable;
  }

  @Override
  public List<LookupElement> findSuggestionsForQueryPrefix(FileType fileType, PsiElement element,
          @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes,
          @Nullable Set<String> siblingsToExclude) {
    debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
    StopWatch timer = new StopWatch();
    timer.start();

    return null;
  }

  /**
   * Debug logging can be enabled by adding fully classified class name/package name with # prefix
   * For eg., to enable debug logging, go `Help > Debug log settings` & type `#cn.taketoday.assistant.startup.SuggestionServiceImpl`
   *
   * @param doWhenDebug code to execute when debug is enabled
   */
  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }

  @Override
  public void reindex() {
    if (indexExecution != null && !indexExecution.isDone()) {
      indexExecution.cancel(false);
    }
    indexExecution = getApplication().executeOnPooledThread(() ->
            DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
              debug(() -> log.debug("--> Indexing requested for module " + module.getName()));
              StopWatch moduleTimer = new StopWatch();
              moduleTimer.start();
              try {
                indexAvailable = false;
                OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

                indexAvailable = true;
              }
              finally {
                moduleTimer.stop();
                debug(() -> log.debug("<-- Indexing took " + moduleTimer + " for module " + module.getName()));
              }
            }));
  }

}
