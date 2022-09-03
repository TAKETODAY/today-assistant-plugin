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
package cn.taketoday.assistant.app.application.config;

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide access to configured replacement tokens.
 */
public class InfraReplacementTokenStorage {

  private final List<String> myTokens = new SmartList<>();

  public void deserialize(String input) {
    myTokens.clear();
    myTokens.addAll(StringUtil.split(input, " "));
  }

  public String serialize() {
    return StringUtil.join(myTokens, " ");
  }

  public List<String> getTokens() {
    return myTokens;
  }

  /**
   * Returns list of valid tokens as {@code prefix|suffix}.
   *
   * @return Valid tokens.
   */
  public List<Couple<String>> getReplacementTokens() {
    List<Couple<String>> tokenList = new ArrayList<>(myTokens.size());
    for (String value : myTokens) {
      if (StringUtil.isEmptyOrSpaces(value))
        continue;

      if (StringUtil.containsChar(value, '*')) {
        String prefix = StringUtil.substringBefore(value, "*");
        String suffix = StringUtil.substringAfter(value, "*");
        if (StringUtil.isEmptyOrSpaces(prefix) || StringUtil.isEmptyOrSpaces(suffix))
          continue;
        tokenList.add(Couple.of(prefix, suffix));
      }
      else {
        tokenList.add(Couple.of(value, value));
      }
    }
    return tokenList;
  }
}
