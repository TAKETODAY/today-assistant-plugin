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

package cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import cn.taketoday.assistant.web.mvc.jam.RequestMethod;

import static cn.taketoday.assistant.InfraAppBundle.message;

class RouterPredicateParser extends LiveRequestMappingPredicateParser {
  static LiveRequestMappingPredicateParser INSTANCE = new RouterPredicateParser();

  protected List<LiveRequestMappingPredicate> parseMapping(String mapping) {
    try {
      RouterPredicateParser.RouterPredicateExpression expression = parseExpression(mapping);
      List<RouterPredicateParser.RouterPredicateBuilder> builders = expression.eval(false);
      return ContainerUtil.mapNotNull(builders, RouterPredicateParser.RouterPredicateBuilder::build);
    }
    catch (RouterPredicateParser.RouterPredicateParserException var4) {
      RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
      builder.path = mapping;
      return new SmartList<>(builder.build());
    }
  }

  private static RouterPredicateParser.RouterPredicateExpression parseExpression(String mapping) {
    Queue<String> tokens = tokenize(mapping);
    return parseTokens(tokens, RouterPredicateParser.Context.NONE);
  }

  private static Queue<String> tokenize(String mapping) {
    var result = new ArrayDeque<String>();
    int position = 0;
    int start = 0;

    while (position < mapping.length()) {
      char next = mapping.charAt(position);
      String delimiter = null;
      if (next != '(' && next != ')' && next != '!') {
        if ((next == '&' || next == '|') && position + 1 < mapping.length()) {
          char operator = mapping.charAt(position + 1);
          if (operator == next) {
            String var10000 = String.valueOf(next);
            delimiter = var10000 + operator;
          }
        }
      }
      else {
        delimiter = String.valueOf(next);
      }

      if (delimiter != null) {
        if (position > start) {
          result.add(mapping.substring(start, position));
        }

        result.add(delimiter);
        position += delimiter.length();
        start = position;
      }
      else {
        ++position;
      }
    }

    return result;
  }

  private static RouterPredicateParser.RouterPredicateExpression parseTokens(
          Queue<String> tokens, RouterPredicateParser.Context context) {

    var elements = new ArrayList<RouterPredicateParser.RouterPredicateExpression>();
    RouterPredicateParser.Operator operator = null;

    while (!tokens.isEmpty()) {
      String token = tokens.poll().trim();
      if (!token.isEmpty()) {
        switch (token) {
          case "(" -> {
            RouterPredicateExpression contents = parseTokens(tokens,
                    Context.BRACKET);
            if (context == Context.INVERT) {
              return contents;
            }
            elements.add(contents);
          }
          case "&&" -> {
            assertWellFormed(operator == null);
            operator = Operator.AND;
          }
          case "||" -> {
            assertWellFormed(operator == null);
            operator = Operator.OR;
          }
          case "!" -> elements.add(not(parseTokens(tokens, Context.INVERT)));
          case ")" -> {
            RouterPredicateExpression merged = merge(elements, operator);
            if (context == Context.BRACKET) {
              return merged;
            }
            elements.clear();
            elements.add(merged);
            operator = null;
          }
          default -> {
            RouterPredicateExpression value = value(token);
            if (context == Context.INVERT) {
              return value;
            }
            elements.add(value);
          }
        }
      }
    }

    return merge(elements, operator);
  }

  private static RouterPredicateParser.RouterPredicateExpression merge(
          List<RouterPredicateParser.RouterPredicateExpression> elements, RouterPredicateParser.Operator operator) {
    assertWellFormed(!elements.isEmpty() && elements.size() <= 2);
    if (elements.size() == 1) {
      return elements.get(0);
    }
    else {
      return operator == RouterPredicateParser.Operator.AND
             ? and(elements.get(0), elements.get(1))
             : or(elements.get(0), elements.get(1));
    }
  }

  private static void assertWellFormed(boolean wellFormed) {
    if (!wellFormed) {
      throw new RouterPredicateParser.RouterPredicateParserException(
              message("infra.application.endpoints.mappings.malformed.router.predicate"));
    }
  }

  private static void assertMatchable(boolean matchable) {
    if (!matchable) {
      throw new RouterPredicateParser.RouterPredicateParserException(
              message("infra.application.endpoints.mappings.unmatchable.router.predicate"));
    }
  }

  private static RouterPredicateParser.RouterPredicateExpression not(
          RouterPredicateParser.RouterPredicateExpression expression) {
    return negative -> expression.eval(!negative);
  }

  private static RouterPredicateParser.RouterPredicateExpression and(
          RouterPredicateParser.RouterPredicateExpression left,
          RouterPredicateParser.RouterPredicateExpression right) {
    return (negative) -> {
      List<RouterPredicateParser.RouterPredicateBuilder> leftBuilders = left.eval(negative);
      List<RouterPredicateParser.RouterPredicateBuilder> rightBuilders = right.eval(negative);
      if (leftBuilders.isEmpty()) {
        return rightBuilders;
      }
      else if (rightBuilders.isEmpty()) {
        return leftBuilders;
      }
      else {
        var result = new SmartList<RouterPredicateParser.RouterPredicateBuilder>();
        if (!rightBuilders.isEmpty()) {
          for (RouterPredicateBuilder leftBuilder : leftBuilders) {
            for (RouterPredicateBuilder rightBuilder : rightBuilders) {
              result.add(leftBuilder.append(rightBuilder));
            }
          }
        }

        return result;
      }
    };
  }

  private static RouterPredicateParser.RouterPredicateExpression or(
          RouterPredicateParser.RouterPredicateExpression left, RouterPredicateParser.RouterPredicateExpression right) {
    return (negative) -> {
      List<RouterPredicateParser.RouterPredicateBuilder> leftBuilders = left.eval(negative);
      List<RouterPredicateParser.RouterPredicateBuilder> rightBuilders = right.eval(negative);
      if (leftBuilders.isEmpty()) {
        return rightBuilders;
      }
      else if (rightBuilders.isEmpty()) {
        return leftBuilders;
      }
      else {
        var result = new LinkedHashSet<RouterPredicateParser.RouterPredicateBuilder>();
        for (RouterPredicateBuilder leftBuilder : leftBuilders) {
          for (RouterPredicateBuilder rightBuilder : rightBuilders) {
            RouterPredicateBuilder combinedBuilder = leftBuilder.combine(rightBuilder);
            if (combinedBuilder != null) {
              result.add(combinedBuilder);
            }
            else {
              result.add(leftBuilder);
              result.add(rightBuilder);
            }
          }
        }

        return new SmartList<>(result);
      }
    };
  }

  private static boolean isPath(String token) {
    return token.startsWith("/");
  }

  private static boolean isRequestMethod(String token) {
    try {
      RequestMethod.valueOf(token);
      return true;
    }
    catch (IllegalArgumentException var2) {
      return false;
    }
  }

  private static RouterPredicateParser.RouterPredicateExpression value(String token) {
    if (isPath(token)) {
      return (negative) -> {
        assertMatchable(!negative);
        RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
        builder.path = token;
        return new SmartList<>(builder);
      };
    }
    else if (isRequestMethod(token)) {
      return (negative) -> {
        RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
        if (negative) {
          builder.requestMethods = ContainerUtil.map(RequestMethod.values(), Objects::toString);
          builder.requestMethods.remove(token);
        }
        else {
          builder.requestMethods = new SmartList<>(token);
        }

        return new SmartList<>(builder);
      };
    }
    else {
      String consumes;
      if (token.startsWith("Accept: ")) {
        consumes = token.substring("Accept: ".length()).trim();
        return (negative) -> {
          RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
          builder.produces = new SmartList<>(negative ? "!" + consumes : consumes);
          return new SmartList<>(builder);
        };
      }
      else if (token.startsWith("Content-Type: ")) {
        consumes = token.substring("Content-Type: ".length()).trim();
        return (negative) -> {
          RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
          builder.consumes = new SmartList<>(negative ? "!" + consumes : consumes);
          return new SmartList<>(builder);
        };
      }
      else {
        int headerSeparator = token.indexOf(58);
        if (headerSeparator > 0 && headerSeparator < token.length() - 1) {
          Pair<String, String> header = Pair.create(token.substring(0, headerSeparator).trim(), token.substring(headerSeparator + 1).trim());
          if (!StringUtil.isEmpty(header.first) && !StringUtil.isEmpty(header.second)) {
            return (negative) -> {
              RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
              builder.headers = new SmartList<>(negative ? Pair.create("!" + header.first, header.second) : header);
              return new SmartList<>(builder);
            };
          }
        }

        int paramSeparator = token.indexOf(' ');
        if (token.startsWith("?") && paramSeparator > 1 && paramSeparator < token.length() - 1) {
          Pair<String, String> param = Pair.create(token.substring(1, paramSeparator).trim(), token.substring(paramSeparator + 1).trim());
          if (!StringUtil.isEmpty(param.first) && !StringUtil.isEmpty(param.second)) {
            return (negative) -> {
              RouterPredicateParser.RouterPredicateBuilder builder = new RouterPredicateParser.RouterPredicateBuilder();
              builder.params = new SmartList<>(negative ? Pair.create("!" + param.first, param.second) : param);
              return new SmartList<>(builder);
            };
          }
        }

        return (negative) -> {
          return new SmartList();
        };
      }
    }
  }

  private static class RouterPredicateParserException extends RuntimeException {
    RouterPredicateParserException(String message) {
      super(message);
    }
  }

  private interface RouterPredicateExpression {
    List<RouterPredicateParser.RouterPredicateBuilder> eval(boolean var1);
  }

  private static class RouterPredicateBuilder {
    String path;
    List<String> requestMethods;
    List<Pair<String, String>> headers;
    List<String> produces;
    List<String> consumes;
    List<Pair<String, String>> params;

    RouterPredicateBuilder() {
    }

    RouterPredicateBuilder(RouterPredicateParser.RouterPredicateBuilder builder) {
      this.path = builder.path;
      this.requestMethods = builder.requestMethods;
      this.headers = builder.headers;
      this.produces = builder.produces;
      this.consumes = builder.consumes;
      this.params = builder.params;
    }

    LiveRequestMappingPredicate build() {
      return this.path == null
             ? null
             : new LiveRequestMappingPredicate(this.path, this.requestMethods == null
                                                          ? Collections.emptyList() : this.requestMethods,
                     this.headers == null ? Collections.emptyList() : this.headers, this.produces == null ? Collections.emptyList() : this.produces,
                     this.consumes == null ? Collections.emptyList() : this.consumes, this.params == null ? Collections.emptyList() : this.params);
    }

    RouterPredicateParser.RouterPredicateBuilder append(RouterPredicateParser.RouterPredicateBuilder builder) {

      RouterPredicateParser.RouterPredicateBuilder commonBuilder = new RouterPredicateParser.RouterPredicateBuilder(this);
      if (commonBuilder.path == null) {
        commonBuilder.path = builder.path;
      }
      else {
        assertEqual(commonBuilder.path, builder.path);
      }

      if (commonBuilder.requestMethods == null) {
        commonBuilder.requestMethods = builder.requestMethods;
      }
      else {
        assertEqual(commonBuilder.requestMethods, builder.requestMethods);
      }

      if (commonBuilder.produces == null) {
        commonBuilder.produces = builder.produces;
      }
      else {
        assertEqual(commonBuilder.produces, builder.produces);
      }

      if (commonBuilder.consumes == null) {
        commonBuilder.consumes = builder.consumes;
      }
      else {
        assertEqual(commonBuilder.consumes, builder.consumes);
      }

      if (commonBuilder.headers == null) {
        commonBuilder.headers = builder.headers;
      }
      else if (builder.headers != null) {
        commonBuilder.headers.addAll(builder.headers);
      }

      if (commonBuilder.params == null) {
        commonBuilder.params = builder.params;
      }
      else if (builder.params != null) {
        commonBuilder.params.addAll(builder.params);
      }

      return commonBuilder;
    }

    RouterPredicateParser.RouterPredicateBuilder combine(
            RouterPredicateParser.RouterPredicateBuilder builder) {
      if (Objects.equals(this.path, builder.path) && Comparing.equal(this.headers, builder.headers) && Comparing.equal(this.params, builder.params)) {
        RouterPredicateParser.RouterPredicateBuilder commonBuilder = new RouterPredicateParser.RouterPredicateBuilder(
                this);
        if (commonBuilder.requestMethods == null) {
          commonBuilder.requestMethods = builder.requestMethods;
        }
        else if (builder.requestMethods != null) {
          commonBuilder.requestMethods.addAll(builder.requestMethods);
        }

        if (commonBuilder.produces == null) {
          commonBuilder.produces = builder.produces;
        }
        else if (builder.produces != null) {
          commonBuilder.produces.addAll(builder.produces);
        }

        if (commonBuilder.consumes == null) {
          commonBuilder.consumes = builder.consumes;
        }
        else if (builder.consumes != null) {
          commonBuilder.consumes.addAll(builder.consumes);
        }

        return commonBuilder;
      }
      else {
        return null;
      }
    }

    private static void assertEqual(Object left, Object right) {
      assertMatchable(right == null || right.equals(left));
    }
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
