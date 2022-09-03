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

import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveDispatcherServlet;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMapping;
import cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.LiveRequestMappingsModel;
import cn.taketoday.lang.Nullable;

public class LiveRequestMappingsParser {
  private static final String CONTEXTS_KEY = "contexts";
  private static final String MAPPINGS_KEY = "mappings";
  private static final String DISPATCHER_SERVLETS_KEY = "dispatcherServlets";
  private static final String DISPATCHER_HANDLERS_KEY = "dispatcherHandlers";
  private static final String HANDLER_ATTRIBUTE = "handler";
  private static final String PREDICATE_ATTRIBUTE = "predicate";
  private static final String SERVLETS_KEY = "servlets";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String DETAILS_ATTRIBUTE = "details";
  private static final String HANDLER_METHOD_ATTRIBUTE = "handlerMethod";
  private static final String CLASS_NAME_ATTRIBUTE = "className";
  private static final String DESCRIPTOR_ATTRIBUTE = "descriptor";
  private static final String BEAN_ATTRIBUTE = "bean";
  private static final String METHOD_ATTRIBUTE = "method";

  public LiveRequestMappingsModel parse(Map<?, ?> mappings) {
    if (mappings == null) {
      return new LiveRequestMappingsModelImpl(Collections.emptyList());
    }
    Object contexts = mappings.get(CONTEXTS_KEY);
    if ((contexts instanceof Map) && mappings.size() == 1) {
      return parseSB20((Map) contexts);
    }
    List<LiveRequestMapping> liveMappings = new ArrayList<>();
    mappings.forEach((key, details) -> {
      if ((key instanceof String) && (details instanceof Map)) {
        liveMappings.addAll(parseEntry((String) key, (Map) details));
      }
    });
    return new LiveRequestMappingsModelImpl(liveMappings);
  }

  private static List<LiveRequestMapping> parseEntry(String mappingKey, Map<?, ?> detailsMap) {
    Object beanValue = detailsMap.get(BEAN_ATTRIBUTE);
    String bean = beanValue == null ? null : beanValue.toString();
    Object methodValue = detailsMap.get(METHOD_ATTRIBUTE);
    String method = methodValue == null ? null : methodValue.toString();
    return getMappings(mappingKey, bean, method, cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl.LiveDispatcherServletImpl.DEFAULT);
  }

  private static List<LiveRequestMapping> getMappings(String mappingKey, @Nullable String bean, @Nullable String method, LiveDispatcherServlet dispatcherServlet) {
    List<LiveRequestMappingPredicate> predicates = LiveRequestMappingPredicateParser.parse(mappingKey);
    return ContainerUtil.map(predicates, predicate -> {
      return new LiveRequestMappingImpl(mappingKey, predicate, bean, method, dispatcherServlet);
    });
  }

  private static LiveRequestMappingsModel parseSB20(Map<?, ?> contexts) {
    List<LiveRequestMapping> liveMappings = new ArrayList<>();
    contexts.forEach((contextKey, contextDetails) -> {
      if (!(contextKey instanceof String) || !(contextDetails instanceof Map)) {
        return;
      }
      Object mappingsObject = ((Map) contextDetails).get(MAPPINGS_KEY);
      if (!(mappingsObject instanceof Map)) {
        return;
      }
      Map<?, ?> mappings = (Map) mappingsObject;
      Map<String, LiveDispatcherServlet> liveDispatcherServlets = new HashMap<>();
      Object servlets = mappings.get(SERVLETS_KEY);
      if (servlets instanceof List) {
        ((List) servlets).forEach(servletObject -> {
          Map<?, ?> servlet;
          Object name;
          if ((servletObject instanceof Map) && (name = (servlet = (Map) servletObject).get("name")) != null) {
            Object servletMappingsObject = servlet.get(MAPPINGS_KEY);
            if (servletMappingsObject instanceof List) {
              List<String> servletMappings = (List) ((List) servletMappingsObject).stream()
                      .filter(Objects::nonNull)
                      .map(Object::toString)
                      .collect(Collectors.toCollection(SmartList::new));
              liveDispatcherServlets.put(name.toString(), new cn.taketoday.assistant.app.mvc.lifecycle.mappings.model.impl.LiveDispatcherServletImpl(name.toString(), servletMappings));
            }
          }
        });
      }
      parseSB20Dispatchers(liveMappings, liveDispatcherServlets, mappings.get(DISPATCHER_SERVLETS_KEY));
      parseSB20Dispatchers(liveMappings, new HashMap(), mappings.get(DISPATCHER_HANDLERS_KEY));
    });
    return new LiveRequestMappingsModelImpl(liveMappings);
  }

  private static void parseSB20Dispatchers(List<LiveRequestMapping> liveMappings, Map<String, LiveDispatcherServlet> liveDispatcherServlets, Object dispatchers) {
    if (!(dispatchers instanceof Map)) {
      return;
    }
    ((Map) dispatchers).forEach((dispatcherKey, dispatcherDetails) -> {
      if (!(dispatcherKey instanceof String name) || !(dispatcherDetails instanceof List)) {
        return;
      }
      LiveDispatcherServlet liveDispatcherServlet = liveDispatcherServlets.computeIfAbsent(name, key -> {
        return new LiveDispatcherServletImpl(name, Collections.emptyList());
      });
      ((List) dispatcherDetails).forEach(mapping -> {
        if (!(mapping instanceof Map)) {
          return;
        }
        liveMappings.addAll(parseSB20Entry((Map) mapping, liveDispatcherServlet));
      });
    });
  }

  private static List<LiveRequestMapping> parseSB20Entry(Map<?, ?> detailsMap, LiveDispatcherServlet dispatcherServlet) {
    Object predicateValue = detailsMap.get(PREDICATE_ATTRIBUTE);
    if (predicateValue == null) {
      List<LiveRequestMapping> emptyList = Collections.emptyList();
      return emptyList;
    }
    String mappingKey = predicateValue.toString();
    String method = getHandlerMethod(detailsMap);
    if (method == null) {
      Object handlerValue = detailsMap.get(HANDLER_ATTRIBUTE);
      String handler = handlerValue != null ? handlerValue.toString() : null;
      method = (handler == null || handler.contains("[")) ? null : handler;
    }
    return getMappings(mappingKey, null, method, dispatcherServlet);
  }

  private static String getHandlerMethod(Map<?, ?> mapping) {
    Map<?, ?> handlerMethodMap;
    Object className;
    String parameters;
    Object details = mapping.get(DETAILS_ATTRIBUTE);
    if (!(details instanceof Map)) {
      return null;
    }
    Object handlerMethod = ((Map) details).get(HANDLER_METHOD_ATTRIBUTE);
    if (!(handlerMethod instanceof Map) || (className = (handlerMethodMap = (Map) handlerMethod).get(CLASS_NAME_ATTRIBUTE)) == null) {
      return null;
    }
    StringBuilder result = new StringBuilder(className.toString());
    Object methodName = handlerMethodMap.get("name");
    if (methodName == null) {
      return null;
    }
    result.append('.').append(methodName).append('(');
    Object descriptor = handlerMethodMap.get(DESCRIPTOR_ATTRIBUTE);
    if (!(descriptor instanceof String) || (parameters = convertParametersDescriptor((String) descriptor)) == null) {
      return null;
    }
    result.append(parameters).append(')');
    return result.toString();
  }

  private static String convertParametersDescriptor(String descriptor) {
    int end;
    if (descriptor.startsWith("(") && (end = descriptor.indexOf(41)) >= 0) {
      String descriptor2 = descriptor.substring(1, end);
      StringBuilder result = new StringBuilder();
      int start = 0;
      while (start < descriptor2.length()) {
        if (start != 0) {
          result.append(", ");
        }
        char parameter = descriptor2.charAt(start);
        int arrayDimension = 0;
        while (parameter == '[') {
          arrayDimension++;
          start++;
          parameter = descriptor2.charAt(start);
        }
        if (parameter == 'L') {
          int typeEnd = descriptor2.indexOf(59, start);
          if (typeEnd < 0) {
            return null;
          }
          String type = descriptor2.substring(start + 1, typeEnd);
          start = typeEnd;
          result.append(type.replaceAll("/", "."));
        }
        else {
          String primitive = mapPrimitive(parameter);
          if (primitive == null) {
            return null;
          }
          result.append(primitive);
        }
        result.append("[]".repeat(arrayDimension));
        start++;
      }
      return result.toString();
    }
    return null;
  }

  private static String mapPrimitive(char primitive) {
    return switch (primitive) {
      case 'B' -> "byte";
      case 'C' -> "char";
      case 'D' -> "double";
      case 'F' -> "float";
      case 'I' -> "int";
      case 'J' -> "long";
      case 'S' -> "short";
      case 'Z' -> "boolean";
      default -> null;
    };
  }
}
