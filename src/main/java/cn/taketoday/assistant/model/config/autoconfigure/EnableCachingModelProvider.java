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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.util.PairProcessor;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModelDependentModelsProvider;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.graph.LocalModelDependency;
import cn.taketoday.assistant.context.model.graph.LocalModelDependencyType;
import cn.taketoday.assistant.util.InfraUtils;

final class EnableCachingModelProvider extends LocalAnnotationModelDependentModelsProvider {
  private static final Map<String, String> CACHE_CONFIGURATION;

  static {
    Map<String, String> cacheConfigurations = new LinkedHashMap<>();
    cacheConfigurations.put(InfraCacheClassesConstant.CAFFEINE_CACHE_CONFIGURATION, "com.github.benmanes.caffeine.cache.Caffeine");
    cacheConfigurations.put(InfraCacheClassesConstant.SIMPLE_CACHE_CONFIGURATION, "cn.taketoday.cache.CacheManager");
    cacheConfigurations.put(InfraCacheClassesConstant.GENERIC_CACHE_CONFIGURATION, "cn.taketoday.cache.Cache");
    cacheConfigurations.put(InfraCacheClassesConstant.JCACHE_CACHE_CONFIGURATION, "cn.taketoday.cache.jcache.JCacheCacheManager");
    cacheConfigurations.put(InfraCacheClassesConstant.EL_CACHE_CACHE_CONFIGURATION, "cn.taketoday.cache.Cache");
    CACHE_CONFIGURATION = cacheConfigurations;
  }

  public boolean processCustomDependentLocalModels(LocalAnnotationModel localAnnotationModel,
          PairProcessor<? super LocalModel, ? super LocalModelDependency> processor) {
    PsiClass cacheConfiguration;
    Module module = localAnnotationModel.getModule();
    if (InfraCacheClassesConstant.CACHE_CONFIGURATION_IMPORT_SELECTOR.equals(localAnnotationModel.getConfig().getQualifiedName())) {
      for (Map.Entry<String, String> classNames : CACHE_CONFIGURATION.entrySet()) {
        if (InfraUtils.findLibraryClass(module, classNames.getValue()) != null
                && (cacheConfiguration = InfraUtils.findLibraryClass(module, classNames.getKey())) != null) {
          LocalAnnotationModel model = LocalModelFactory.of().getOrCreateLocalAnnotationModel(cacheConfiguration, module, localAnnotationModel.getActiveProfiles());
          return processor.process(model, LocalModelDependency.create(LocalModelDependencyType.IMPORT, cacheConfiguration));
        }
      }
      return true;
    }
    return true;
  }
}
