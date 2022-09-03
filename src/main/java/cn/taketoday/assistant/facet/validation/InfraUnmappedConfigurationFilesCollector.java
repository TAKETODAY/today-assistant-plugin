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

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.xml.config.ConfigFileSearcher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.facet.InfraFrameworkDetector;
import cn.taketoday.assistant.facet.searchers.CodeConfigSearcher;
import cn.taketoday.assistant.facet.searchers.XmlConfigSearcher;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraUnmappedConfigurationFilesCollector {
  private static final Logger LOG = Logger.getInstance(InfraUnmappedConfigurationFilesCollector.class);
  private static final String XML_CONFIG_INSPECTION = "TodayFacetInspection";
  private static final String CODE_CONFIG_INSPECTION = "TodayFacetCodeInspection";
  private final DetectionExcludesConfiguration myDetectionExcludesConfiguration;
  private final InspectionProfile myProfile;
  private final boolean myCheckXml;
  private final boolean myCheckCode;
  private final Module[] myModules;
  private final Collection<InfraFileSet> myFileSets;
  private final Project myProject;
  private final MultiMap<Module, PsiFile> myNotConfiguredStorage;

  public InfraUnmappedConfigurationFilesCollector(Module... modules) {
    this(null, modules);
  }

  public InfraUnmappedConfigurationFilesCollector(@Nullable Collection<InfraFileSet> fileSets, Module... modules) {
    this.myNotConfiguredStorage = MultiMap.createLinked();
    this.myFileSets = fileSets;
    this.myModules = modules;
    assert modules.length != 0;

    this.myProject = modules[0].getProject();
    this.myDetectionExcludesConfiguration = DetectionExcludesConfiguration.getInstance(this.myProject);
    InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(this.myProject);
    this.myProfile = profileManager.getCurrentProfile();
    this.myCheckXml = this.myProfile.isToolEnabled(HighlightDisplayKey.find(XML_CONFIG_INSPECTION));
    this.myCheckCode = this.myProfile.isToolEnabled(HighlightDisplayKey.find(CODE_CONFIG_INSPECTION));
  }

  public boolean isEnabledInProject() {
    return this.myCheckXml || this.myCheckCode;
  }

  public void collect() {
    collect(new EmptyProgressIndicator());
  }

  public void collect(ProgressIndicator indicator) {
    InfraFileSetService fileSetService = InfraFileSetService.of();
    long start = System.currentTimeMillis();
    LOG.debug("================= START ============ total modules #" + this.myModules.length);
    NullableFunction<VirtualFilePointer, PsiFile> virtualFileMapper = getVirtualFileMapper(this.myProject);
    Set<PsiFile> allKnownConfigurationFiles = new HashSet<>();
    if (this.myFileSets != null) {
      for (InfraFileSet fileSet : this.myFileSets) {
        allKnownConfigurationFiles.addAll(ContainerUtil.mapNotNull(fileSet.getXmlFiles(), virtualFileMapper));
        allKnownConfigurationFiles.addAll(ContainerUtil.mapNotNull(fileSet.getCodeConfigurationFiles(), virtualFileMapper));
      }
    }
    else {
      for (Module module : this.myModules) {
        indicator.checkCanceled();
        InfraFacet infraFacet = InfraFacet.from(module);
        if (infraFacet != null) {
          for (InfraFileSet fileSet2 : fileSetService.getAllSets(infraFacet)) {
            allKnownConfigurationFiles.addAll(ContainerUtil.mapNotNull(fileSet2.getXmlFiles(), virtualFileMapper));
            allKnownConfigurationFiles.addAll(ContainerUtil.mapNotNull(fileSet2.getCodeConfigurationFiles(), virtualFileMapper));
          }
        }
      }
    }
    LOG.debug("=== collected all known");
    for (Module module2 : this.myModules) {
      indicator.checkCanceled();
      if (this.myCheckXml) {
        List<PsiFile> files = collectNotConfigured(indicator, allKnownConfigurationFiles, new XmlConfigSearcher(module2, false), module2);
        this.myNotConfiguredStorage.putValues(module2, files);
      }
      if (this.myCheckCode) {
        List<PsiFile> files2 = collectNotConfigured(indicator, allKnownConfigurationFiles, new CodeConfigSearcher(module2, false), module2);
        this.myNotConfiguredStorage.putValues(module2, files2);
      }
    }
    LOG.debug("=== collected not configured");
    Collection<?> filesUsedInSpringModels = getFilesUsedImplicitlyInSpringModels(indicator, this.myNotConfiguredStorage);
    for (Map.Entry<Module, Collection<PsiFile>> entry : this.myNotConfiguredStorage.entrySet()) {
      entry.getValue().removeAll(filesUsedInSpringModels);
    }
    LOG.debug("=== processed implicit infra model");
    Collection<?> filesUsedImplicitlyAsStereotype = getFilesUsedImplicitlyAsStereotype(indicator, this.myNotConfiguredStorage);
    for (Map.Entry<Module, Collection<PsiFile>> entry2 : this.myNotConfiguredStorage.entrySet()) {
      entry2.getValue().removeAll(filesUsedImplicitlyAsStereotype);
    }
    LOG.debug("================= END ============  total time: " + (System.currentTimeMillis() - start));
  }

  public MultiMap<Module, PsiFile> getResults() {
    return this.myNotConfiguredStorage;
  }

  private List<PsiFile> collectNotConfigured(ProgressIndicator indicator, Set<PsiFile> allKnownConfigurationFiles, ConfigFileSearcher searcher, Module module) {
    searcher.search();
    Collection<PsiFile> files = searcher.getFilesByModules().get(module);
    SmartList<PsiFile> smartList = new SmartList();
    for (PsiFile psiFile : files) {
      if (!allKnownConfigurationFiles.contains(psiFile)) {
        indicator.checkCanceled();
        if (!this.myDetectionExcludesConfiguration.isExcludedFromDetection(psiFile.getVirtualFile(),
                InfraFrameworkDetector.getSpringFrameworkType()) && (!(psiFile instanceof XmlFile) || !skipConfigInspectionFor(psiFile, XML_CONFIG_INSPECTION))) {
          if (psiFile instanceof PsiClassOwner) {
            PsiElement[] classes = ((PsiClassOwner) psiFile).getClasses();
            if (classes.length == 1) {
              skipConfigInspectionFor(classes[0], CODE_CONFIG_INSPECTION);
            }
          }
          smartList.add(psiFile);
        }
      }
    }
    return smartList;
  }

  private boolean skipConfigInspectionFor(PsiElement place, String toolId) {
    InspectionProfileEntry tool;
    HighlightDisplayKey toolHighlightDisplayKey = HighlightDisplayKey.find(toolId);
    if (this.myProfile.isToolEnabled(toolHighlightDisplayKey, place) && (tool = this.myProfile.getUnwrappedTool(toolId, place)) != null) {
      return SuppressionUtil.inspectionResultSuppressed(place, tool);
    }
    return true;
  }

  private Set<PsiFile> getFilesUsedImplicitlyInSpringModels(ProgressIndicator indicator, MultiMap<Module, PsiFile> notConfigured) {
    Collection<?> allNotConfiguredFiles = notConfigured.values();
    if (allNotConfiguredFiles.isEmpty()) {
      return Collections.emptySet();
    }
    indicator.setText2(InfraBundle.message("searching.for.implicit.usages"));
    indicator.setFraction(0.0d);
    int i = 0;
    int moduleCount = notConfigured.size();
    LOG.debug("=== implicit infra model  modules #" + moduleCount + " total files #" + allNotConfiguredFiles.size());
    InfraManager infraManager = InfraManager.from(this.myProject);
    int moduleIdx = 0;
    Set<PsiFile> found = new HashSet<>();
    for (Map.Entry<Module, Collection<PsiFile>> entry : notConfigured.entrySet()) {
      Module module = entry.getKey();
      Collection<PsiFile> notConfiguredFiles = entry.getValue();
      moduleIdx++;
      LOG.debug("=== processing " + moduleIdx + " files #" + notConfiguredFiles.size());
      ModuleUtilCore.visitMeAndDependentModules(module, visitModule -> {
        Set<InfraModel> models = infraManager.getAllModelsWithoutDependencies(visitModule);
        for (InfraModel model : models) {
          if (model.getFileSet() != null) {
            indicator.checkCanceled();
            for (PsiFile configFile : InfraModelVisitorUtils.getConfigFiles(model)) {
              if (notConfiguredFiles.contains(configFile)) {
                found.add(configFile);
                if (found.containsAll(notConfiguredFiles)) {
                  return false;
                }
              }
            }
            continue;
          }
        }
        return true;
      });
      if (found.containsAll(allNotConfiguredFiles)) {
        LOG.debug("=== early exit");
        return found;
      }
      int i2 = i;
      i++;
      indicator.setFraction(i2 / moduleCount);
    }
    return found;
  }

  private static Set<PsiFile> getFilesUsedImplicitlyAsStereotype(ProgressIndicator indicator, MultiMap<Module, PsiFile> notConfigured) {
    if (notConfigured.isEmpty()) {
      return Collections.emptySet();
    }
    LOG.debug("=== collected implicit stereotype   remaining modules #" + notConfigured.size());
    indicator.setText2(InfraBundle.message("searching.for.stereotype.usages"));
    indicator.setFraction(0.0d);
    int totalNotConfiguredStereoTypes = 0;
    Map<Module, List<PsiClassOwner>> notConfiguredStereotypes = new HashMap<>();
    for (Map.Entry<Module, Collection<PsiFile>> entry : notConfigured.entrySet()) {
      Collection<PsiFile> configFiles = entry.getValue();
      List<PsiClassOwner> psiClassOwners = ContainerUtil.findAll(configFiles, PsiClassOwner.class);
      if (!psiClassOwners.isEmpty()) {
        notConfiguredStereotypes.put(entry.getKey(), psiClassOwners);
        totalNotConfiguredStereoTypes += psiClassOwners.size();
      }
    }
    int moduleIdx = 0;
    int processed = 0;
    Set<PsiFile> foundStereoTypeConfigFiles = new HashSet<>();
    for (Map.Entry<Module, List<PsiClassOwner>> entry2 : notConfiguredStereotypes.entrySet()) {
      Module module = entry2.getKey();
      List<PsiClassOwner> psiClassOwners2 = entry2.getValue();
      moduleIdx++;
      Set<InfraModel> allModels = InfraManager.from(module.getProject()).getAllModels(module);
      LOG.debug("=== processing " + moduleIdx + " with " + allModels.size() + " models, PCO #" + psiClassOwners2.size());
      for (PsiClassOwner psiClassOwner : psiClassOwners2) {
        PsiClass[] classes = psiClassOwner.getClasses();
        if (classes.length != 0) {
          PsiClass configClass = classes[0];
          LOG.debug("  " + configClass);
          if (InfraUtils.isBeanCandidateClass(configClass)) {
            ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(configClass);
            if (params.canSearch()) {
              Iterator<InfraModel> it = allModels.iterator();
              while (true) {
                if (!it.hasNext()) {
                  break;
                }
                CommonInfraModel springModel = it.next();
                LOG.debug("  in " + springModel);
                indicator.checkCanceled();
                if (InfraModelSearchers.doesBeanExist(springModel, params)) {
                  foundStereoTypeConfigFiles.add(psiClassOwner);
                  break;
                }
              }
              int i = processed;
              processed++;
              indicator.setFraction(i / totalNotConfiguredStereoTypes);
              LOG.debug("   processed: " + processed);
            }
          }
        }
      }
    }
    return foundStereoTypeConfigFiles;
  }

  public static NullableFunction<VirtualFilePointer, PsiFile> getVirtualFileMapper(Project project) {
    PsiManager psiManager = PsiManager.getInstance(project);
    return pointer -> {
      if (!pointer.isValid() || pointer.getFile() == null) {
        return null;
      }
      return psiManager.findFile(pointer.getFile());
    };
  }
}
