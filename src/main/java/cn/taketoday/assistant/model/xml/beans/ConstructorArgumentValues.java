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
package cn.taketoday.assistant.model.xml.beans;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.util.xml.DomUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class ConstructorArgumentValues {

  private static final Comparator<ConstructorArg> ARG_COMPARATOR =
          (o1, o2) -> Comparing.compare(DomUtil.hasXml(o2.getType()), DomUtil.hasXml(o1.getType()));

  private Map<String, ConstructorArgDefinition> namedArgs;
  private Map<Integer, ConstructorArgDefinition> indexedArgs;
  private List<ConstructorArg> genericArgs;

  public int init(final InfraBean bean) {
    final Set<ConstructorArg> args = bean.getAllConstructorArgs();

    indexedArgs = new HashMap<>(args.size());
    genericArgs = new ArrayList<>(args.size());
    namedArgs = new HashMap<>();

    int minNrOfArgs = args.size();

    for (CNamespaceDomElement definition : bean.getCNamespaceConstructorArgDefinitions()) {
      final String name = definition.getAttributeName();
      if (StringUtil.isEmptyOrSpaces(name))
        continue;

      if (!definition.isIndexAttribute()) {
        namedArgs.put(name, definition);
      }
      else {
        Integer index = definition.getIndex();
        if (index != null) {
          indexedArgs.put(index, definition);
        }
      }
    }

    for (ConstructorArg arg : args) {
      final Integer index = arg.getIndex().getValue();
      if (index != null) {
        indexedArgs.put(index, arg);
        minNrOfArgs = Math.max(minNrOfArgs, index);
      }
      else if (DomUtil.hasXml(arg.getNameAttr())) {
        final String name = arg.getNameAttr().getStringValue();
        if (!StringUtil.isEmptyOrSpaces(name)) {
          namedArgs.put(name, arg);
        }
      }
      else {
        genericArgs.add(arg);
      }
    }
    genericArgs.sort(ARG_COMPARATOR);
    return minNrOfArgs;
  }

  @Nullable
  public ConstructorArgDefinition resolve(final int index,
          PsiParameter parameter,
          @Nullable Set<ConstructorArgDefinition> usedArgs) {
    final PsiType paramType = parameter.getType();

    if (!namedArgs.isEmpty()) {
      ConstructorArgDefinition arg = resolveNamed(parameter.getName(), paramType);
      if (arg != null)
        return arg;
    }

    ConstructorArgDefinition arg = resolveIndexed(index, paramType);
    if (arg != null)
      return arg;

    return resolveGeneric(paramType, usedArgs);
  }

  @Nullable
  public ConstructorArg resolveGeneric(@Nullable final PsiType requiredType, @Nullable Set<ConstructorArgDefinition> usedArgs) {
    for (final ConstructorArg arg : genericArgs) {
      if (usedArgs != null && usedArgs.contains(arg)) {
        continue;
      }
      final PsiType type = arg.getType().getValue();
      if (requiredType == null) {
        if (type == null) {
          return arg;
        }
      }
      else {
        if (type != null) {
          if (requiredType.isAssignableFrom(type)) {
            return arg;
          }
        }
        else if (usedArgs == null || arg.isAssignable(requiredType)) {
          return arg;
        }
      }
    }
    return null;
  }

  @Nullable
  private ConstructorArgDefinition resolveIndexed(int index, final PsiType paramType) {
    final ConstructorArgDefinition arg = indexedArgs.get(index);

    // here are resolved c:namespace indexed arguments
    //  <bean id="foo" class="x.y.Foo" c:_0-ref="bar" c:_1-ref="baz" c:_2="foo@bar.com">
    if (!(arg instanceof ConstructorArg)) {
      return arg;
    }

    final PsiType type = ((ConstructorArg) arg).getType().getValue();
    if (type == null || type.isAssignableFrom(paramType)) {
      return arg;
    }
    return null;
  }

  @Nullable
  private ConstructorArgDefinition resolveNamed(String name, final PsiType paramType) {
    final ConstructorArgDefinition arg = namedArgs.get(name);
    if (arg == null) {
      return null;
    }

    // here are resolved c:namespace named arguments
    //  <bean id="foo" class="x.y.Foo" c:bar-ref="bar" c:baz-ref="baz" c:email="foo@bar.com">
    if (!(arg instanceof ConstructorArg)) {
      return arg;
    }

    // user can use both "name" and "type" attributes... it isn't recommended but possible
    // <constructor-arg name="list" type="my.Type" ... />
    final PsiType type = ((ConstructorArg) arg).getType().getValue();
    if (type == null || type.isAssignableFrom(paramType)) {
      return arg;
    }
    return null;
  }
}
