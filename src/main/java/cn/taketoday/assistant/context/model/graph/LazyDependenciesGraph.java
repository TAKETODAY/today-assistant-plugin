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

package cn.taketoday.assistant.context.model.graph;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.psi.util.CachedValue;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.Graph;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LazyDependenciesGraph<N, E> extends UserDataHolderBase implements Graph<N> {
  private static final Key<CachedValue<Map<String, LazyModelDependenciesGraph>>> OUTS_KEY = Key.create("OUTS_KEY");
  private final Map<N, Collection<Pair<N, E>>> myIns = new ConcurrentHashMap<>();
  private final Map<N, Collection<Pair<N, E>>> myOuts = new ConcurrentHashMap<>();
  private volatile boolean myIsBuilt = false;
  private final Function<Pair<N, E>, N> mySourceNodeFunction = pair -> {
    return pair.getFirst();
  };

  protected abstract Collection<Pair<N, E>> getDependencies(N n);

  public Iterator<N> getIn(N n) {
    guaranteeGraphIsBuilt();
    Collection<Pair<N, E>> dependencies = this.myIns.get(n);
    return dependencies != null ? getIterator(dependencies) : Collections.emptyIterator();
  }

  public Iterator<N> getOut(N n) {
    return getIterator(getOrCreateOutDependencies(n));
  }

  private Iterator<N> getIterator(Collection<Pair<N, E>> dependencies) {
    return ContainerUtil.map(dependencies, this.mySourceNodeFunction).iterator();
  }

  protected final void guaranteeGraphIsBuilt() {
    if (!this.myIsBuilt) {
      Collection<N> nodes = getNodes();
      for (N node : nodes) {
        getOrCreateOutDependencies(node);
      }
      this.myIsBuilt = true;
    }
  }

  public final Collection<Pair<N, E>> getOrCreateOutDependencies(N n) {
    if (this.myOuts.get(n) == null) {
      Collection<Pair<N, E>> outSet = getDependencies(n);
      for (Pair<N, E> dependency : outSet) {
        addInDependency(n, dependency.first, dependency.second);
      }
      this.myOuts.put(n, outSet);
    }
    return this.myOuts.get(n);
  }

  public void addInDependency(N from, N to, E edgeDescriptor) {
    Collection<Pair<N, E>> inNodes = this.myIns.get(to);
    if (inNodes == null) {
      inNodes = new LinkedHashSet<>();
    }
    inNodes.add(Pair.create(from, edgeDescriptor));
    this.myIns.put(to, inNodes);
  }
}
