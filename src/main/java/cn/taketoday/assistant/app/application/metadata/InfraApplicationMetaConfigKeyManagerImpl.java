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

package cn.taketoday.assistant.app.application.metadata;

import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigFileConstants;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraApplicationMetaConfigKeyManagerImpl extends InfraApplicationMetaConfigKeyManager {
  private static final Logger LOG = Logger.getInstance(InfraApplicationMetaConfigKeyManagerImpl.class);
  private static final ConfigKeyNameBinder ourRelaxedConfigKeyNameBinder = new RelaxedNamesConfigKeyNameBinder();
  private static final ConfigKeyNameBinder ourBinderConfigKeyNameBinder = new BinderConfigKeyNameBinder();

  public List<MetaConfigKey> getAllMetaConfigKeys(@Nullable Module module) {
    List<MetaConfigKey> fromLibraries = CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      List<PsiFile> metaInfConfigFiles = InfraUtils.findConfigFilesInMetaInf(module, true, InfraConfigFileConstants.SPRING_CONFIGURATION_METADATA_JSON, PsiFile.class);
      List<MetaConfigKey> allKeys = new ArrayList<>();
      for (PsiFile configMetadataFile : metaInfConfigFiles) {
        List<MetaConfigKey> keys = getConfigKeysForFile(module, configMetadataFile);
        allKeys.addAll(keys);
      }
      return CachedValueProvider.Result.create(allKeys, PsiModificationTracker.MODIFICATION_COUNT);
    });
    List<MetaConfigKey> localKeys = getLocalMetaConfigKeys(module);
    return ContainerUtil.concat(fromLibraries, localKeys);
  }

  private static List<MetaConfigKey> getConfigKeysForFile(Module module, PsiFile jsonFile) {
    List<MetaConfigKey> keys = new ArrayList<>();
    Processor<MetaConfigKey> collect = Processors.cancelableCollectProcessor(keys);
    try {
      InfraConfigurationMetadataParser parser = new InfraConfigurationMetadataParser(jsonFile);
      parser.processKeys(module, collect);
    }
    catch (ProcessCanceledException | IndexNotReadyException e) {
      throw e;
    }
    catch (Throwable e2) {
      LOG.warn("Error parsing " + jsonFile.getVirtualFile().getPath(), e2);
    }
    return keys;
  }

  private static List<MetaConfigKey> getLocalMetaConfigKeys(Module localModule) {
    return CachedValuesManager.getManager(localModule.getProject()).getCachedValue(localModule, () -> {
      Object projectRootManager;
      Set<Module> allModules = new LinkedHashSet<>();
      ModuleUtilCore.getDependencies(localModule, allModules);
      boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
      if (ContainerUtil.process(allModules, module -> !InfraLibraryUtil.hasConfigurationMetadataAnnotationProcessor(module))) {
        if (unitTestMode) {
          projectRootManager = PsiModificationTracker.MODIFICATION_COUNT;
        }
        else {
          projectRootManager = ProjectRootManager.getInstance(localModule.getProject());
        }
        Object libraryCheckDependency = projectRootManager;
        return CachedValueProvider.Result.create(Collections.emptyList(), new Object[] { libraryCheckDependency });
      }

      SmartList<Object> dependencies = new SmartList<>(PsiModificationTracker.MODIFICATION_COUNT);
      List<MetaConfigKey> allKeys = new ArrayList<>();
      for (Module module : allModules) {
        File localJsonFile = findLocalMetadataJsonFile(module, InfraConfigFileConstants.SPRING_CONFIGURATION_METADATA_JSON, unitTestMode);
        if (localJsonFile != null) {
          List<MetaConfigKey> keys = new ArrayList<>();
          Processor<MetaConfigKey> collect = Processors.cancelableCollectProcessor(keys);
          try {
            InfraConfigurationMetadataParser parser = new InfraConfigurationMetadataParser(module, localJsonFile);
            parser.processKeys(module, collect);
          }
          catch (ProcessCanceledException | IndexNotReadyException e) {
            throw e;
          }
          catch (Throwable throwable) {
            LOG.warn("Error parsing " + localJsonFile.getPath(), throwable);
          }
          allKeys.addAll(keys);
          ContainerUtil.addIfNotNull(dependencies, LocalFileSystem.getInstance().findFileByIoFile(localJsonFile));
        }
      }
      return CachedValueProvider.Result.create(allKeys, ArrayUtil.toObjectArray(dependencies));
    });
  }

  public MetaConfigKeyManager.ConfigKeyNameBinder getConfigKeyNameBinder(Module module) {
    return ourBinderConfigKeyNameBinder;
  }

  protected boolean isCaseSensitiveKeys() {
    return false;
  }
}
