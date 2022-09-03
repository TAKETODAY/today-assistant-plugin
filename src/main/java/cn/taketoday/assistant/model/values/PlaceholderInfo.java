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

package cn.taketoday.assistant.model.values;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;

public class PlaceholderInfo {
  final String text;
  final Pair<String, String> prefixAndSuffix;
  final String myElementText;
  final TextRange fullTextRange;

  public PlaceholderInfo(String text, Pair<String, String> prefixAndSuffix, String elementText, TextRange fullTextRange) {
    this.text = text;
    this.prefixAndSuffix = prefixAndSuffix;
    this.myElementText = elementText;
    this.fullTextRange = fullTextRange;
  }

  public String getText() {
    return this.text;
  }
}
