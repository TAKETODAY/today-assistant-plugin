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
package cn.taketoday.assistant.model.config.autoconfigure.conditions;

import com.intellij.openapi.util.text.StringUtil;

public final class ConditionMessage {
  private final String myText;

  ConditionMessage(String text) {
    myText = text;
  }

  public String getText() {
    return myText;
  }

  public static ConditionMessage found(String thing, String... objects) {
    return generic("Found", thing, objects);
  }

  public static ConditionMessage didNotFind(String thing, String... objects) {
    return generic("Did not find", thing, objects);
  }

  public static ConditionMessage foundClass(String fqn) {
    return found("required class", fqn);
  }

  public static ConditionMessage didNotFindClass(String... fqns) {
    return didNotFind("required class", fqns);
  }

  public static ConditionMessage didNotFindUnwantedClass(String... fqns) {
    return didNotFind("unwanted class", fqns);
  }

  public static ConditionMessage didNotFindConfigKey(String... configKeys) {
    return didNotFind("property", configKeys);
  }

  public static ConditionMessage foundConfigKey(String configKey) {
    return found("property", configKey);
  }

  public static ConditionMessage foundConfigKeyWithValue(String configKey, String value) {
    return found("property", configKey + "=" + value);
  }

  public static ConditionMessage generic(String prefix, String thing, String... objects) {
    if (objects.length == 0) {
      return new ConditionMessage(prefix + " " + thing);
    }

    return new ConditionMessage(prefix + " " + StringUtil.pluralize(thing, objects.length) +
            " " + StringUtil.join(objects, s -> "'" + s + "'", ", "));
  }
}
