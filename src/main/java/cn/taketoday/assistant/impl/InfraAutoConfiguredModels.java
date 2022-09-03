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

package cn.taketoday.assistant.impl;

import com.intellij.facet.FacetFinder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Chunk;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphAlgorithms;
import com.intellij.util.xml.DomService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.context.model.LocalXmlModel;
import cn.taketoday.assistant.context.model.graph.LazyModelDependenciesGraph;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraAutoConfiguredModels {
  public static final List<String> annotations = List.of(
          InfraConfigConstant.INFRA_APPLICATION,
          InfraConfigConstant.ENABLE_AUTO_CONFIGURATION
  );

  public static Result<Set<InfraModel>> getAutoConfiguredModels(Module module) {
    Set<InfraModel> models = discoverAutoConfiguredModels(module);
    return Result.create(models,
            PsiModificationTracker.MODIFICATION_COUNT,
            FacetFinder.getInstance(module.getProject())
                    .getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID));
  }

  public static Set<InfraModel> discoverAutoConfiguredModels(Module module) {
    Set<InfraModel> infraModels = getInfraModels(module);
    if (!infraModels.isEmpty()) {
      return infraModels;
    }
    Set<LocalModel<?>> autoConfiguredModels = getAutoConfiguredLocalModels(module);
    if (autoConfiguredModels.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.singleton(new CombinedInfraModelImpl(autoConfiguredModels, module));
  }

  public static Set<InfraModel> getInfraModels(Module module) {
    var applications = new LinkedHashSet<InfraModel>();
    for (String bootAnno : annotations) {
      applications.addAll(getAnnoConfiguredModels(module, bootAnno));
    }
    return applications;
  }

  public static Set<LocalModel<?>> getAutoConfiguredLocalModels(Module module) {
    Set<LocalModel<?>> localModels = getLocalModelCandidates(module);
    if (!localModels.isEmpty()) {
      return filterAutoConfiguredModelsSet(localModels, module);
    }
    return Collections.emptySet();
  }

  public static Set<LocalModel<?>> getLocalModelCandidates(Module module) {
    var localModels = new LinkedHashSet<LocalModel<?>>();
    localModels.addAll(getLocalAnnotationModelCandidates(module));
    localModels.addAll(getLocalXmlModelCandidates(module));
    return localModels;
  }

  private static Set<LocalModel<?>> filterAutoConfiguredModelsSet(Set<LocalModel<?>> models, Module module) {
    LazyModelDependenciesGraph graph = getModelsGraph(models, module);
    HashSet<LocalModel<?>> configuredModels = new HashSet<>();
    Graph<Chunk<LocalModel<?>>> sccGraph = GraphAlgorithms.getInstance().computeSCCGraph(graph);
    for (Chunk<LocalModel<?>> chunk : sccGraph.getNodes()) {
      if (!sccGraph.getIn(chunk).hasNext()) {
        Set<LocalModel<?>> chunkNodes = chunk.getNodes();
        if (chunkNodes.iterator().hasNext()) {
          configuredModels.add(chunkNodes.iterator().next());
        }
      }
    }
    return configuredModels;
  }

  private static LazyModelDependenciesGraph getModelsGraph(Set<LocalModel<?>> models, Module module) {
    return new LazyModelDependenciesGraph(module, Collections.emptySet()) {

      @Override
      public Collection<LocalModel<?>> getNodes() {
        return models;
      }
    };
  }

  private static Set<LocalAnnotationModel> getLocalAnnotationModelCandidates(Module module) {
    List<Configuration> configurations = InfraJamModel.from(module)
            .getConfigurations(GlobalSearchScope.moduleWithDependenciesScope(module));
    return configurations.stream().filter(configuration -> {
              PsiFile file = configuration.getContainingFile();
              return file != null && !isInTestSourceContent(file.getProject(), file.getVirtualFile());
            })
            .filter(configuration2 -> InfraUtils.isBeanCandidateClass(configuration2.getPsiElement()))
            .map(configuration3 -> LocalModelFactory.of().getOrCreateLocalAnnotationModel(configuration3.getPsiElement(), module, Collections.emptySet())).filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  private static Set<LocalXmlModel> getLocalXmlModelCandidates(Module module) {
    Project project = module.getProject();
    Collection<VirtualFile> candidates = DomService.getInstance().getDomFileCandidates(Beans.class, GlobalSearchScope.moduleWithDependenciesScope(module));
    return candidates.stream()
            .filter(vf -> ApplicationManager.getApplication().isUnitTestMode()
                    || !isInTestSourceContent(project, vf))
            .map(file -> {
              PsiFile findFile = PsiManager.getInstance(project).findFile(file);
              if (findFile instanceof XmlFile xmlFile) {
                return LocalModelFactory.of().getOrCreateLocalXmlModel(xmlFile, module, Collections.emptySet());
              }
              return null;
            })
            .filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static boolean isInTestSourceContent(Project project, @Nullable VirtualFile vf) {
    return vf != null && ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(vf);
  }

  private static Set<InfraModel> getAnnoConfiguredModels(Module module, String anno) {
    LocalAnnotationModel localAnnotationModel;
    if (DumbService.isDumb(module.getProject())) {
      return Collections.emptySet();
    }
    var models = new LinkedHashSet<InfraModel>();
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    PsiClass psiAnno = JavaPsiFacade.getInstance(module.getProject()).findClass(anno, scope);
    if (psiAnno != null) {
      for (PsiMember psiMember : AnnotatedMembersSearch.search(psiAnno, scope).findAll()) {
        if ((psiMember instanceof PsiClass psiClass)
                && InfraUtils.isBeanCandidateClass(psiClass)
                && (localAnnotationModel = LocalModelFactory.of()
                .getOrCreateLocalAnnotationModel(psiClass, module, Collections.emptySet())) != null) {
          models.add(new CombinedInfraModelImpl(Collections.singleton(localAnnotationModel), localAnnotationModel.getModule()));
        }
      }
    }
    return models;
  }

  public static boolean isAllowAutoConfiguration(Project project) {
    return !project.isDisposed() && InfraGeneralSettings.from(project).isAllowAutoConfigurationMode();
  }

  public static boolean hasAutoConfiguredModels(Module module) {
    return isAllowAutoConfiguration(module.getProject()) && !getAutoConfiguredLocalModels(module).isEmpty();
  }
}
