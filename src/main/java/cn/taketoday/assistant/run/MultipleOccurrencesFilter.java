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

package cn.taketoday.assistant.run;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.execution.filters.Filter;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;

import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.lang.Nullable;

abstract class MultipleOccurrencesFilter implements Filter {

  @Nullable
  public final Filter.Result applyFilter(String line, int entireLength) {

    int startOffset = 0;
    List<ResultItem> items = new SmartList<>();

    do {
      Filter.Result occurrence = this.findNextOccurrence(startOffset, line, entireLength);
      if (occurrence == null) {
        break;
      }

      items.add(occurrence);
      startOffset = -(entireLength - occurrence.getResultItems().get(0).getHighlightEndOffset() + 1 - line.length());
    }
    while (startOffset >= 0);

    return items.isEmpty() ? null : new Filter.Result(items);
  }

  protected abstract Filter.Result findNextOccurrence(int var1, String var2, int var3);

  protected static void showResult(Consumer<? super Editor> consumer) {
    DataManager.getInstance().getDataContextFromFocusAsync().onSuccess((context) -> {
      Editor editor = CommonDataKeys.EDITOR.getData(context);
      if (editor != null) {
        Project project = CommonDataKeys.PROJECT.getData(context);
        if (project != null && !DumbService.isDumb(project)) {
          consumer.consume(editor);
        }
        else {
          HintManager.getInstance().showErrorHint(editor, InfraBundle.message("message.navigation.available.during.indexing"));
        }
      }
    });
  }
}
