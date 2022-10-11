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

package cn.taketoday.assistant.profiles;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import cn.taketoday.assistant.model.InfraProfile;

import static cn.taketoday.assistant.InfraBundle.message;

final class InfraProfilesParser {
  private static final Predicate<Set<String>> DEFAULT = (activeProfiles) -> true;

  static Predicate<Set<String>> parse(Collection<String> expressions) {
    String expression = ContainerUtil.getOnlyItem(expressions);
    if (InfraProfile.DEFAULT_PROFILE_NAME.equals(expression)) {
      return DEFAULT;
    }
    else {
      return expression != null ? parseExpression(expression) : or(ContainerUtil.map(expressions, InfraProfilesParser::parseExpression));
    }
  }

  private static Predicate<Set<String>> parseExpression(String expression) {
    if (StringUtil.isEmptyOrSpaces(expression)) {
      throw new InfraProfilesFactory.MalformedProfileExpressionException(message("profile.expression.empty"));
    }
    else {
      StringTokenizer tokens = new StringTokenizer(expression, "()&|!", true);
      return parseTokens(tokens, InfraProfilesParser.Context.NONE);
    }
  }

  private static Predicate<Set<String>> parseTokens(StringTokenizer tokens, InfraProfilesParser.Context context) {
    ArrayList<Predicate<Set<String>>> elements = new ArrayList<>();
    InfraProfilesParser.Operator operator = null;

    while (true) {
      String token;
      do {
        if (!tokens.hasMoreTokens()) {
          return merge(elements, operator);
        }

        token = tokens.nextToken().trim();
      }
      while (token.isEmpty());

      switch (token) {
        case "(" -> {
          Predicate<Set<String>> contents = parseTokens(tokens, Context.BRACKET);
          if (context == Context.INVERT) {
            return contents;
          }
          elements.add(contents);
        }
        case "&" -> {
          assertWellFormed(operator == null || operator == Operator.AND);
          operator = Operator.AND;
        }
        case "|" -> {
          assertWellFormed(operator == null || operator == Operator.OR);
          operator = Operator.OR;
        }
        case "!" -> elements.add(parseTokens(tokens, Context.INVERT).negate());
        case ")" -> {
          Predicate<Set<String>> merged = merge(elements, operator);
          if (context == Context.BRACKET) {
            return merged;
          }
          elements.clear();
          elements.add(merged);
          operator = null;
        }
        default -> {
          Predicate<Set<String>> value = equals(token);
          if (context == Context.INVERT) {
            return value;
          }
          elements.add(value);
        }
      }
    }
  }

  private static Predicate<Set<String>> merge(List<Predicate<Set<String>>> elements, InfraProfilesParser.Operator operator) {
    assertWellFormed(!elements.isEmpty());
    if (elements.size() == 1) {
      return elements.get(0);
    }
    else {
      ArrayList<Predicate<Set<String>>> profiles = new ArrayList<>(elements);
      return operator == InfraProfilesParser.Operator.AND ? and(profiles) : or(profiles);
    }
  }

  private static void assertWellFormed(boolean wellFormed) {
    if (!wellFormed) {
      throw new InfraProfilesFactory.MalformedProfileExpressionException(message("profile.expression.malformed"));
    }
  }

  private static Predicate<Set<String>> or(List<Predicate<Set<String>>> profiles) {
    return (activeProfiles) -> {
      return profiles.stream().anyMatch((profile) -> {
        return profile.test(activeProfiles);
      });
    };
  }

  private static Predicate<Set<String>> and(List<Predicate<Set<String>>> profiles) {
    return (activeProfiles) -> {
      return profiles.stream().allMatch((profile) -> {
        return profile.test(activeProfiles);
      });
    };
  }

  private static Predicate<Set<String>> equals(String profile) {
    return activeProfiles -> activeProfiles.contains(profile);
  }

  private enum Context {
    NONE,
    INVERT,
    BRACKET;

    Context() {
    }
  }

  private enum Operator {
    AND,
    OR;

    Operator() {
    }
  }
}
