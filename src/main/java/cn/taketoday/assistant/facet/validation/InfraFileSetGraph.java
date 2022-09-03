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

package cn.taketoday.assistant.facet.validation;

import com.intellij.util.SmartList;
import com.intellij.util.graph.InboundSemiGraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import cn.taketoday.assistant.facet.InfraFileSet;

class InfraFileSetGraph implements InboundSemiGraph<InfraFileSet> {
  private final Set<InfraFileSet> fileSets;

  public InfraFileSetGraph(Set<InfraFileSet> fileSets) {
    this.fileSets = fileSets;
  }

  public Collection<InfraFileSet> getNodes() {
    return fileSets;
  }

  public Iterator<InfraFileSet> getIn(InfraFileSet node) {
    Set<InfraFileSet> dependencies = node.getDependencyFileSets();
    SmartList<InfraFileSet> smartList = new SmartList<>();
    for (InfraFileSet set : this.fileSets) {
      if (dependencies.contains(set)) {
        smartList.add(set);
      }
    }
    return smartList.iterator();
  }
}
