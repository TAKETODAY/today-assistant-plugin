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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ConcurrentFactoryMap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.model.utils.ProfileUtils;

public class LazyModelDependenciesGraph extends AbstractModelDependenciesGraph {
  private static final Key<CachedValue<Map<String, LazyModelDependenciesGraph>>> MODELS_GRAPH_KEY = Key.create("MODELS_GRAPH_KEY");

  @Override
  protected Collection<Pair<LocalModel<?>, LocalModelDependency>> getDependencies(LocalModel<?> localModel) {
    return getDependencies2(localModel);
  }

  public LazyModelDependenciesGraph(Module module, Set<String> profiles) {
    super(module, profiles);
  }

  public Collection<LocalModel<?>> getNodes() {
    throw new UnsupportedOperationException();
  }

  protected Collection<Pair<LocalModel<?>, LocalModelDependency>> getDependencies2(LocalModel model) {
    return model.getDependentLocalModels();
  }

  public static LazyModelDependenciesGraph getOrCreateLocalModelDependenciesGraph(Module module, Set<String> activeProfiles) {
    String key = ProfileUtils.profilesAsString(activeProfiles);
    Map<String, LazyModelDependenciesGraph> graphsMap = CachedValuesManager.getManager(module.getProject())
            .getCachedValue(module, MODELS_GRAPH_KEY, createGraphProvider(module, activeProfiles), false);
    return graphsMap.get(key);
  }

  private static CachedValueProvider<Map<String, LazyModelDependenciesGraph>> createGraphProvider(Module module, Set<String> activeProfiles) {
    return () -> {
      Map<String, LazyModelDependenciesGraph> map = ConcurrentFactoryMap.createMap(key -> new LazyModelDependenciesGraph(module, activeProfiles));
      return CachedValueProvider.Result.create(map, getDependencies(module.getProject()));
    };
  }

  private static Object[] getDependencies(Project project) {
    Set<Object> set = new LinkedHashSet<>();
    InfraModificationTrackersManager infraModificationTrackersManager = InfraModificationTrackersManager.from(project);
    set.add(infraModificationTrackersManager.getProfilesModificationTracker());
    set.add(PsiModificationTracker.MODIFICATION_COUNT);
    set.add(infraModificationTrackersManager.getCustomBeanParserModificationTracker());
    set.add(ProjectRootManager.getInstance(project));
    return set.toArray();
  }
}
