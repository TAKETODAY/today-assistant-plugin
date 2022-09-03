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

public final class ConditionOutcome {

  private final boolean myMatch;

  private final ConditionMessage myMessage;

  private ConditionOutcome(boolean match, ConditionMessage message) {
    myMatch = match;
    myMessage = message;
  }

  public boolean isMatch() {
    return myMatch;
  }

  public ConditionMessage getMessage() {
    return myMessage;
  }

  public static ConditionOutcome match(String message) {
    return match(new ConditionMessage(message));
  }

  public static ConditionOutcome match(ConditionMessage message) {
    return new ConditionOutcome(true, message);
  }

  public static ConditionOutcome noMatch(String message) {
    return noMatch(new ConditionMessage(message));
  }

  public static ConditionOutcome noMatch(ConditionMessage message) {
    return new ConditionOutcome(false, message);
  }
}
