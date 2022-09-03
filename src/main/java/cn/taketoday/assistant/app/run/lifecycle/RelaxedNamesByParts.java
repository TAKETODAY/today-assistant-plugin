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

package cn.taketoday.assistant.app.run.lifecycle;

import com.intellij.microservices.jvm.config.RelaxedNames;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class RelaxedNamesByParts {

  static Set<String> generateRelaxedNamesByParts(String source) {
    Set<String> variations = new LinkedHashSet<>();
    ContainerUtil.addAll(variations, new RelaxedNames(source));
    List<String> parts = StringUtil.split(source, ".");
    List<RelaxedNames> relaxedNamesParts = parts.stream().map(RelaxedNames::new).collect(Collectors.toCollection(ArrayList::new));
    addVariations(variations, relaxedNamesParts);
    return variations;
  }

  private static void addVariations(Set<String> names, List<RelaxedNames> relaxedNamesParts) {
    if (relaxedNamesParts.isEmpty()) {
      return;
    }
    RelaxedNames prefixes = relaxedNamesParts.remove(0);
    if (relaxedNamesParts.isEmpty()) {
      ContainerUtil.addAll(names, prefixes);
      return;
    }
    Set<String> suffixes = new LinkedHashSet<>();
    addVariations(suffixes, relaxedNamesParts);
    for (String prefix : prefixes) {
      for (String suffix : suffixes) {
        names.add(prefix + "." + suffix);
        names.add(prefix + "_" + suffix);
      }
    }
  }
}
