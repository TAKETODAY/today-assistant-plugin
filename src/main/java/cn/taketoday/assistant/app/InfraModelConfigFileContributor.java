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

package cn.taketoday.assistant.app;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.lang.Nullable;

/**
 * Collects all default ({@code application|bootstrap}) and/or custom named config files located in resource roots for the given filetype.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/28 00:44
 */
public abstract class InfraModelConfigFileContributor {
  public static final ExtensionPointName<InfraModelConfigFileContributor> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.modelConfigFileContributor");

  private static final Comparator<VirtualFile> VF_BY_NAME_COMPARATOR = (o1, o2) -> {
    int nameCompare = StringUtil.naturalCompare(o1.getNameWithoutExtension(), o2.getNameWithoutExtension());
    if (nameCompare != 0)
      return nameCompare;

    // invert extension comparison, since .yml takes precedence over .yaml
    return StringUtil.compare(o2.getExtension(), o1.getExtension(), true);
  };

  private static final String SPRING_DEFAULT_PROFILE = "default";

  private static final boolean OUR_TEST_MODE = ApplicationManager.getApplication().isUnitTestMode();

  private final FileType myFileType;

  protected InfraModelConfigFileContributor(FileType fileType) {
    myFileType = fileType;
  }

  public FileType getFileType() {
    return myFileType;
  }

  /**
   * Returns all default and user configuration files.
   *
   * @param module Module to evaluate.
   * @param includeTestScope
   * @return Configuration files for this contributor.
   * @deprecated Use {@link InfraConfigurationFileService#findConfigFiles(Module, boolean)} instead.
   */
  @Deprecated
  public List<VirtualFile> getConfigurationFiles(Module module, boolean includeTestScope) {
    List<VirtualFile> files = new SmartList<>();
    for (InfraModelConfigFileNameContributor fileNameContributor : InfraModelConfigFileNameContributor.EP_NAME.getExtensions()) {
      if (fileNameContributor.accept(module)) {
        files.addAll(getConfigurationFiles(module, fileNameContributor, includeTestScope));
      }
    }
    return files;
  }

  /**
   * Returns all default and user configuration files applicable for the given file set.
   *
   * @param module Module to evaluate.
   * @param fileSet Fileset to evaluate.
   * @param includeTestScope
   * @return Configuration files for this contributor.
   * @deprecated Use {@link InfraConfigurationFileService#findConfigFiles(Module, boolean, Condition)} instead.
   */
  @Deprecated
  public List<VirtualFile> getConfigurationFiles(Module module, InfraFileSet fileSet, boolean includeTestScope) {
    List<VirtualFile> files = new SmartList<>();
    for (InfraModelConfigFileNameContributor fileNameContributor : InfraModelConfigFileNameContributor.EP_NAME.getExtensions()) {
      if (fileNameContributor.accept(fileSet)) {
        files.addAll(getConfigurationFiles(module, fileNameContributor, includeTestScope));
        break;
      }
    }
    return files;
  }

  /**
   * @deprecated Use {@link InfraConfigurationFileService#findConfigFiles(Module, boolean, Condition)} instead.
   */
  @Deprecated
  public List<VirtualFile> getConfigurationFiles(Module module, InfraModelConfigFileNameContributor fileNameContributor,
          boolean includeTestScope) {
    String springConfigName = fileNameContributor.getInfraConfigName(module);
    Pair<List<VirtualFile>, List<VirtualFile>>
            configFiles = findApplicationConfigFiles(module, includeTestScope, springConfigName);
    List<VirtualFile> customFiles = ContainerUtil.filter(fileNameContributor.findCustomConfigFiles(module),
            file -> myFileType.equals(file.getFileType()));
    return ContainerUtil.concat(configFiles.first, ContainerUtil.concat(customFiles, configFiles.second));
  }

  public Pair<List<VirtualFile>, List<VirtualFile>> findApplicationConfigFiles(Module module,
          boolean includeTestScope,
          String baseName) {
    List<VirtualFile> profileConfigFiles = new SmartList<>();
    List<VirtualFile> baseNameConfigFiles = new SmartList<>();
    List<VirtualFile> directories = getConfigFileDirectories(module, includeTestScope);
    for (VirtualFile directory : directories) {
      Pair<List<VirtualFile>, List<VirtualFile>> allConfigs = findConfigFiles(module, directory, myFileType, baseName);
      profileConfigFiles.addAll(allConfigs.first);
      baseNameConfigFiles.addAll(allConfigs.second);
    }
    return Pair.create(profileConfigFiles, baseNameConfigFiles);
  }

  /**
   * Returns actual configuration value(s) if found in file for given profiles.
   * <p/>
   * Use {@link InfraConfigValueSearcher} for direct value search/processing.
   *
   * @param params Search parameters.
   * @return Actual configuration value(s) or empty list if no occurrence(s) of key.
   */

  public abstract List<ConfigurationValueResult> findConfigurationValues(PsiFile configFile, ConfigurationValueSearchParams params);

  protected static boolean isProfileRelevant(ConfigurationValueSearchParams params, @Nullable String fileNameSuffix) {
    if (fileNameSuffix == null || params.isProcessAllProfiles())
      return true;
    Set<String> activeProfiles = params.getActiveProfiles();
    if (ContainerUtil.isEmpty(activeProfiles)) {
      return SPRING_DEFAULT_PROFILE.equals(fileNameSuffix);
    }
    else {
      return activeProfiles.contains(fileNameSuffix);
    }
  }

  protected static void processImports(ConfigurationValueSearchParams params, VirtualFile virtualFile,
          List<ConfigurationValueResult> results, int documentId) {
    if (!params.getProcessImports())
      return;

    List<InfraConfigImport> imports =
            InfraConfigurationFileService.of().getImports(params.getModule(), virtualFile);
    for (InfraConfigImport configImport : imports) {
      VirtualFile importedFile = configImport.getVirtualFile();
      if (configImport.getDocumentId() == documentId && !params.getProcessedFiles().contains(importedFile)) {
        PsiFile psiFile = PsiManager.getInstance(params.getModule().getProject()).findFile(importedFile);
        if (psiFile != null) {
          InfraModelConfigFileContributor contributor = getContributor(importedFile);
          if (contributor != null) {
            results.addAll(contributor.findConfigurationValues(psiFile, params));
          }
        }
      }
    }
  }

  private static boolean USE_RESOURCE_ROOTS_FOR_TESTS;

  public static void setUseResourceRootsForTests(boolean useResourceRootsForTests) {
    USE_RESOURCE_ROOTS_FOR_TESTS = useResourceRootsForTests;
  }

  @Nullable
  public static InfraModelConfigFileContributor getContributor(VirtualFile virtualFile) {
    return EP_NAME.findFirstSafe(contributor -> virtualFile.getFileType().equals(contributor.myFileType));
  }

  public static List<VirtualFile> getConfigFileDirectories(Module module, boolean includeTestScope) {
    if (module.isDisposed()) {
      return Collections.emptyList();
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

    List<VirtualFile> roots = new ArrayList<>();
    if (OUR_TEST_MODE && !USE_RESOURCE_ROOTS_FOR_TESTS) {
      ContainerUtil.addAll(roots, moduleRootManager.getContentRoots());
    }
    else {
      if (includeTestScope) {
        roots.addAll(moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE));
      }
      roots.addAll(moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE));
    }
    List<VirtualFile> configRoots = ContainerUtil.mapNotNull(roots, root -> {
      VirtualFile config = root.findChild("config");
      return config != null && config.isDirectory() ? config : null;
    });
    return ContainerUtil.concat(configRoots, roots);
  }

  public static Pair<List<VirtualFile>, List<VirtualFile>> findConfigFiles(Module module,
          VirtualFile directory,
          FileType fileType,
          String baseName) {
    GlobalSearchScope searchScope = GlobalSearchScopesCore.directoriesScope(module.getProject(), false, directory);
    String fileNamePrefix = baseName + '-';

    List<VirtualFile> profileConfigFiles = new SmartList<>();
    List<VirtualFile> baseNameConfigFiles = new SmartList<>();
    FileTypeIndex.processFiles(fileType, file -> {
      String fileName = file.getNameWithoutExtension();
      if (fileName.equals(baseName)) {
        baseNameConfigFiles.add(file);
      }
      else if (StringUtil.startsWith(fileName, fileNamePrefix)) {
        profileConfigFiles.add(file);
      }
      return true;
    }, searchScope);
    profileConfigFiles.sort(VF_BY_NAME_COMPARATOR);
    if (baseNameConfigFiles.size() > 1) {
      baseNameConfigFiles.sort(VF_BY_NAME_COMPARATOR);
    }
    return Pair.create(profileConfigFiles, baseNameConfigFiles);
  }
}
