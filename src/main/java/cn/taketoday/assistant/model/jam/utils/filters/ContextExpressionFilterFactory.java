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

package cn.taketoday.assistant.model.jam.utils.filters;

import com.intellij.util.Function;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.assistant.model.xml.context.Filter;
import cn.taketoday.assistant.model.xml.context.Type;
import cn.taketoday.lang.Nullable;

public final class ContextExpressionFilterFactory {
  private static final Map<Type, Function<String, InfraContextFilter.Exclude>> excludeFilters = new HashMap();
  private static final Map<Type, Function<String, InfraContextFilter.Include>> includeFilters = new HashMap();

  static {
    registerExcludeFilters();
    registerIncludeFilters();
  }

  private static void registerIncludeFilters() {
    includeFilters.put(Type.ANNOTATION, InfraContextIncludeAnnotationFilter::new);
    includeFilters.put(Type.ASSIGNABLE, InfraContextIncludeAssignableFilter::new);
    includeFilters.put(Type.REGEX, InfraContextIncludeRegexFilter::new);
    includeFilters.put(Type.ASPECTJ, InfraContextIncludeAspectJFilter::new);
  }

  private static void registerExcludeFilters() {
    excludeFilters.put(Type.ANNOTATION, InfraContextExcludeAnnotationFilter::new);
    excludeFilters.put(Type.ASSIGNABLE, InfraContextExcludeAssignableFilter::new);
    excludeFilters.put(Type.REGEX, InfraContextExcludeRegexpFilter::new);
    excludeFilters.put(Type.ASPECTJ, InfraContextExcludeAspectJFilter::new);
  }

  public static InfraContextFilter.Exclude createExcludeFilter(Filter filter) {
    Type type = filter.getType().getValue();
    String expression = filter.getExpression().getStringValue();
    return createExcludeFilter(type, expression);
  }

  public static InfraContextFilter.Include createIncludeFilter(Filter filter) {
    Type type = filter.getType().getValue();
    String expression = filter.getExpression().getStringValue();
    return createIncludeFilter(type, expression);
  }

  public static InfraContextFilter.Exclude createExcludeFilter(@Nullable Type type, @Nullable String expression) {
    if (excludeFilters.containsKey(type)) {
      return excludeFilters.get(type).fun(expression);
    }
    return InfraContextFilter.Exclude.EMPTY_EXCLUDE;
  }

  public static InfraContextFilter.Include createIncludeFilter(@Nullable Type type, @Nullable String expression) {
    if (includeFilters.containsKey(type)) {
      return includeFilters.get(type).fun(expression);
    }
    return InfraContextFilter.Include.EMPTY_INCLUDE;
  }
}
