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

import com.intellij.facet.FacetFinder;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.application.config.hints.ConfigReferenceProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.ConfigurationValueSearchParams;
import cn.taketoday.assistant.model.config.InfraConfigFileModificationTracker;
import cn.taketoday.assistant.model.config.InfraConfigFileDetector;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraConfigurationFileService {
  private static final String INFRA_CONFIG_IMPORT_KEY = "app.config.import";

  public static InfraConfigurationFileService of() {
    return ApplicationManager.getApplication().getService(InfraConfigurationFileService.class);
  }

  public boolean isApplicationConfigurationFile(PsiFile file) {
    return getApplicationConfigurationFileIcon(file) != null || isDetectedConfigFile(file);
  }

  private static boolean isDetectedConfigFile(PsiFile file) {
    InfraConfigFileDetector[] extensions = InfraConfigFileDetector.EP_NAME.getExtensions();
    for (InfraConfigFileDetector detector : extensions) {
      if (detector.isInfraConfigFile(file)) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  public Icon getApplicationConfigurationFileIcon(PsiFile file) {

    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile != null) {
      Module module = ModuleUtilCore.findModuleForPsiElement(file);
      if (module != null && !module.isDisposed()
              && InfraUtils.hasFacet(module)
              && InfraLibraryUtil.hasFrameworkLibrary(module)) {
        boolean includeTestScope = ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(virtualFile);

        var extensions = InfraModelConfigFileNameContributor.EP_NAME.getExtensions();
        for (var fileNameContributor : extensions) {
          List<VirtualFile> files = findConfigFiles(module, includeTestScope, (contributor) -> {
            return contributor == fileNameContributor && contributor.accept(module);
          });
          if (files.contains(virtualFile)) {
            return fileNameContributor.getFileIcon();
          }

          List<VirtualFile> imports = collectImports(module, files);
          if (imports.contains(virtualFile)) {
            return fileNameContributor.getFileIcon();
          }
        }

      }
    }
    return null;
  }

  public List<VirtualFile> findConfigFiles(Module module, boolean includeTestScope) {
    return this.findConfigFiles(module, includeTestScope, (contributor) -> {
      return contributor.accept(module);
    });
  }

  public List<VirtualFile> findConfigFiles(Module module, boolean includeTestScope, Condition<? super InfraModelConfigFileNameContributor> filter) {

    SmartList<VirtualFile> result = new SmartList<>();
    InfraModelConfigFileNameContributor[] extensions = InfraModelConfigFileNameContributor.EP_NAME.getExtensions();
    for (InfraModelConfigFileNameContributor fileNameContributor : extensions) {
      if (filter.value(fileNameContributor)) {
        result.addAll(findConfigFiles(module, includeTestScope, fileNameContributor));
      }
    }

    return result;
  }

  public List<VirtualFile> findConfigFilesWithImports(Module module, boolean includeTestScope) {
    List<VirtualFile> configFiles = findConfigFiles(module, includeTestScope);
    List<VirtualFile> importedFiles = collectImports(module, configFiles);
    return importedFiles.isEmpty() ? configFiles : ContainerUtil.concat(configFiles, importedFiles);
  }

  private static List<VirtualFile> findConfigFiles(Module module, boolean includeTestScope, InfraModelConfigFileNameContributor fileNameContributor) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Map<Pair<InfraModelConfigFileNameContributor, Boolean>, List<VirtualFile>> map = FactoryMap.create((key) -> {
        return doFindConfigFile(module, key.second, key.first);
      });
      return Result.create(map, getConfigFilesDependencies(module));
    }).get(Pair.create(fileNameContributor, includeTestScope));
  }

  private static List<Object> getConfigFilesDependencies(Module module) {
    List<Object> dependencies = ContainerUtil.newArrayList(
            module.getProject().getService(InfraConfigFileModificationTracker.class),
            FacetFinder.getInstance(module.getProject()).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID),
            ProjectRootManager.getInstance(module.getProject())
    );
    InfraFacet facet = InfraFacet.from(module);
    if (facet != null) {
      dependencies.add(facet.getConfiguration());
    }

    return dependencies;
  }

  private static List<VirtualFile> doFindConfigFile(Module module, boolean includeTestScope, InfraModelConfigFileNameContributor fileNameContributor) {

    List<VirtualFile> result = new SmartList<>();
    List<FileType> fileTypes = ContainerUtil.map(InfraModelConfigFileContributor.array(), InfraModelConfigFileContributor::getFileType);
    String configName = fileNameContributor.getInfraConfigName(module);
    List<VirtualFile> profileConfigFiles = new SmartList<>();
    List<VirtualFile> baseNameConfigFiles = new SmartList<>();
    List<VirtualFile> directories = InfraModelConfigFileContributor.getConfigFileDirectories(module, includeTestScope);

    for (VirtualFile directory : directories) {
      for (FileType fileType : fileTypes) {
        Pair<List<VirtualFile>, List<VirtualFile>> allConfigs = InfraModelConfigFileContributor.findConfigFiles(module, directory, fileType, configName);
        profileConfigFiles.addAll(allConfigs.first);
        baseNameConfigFiles.addAll(allConfigs.second);
      }
    }

    result.addAll(profileConfigFiles);
    result.addAll(fileNameContributor.findCustomConfigFiles(module));
    result.addAll(baseNameConfigFiles);
    return result;
  }

  public List<VirtualFile> collectImports(Module module, List<VirtualFile> configFiles) {

    if (getInfraConfigImportKey(module) == null) {
      return Collections.emptyList();
    }
    else {
      List<VirtualFile> result = new ArrayList<>(configFiles);
      Queue<VirtualFile> filesToProcess = new ArrayDeque<>(configFiles);

      while (!filesToProcess.isEmpty()) {
        VirtualFile configFile = filesToProcess.poll();
        List<InfraConfigImport> imports = this.getImports(module, configFile);
        for (InfraConfigImport configImport : imports) {
          VirtualFile importedFile = configImport.getVirtualFile();
          if (!result.contains(importedFile) && !configFiles.contains(importedFile)) {
            result.add(importedFile);
            filesToProcess.add(importedFile);
          }
        }
      }

      return result;
    }
  }

  public List<InfraConfigImport> getImports(Module module, VirtualFile virtualFile) {

    InfraModelConfigFileContributor contributor = InfraModelConfigFileContributor.getContributor(virtualFile);
    if (contributor == null) {
      return Collections.emptyList();
    }
    else {
      PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
      return psiFile == null ? Collections.emptyList() : CachedValuesManager.getCachedValue(psiFile, () -> {
        MetaConfigKey configImportKey = getInfraConfigImportKey(module);
        List<InfraConfigImport> imports = configImportKey == null ? Collections.emptyList() : doGetImports(module, psiFile, contributor, configImportKey);
        if (!imports.isEmpty()) {
          List<FileType> fileTypes = ContainerUtil.map(InfraModelConfigFileContributor.array(),
                  InfraModelConfigFileContributor::getFileType);
          imports = ContainerUtil.filter(imports, (configImport) -> {
            return fileTypes.contains(configImport.getVirtualFile().getFileType());
          });
        }

        return Result.create(imports, psiFile, module.getProject().getService(InfraConfigFileModificationTracker.class), ProjectRootManager.getInstance(module.getProject()));
      });
    }
  }

  private static List<InfraConfigImport> doGetImports(
          Module module, PsiFile psiFile, InfraModelConfigFileContributor contributor, MetaConfigKey configImportKey) {
    List<String> values;
    SmartList<InfraConfigImport> smartList = new SmartList<>();
    VirtualFile virtualFile = psiFile.getVirtualFile();
    NotNullLazyValue<Boolean> includeTestScope = NotNullLazyValue.lazy(() -> {
      return ProjectFileIndex.getInstance(module.getProject()).isInTestSourceContent(virtualFile);
    });
    var params = new ConfigurationValueSearchParams(module, true, ConfigurationValueSearchParams.PROCESS_ALL_VALUES, configImportKey, null, null, false, new HashSet<>());
    List<ConfigurationValueResult> valueResults = contributor.findConfigurationValues(psiFile, params);

    for (ConfigurationValueResult valueResult : valueResults) {
      String valueText = valueResult.getValueText();
      if (!StringUtil.isEmpty(valueText)) {
        if (valueResult.getKeyIndexText() != null) {
          values = new SmartList<>(valueText);
        }
        else {
          values = StringUtil.split(valueText, ",");
        }
        for (String value : values) {
          VirtualFile importedFile = findImport(value, module, virtualFile, includeTestScope);
          if (importedFile != null) {
            smartList.add(new InfraConfigImportImpl(importedFile, valueResult.getDocumentId()));
          }
        }
      }
    }
    return smartList;
  }

  private static VirtualFile findImport(String value, Module module, VirtualFile virtualFile, NotNullLazyValue<Boolean> includeTestScope) {
    value = value.trim();
    value = StringUtil.trimStart(value, "optional:");
    if (value.startsWith("configtree:")) {
      return null;
    }
    else {
      List<PsiDirectory> roots;
      if (value.startsWith("file:")) {
        value = value.substring("file:".length());
        VirtualFile contentRoot = ProjectRootManager.getInstance(module.getProject()).getFileIndex().getContentRootForFile(virtualFile);
        PsiDirectory psiDirectory = contentRoot == null ? null : PsiManager.getInstance(module.getProject()).findDirectory(contentRoot);
        if (psiDirectory == null) {
          roots = Collections.emptyList();
        }
        else {
          roots = new SmartList<>(psiDirectory);
        }
      }
      else {
        value = StringUtil.trimStart(value, "classpath:");
        roots = ConfigReferenceProvider.getClasspathRoots(module, includeTestScope.getValue());
      }

      value = StringUtil.trimStart(value, "/");
      List<String> segments = StringUtil.split(value, "/");
      Iterator<PsiDirectory> var10 = (roots).iterator();

      VirtualFile file;
      do {
        if (!var10.hasNext()) {
          return null;
        }

        PsiDirectory root = var10.next();
        file = findFile(root, segments);
      }
      while (file == null);

      return file;
    }
  }

  private static VirtualFile findFile(PsiDirectory root, List<String> segments) {
    Queue<String> queue = new ArrayDeque<>(segments);

    do {
      if (queue.isEmpty()) {
        return null;
      }

      String segment = queue.poll();
      if (queue.isEmpty()) {
        PsiFile file = root.findFile(segment);
        return file == null ? null : file.getVirtualFile();
      }

      root = root.findSubdirectory(segment);
    }
    while (root != null);

    return null;
  }

  @Nullable
  private static MetaConfigKey getInfraConfigImportKey(Module module) {
    return InfraApplicationMetaConfigKeyManager.getInstance()
            .findCanonicalApplicationMetaConfigKey(module, INFRA_CONFIG_IMPORT_KEY);
  }

  private static class InfraConfigImportImpl implements InfraConfigImport {
    private final VirtualFile myVirtualFile;
    private final int myDocumentId;

    InfraConfigImportImpl(VirtualFile virtualFile, int documentId) {

      this.myVirtualFile = virtualFile;
      this.myDocumentId = documentId;
    }

    public VirtualFile getVirtualFile() {
      return myVirtualFile;
    }

    public int getDocumentId() {
      return this.myDocumentId;
    }
  }
}
