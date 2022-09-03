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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.lang.Nullable;

abstract class LiveRequestMappingPredicateParser {
  private static final String REQUEST_METHODS = "methods";
  private static final String PRODUCES = "produces";
  private static final String CONSUMES = "consumes";
  private static final String HEADERS = "headers";
  private static final String PARAMS = "params";

  protected abstract List<LiveRequestMappingPredicate> parseMapping(String str);

  static List<LiveRequestMappingPredicate> parse(String mapping) {
    return getParser(mapping).parseMapping(mapping);
  }

  private static LiveRequestMappingPredicateParser getParser(String mapping) {
    return (mapping.startsWith("(") || mapping.startsWith("!")) ? RouterPredicateParser.INSTANCE : !mapping.startsWith("{") ? SimplePredicateParser.INSTANCE : mapping.startsWith(
            "{[/") ? MappingPredicateParser.INSTANCE : MappingPredicateParser211.INSTANCE;
  }

  private abstract static class WebPredicateParserBase extends LiveRequestMappingPredicateParser {

    abstract List<String> getPaths(String str);

    @Nullable
    protected abstract String getPartPattern();

    @Override

    protected List<LiveRequestMappingPredicate> parseMapping(String mapping) {
      List<String> paths = getPaths(mapping);
      if (paths.isEmpty()) {
        List<LiveRequestMappingPredicate> emptyList = Collections.emptyList();
        return emptyList;
      }
      List<String> requestMethods = getRequestMethods(mapping);
      List<Pair<String, String>> headers = getHeaders(mapping);
      List<String> produces = getProduces(mapping);
      List<String> consumes = getConsumes(mapping);
      List<Pair<String, String>> params = getParams(mapping);
      return ContainerUtil.map(paths, path -> {
        return new LiveRequestMappingPredicate(path, requestMethods, headers, produces, consumes, params);
      });
    }

    protected List<String> getRequestMethods(String mapping) {
      return parsePartValues(mapping, getPartPattern(), REQUEST_METHODS, "||", Function.identity());
    }

    private List<Pair<String, String>> getParams(String mapping) {
      return parsePartValues(mapping, getPartPattern(), PARAMS, "&&", WebPredicateParserBase::parseKeyValuePair);
    }

    private List<Pair<String, String>> getHeaders(String mapping) {
      return parsePartValues(mapping, getPartPattern(), HEADERS, "&&", WebPredicateParserBase::parseKeyValuePair);
    }

    private List<String> getProduces(String mapping) {
      return parsePartValues(mapping, getPartPattern(), PRODUCES, "||", Function.identity());
    }

    private List<String> getConsumes(String mapping) {
      return parsePartValues(mapping, getPartPattern(), CONSUMES, "||", Function.identity());
    }

    private static <P> List<P> parsePartValues(String mapping, @Nullable String partPattern, String part, String separator, Function<String, P> mapper) {
      int end;
      if (partPattern == null) {
        List<P> emptyList = Collections.emptyList();
        return emptyList;
      }
      String partPrefix = String.format(partPattern, part);
      int partIndex = mapping.indexOf(partPrefix);
      if (partIndex >= 0 && (end = mapping.indexOf(93, partIndex)) > partIndex) {
        String values = mapping.substring(partIndex + partPrefix.length(), end);
        return Arrays.stream(values.split(Pattern.quote(separator))).map(String::trim).map(mapper).collect(Collectors.toList());
      }
      return Collections.emptyList();
    }

    private static Pair<String, String> parseKeyValuePair(String keyValue) {
      int separatorIndex = keyValue.indexOf('=');
      if (separatorIndex >= 0) {
        boolean negative = separatorIndex > 0 && keyValue.charAt(separatorIndex - 1) == '!';
        String key = negative ? keyValue.substring(0, separatorIndex - 1) : keyValue.substring(0, separatorIndex);
        String value = separatorIndex < keyValue.length() - 1 ? keyValue.substring(separatorIndex + 1) : "";
        if (negative) {
          value = "!" + value;
        }
        return Pair.create(key, value);
      }
      return Pair.create(keyValue, "");
    }
  }

  private static class SimplePredicateParser extends WebPredicateParserBase {
    static LiveRequestMappingPredicateParser INSTANCE = new SimplePredicateParser();

    @Override
    List<String> getPaths(String mapping) {
      return new SmartList(mapping);
    }

    @Override
    @Nullable
    protected String getPartPattern() {
      return null;
    }
  }

  private static class MappingPredicateParser extends WebPredicateParserBase {
    static LiveRequestMappingPredicateParser INSTANCE;

    static {
      INSTANCE = new MappingPredicateParser();
    }

    @Override
    List<String> getPaths(String mapping) {
      int end = mapping.indexOf(93);
      if (end < 2) {
        return new SmartList<>(mapping);
      }
      return ContainerUtil.map(StringUtil.split(mapping.substring(2, end), "||"), String::trim);
    }

    @Override
    protected String getPartPattern() {
      return ",%s=[";
    }
  }

  private static class MappingPredicateParser211 extends WebPredicateParserBase {
    static LiveRequestMappingPredicateParser INSTANCE;

    static {
      INSTANCE = new MappingPredicateParser211();
    }

    @Override
    List<String> getPaths(String mapping) {
      String paths = extractPaths(mapping);
      if (paths == null) {
        return new SmartList<>(mapping);
      }
      return StringUtil.split(paths, ", ");
    }

    @Override
    protected String getPartPattern() {
      return ", %s [";
    }

    @Override
    protected List<String> getRequestMethods(String mapping) {
      if (mapping.startsWith("{[")) {
        int end = mapping.indexOf(93);
        if (end < 2) {
          return Collections.emptyList();
        }
        String methodsPart = mapping.substring(2, end);
        return Arrays.asList(methodsPart.split(Pattern.quote(", ")));
      }
      int end2 = mapping.indexOf(32);
      if (end2 >= 2) {
        return new SmartList<>(mapping.substring(1, end2));
      }
      return Collections.emptyList();
    }

    @Nullable
    private static String extractPaths(String mapping) {
      String methodsEndMarker;
      if (mapping.startsWith("{[")) {
        methodsEndMarker = "] ";
      }
      else {
        methodsEndMarker = " ";
      }
      int methodsEnd = mapping.indexOf(methodsEndMarker);
      if (methodsEnd < 0) {
        return null;
      }
      int start = methodsEnd + methodsEndMarker.length();
      if (start == mapping.length()) {
        return null;
      }
      if (mapping.charAt(start) == '[') {
        int start2 = start + 1;
        if (start2 == mapping.length()) {
          return null;
        }
        int end = mapping.indexOf("], ", start2);
        if (end < 0) {
          end = mapping.length() - 2;
          if (end < start2) {
            return null;
          }
        }
        return mapping.substring(start2, end);
      }
      int end2 = mapping.indexOf(", ", start);
      if (end2 < 0) {
        end2 = mapping.length() - 1;
        if (end2 < start) {
          return null;
        }
      }
      return mapping.substring(start, end2);
    }
  }
}
