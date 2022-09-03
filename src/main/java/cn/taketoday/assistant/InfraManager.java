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

package cn.taketoday.assistant;

import com.intellij.facet.FacetFinder;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.assistant.context.chooser.InfraContextDescriptor;
import cn.taketoday.assistant.context.chooser.InfraMultipleContextsManager;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalModel;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.assistant.impl.InfraCombinedModelFactory;
import cn.taketoday.assistant.impl.ModelsCreationContext;
import cn.taketoday.assistant.settings.InfraGeneralSettings;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 23:56
 */
public class InfraManager implements Disposable {
  private static final Key<CachedValue<Set<InfraModel>>> ALL_MODELS_MODULE_WITH_DEPS = Key.create("ALL_MODELS_MODULE_WITH_DEPS");
  private static final Key<CachedValue<InfraModel>> MODULE_COMBINED_MODEL_KEY = Key.create("MODULE_COMBINED_MODEL_KEY");
  private final Project myProject;

  public InfraManager(Project myProject) {
    this.myProject = myProject;
  }

  public static InfraManager from(Project project) {
    return project.getService(InfraManager.class);
  }

  public Object[] getModelsDependencies(Module module, Object... additional) {
    return getModelDependenciesObjects(module, additional);
  }

  private static Object[] getModelDependenciesObjects(Module module, Object... additional) {
    ArrayList<Object> dependencies = new ArrayList<>(6);
    Collections.addAll(dependencies, additional);
    return addModuleModelsDependencies(module, dependencies).toArray();
  }

  private static Collection<Object> addModuleModelsDependencies(Module module, Collection<Object> dependencies) {

    ContainerUtil.addAll(dependencies, InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies());
    InfraFacet facet = InfraFacet.from(module);
    if (facet != null) {
      dependencies.add(facet.getConfiguration());
    }

    return dependencies;
  }

  /**
   * Returns all models configured in given module including all dependencies.
   *
   * @param module a module.
   * @return Models or empty set if none configured.
   * @see #getCombinedModel(Module)
   */
  public Set<InfraModel> getAllModels(Module module) {
    return CachedValuesManager.getManager(this.myProject).getCachedValue(module, ALL_MODELS_MODULE_WITH_DEPS, () -> {
      Set<InfraModel> models = computeAllModels(ModelsCreationContext.create(module));
      return Result.create(models, this.getModelsDependencies(module, getConfigs(models)));
    }, false);
  }

  private static Object[] getConfigs(Set<InfraModel> models) {
    Set<PsiElement> configs = new HashSet<>();

    for (InfraModel model : models) {
      for (CommonInfraModel relatedModel : model.getRelatedModels()) {
        if (relatedModel instanceof LocalModel<?> localModel) {
          PsiElement config = localModel.getConfig();
          configs.add(config);
        }
      }
    }

    return ArrayUtil.toObjectArray(configs);
  }

  public static Set<InfraModel> getAllModels(ModelsCreationContext context) {
    return computeAllModels(context);
  }

  /**
   * Returns all models configured in given module without dependencies.
   * <p/>
   * Usually, you will want to use {@link #getAllModels(Module)}.
   *
   * @param module Module.
   * @return Models or empty set if none configured.
   */
  public Set<InfraModel> getAllModelsWithoutDependencies(Module module) {
    return getModelsWithoutDependencies(module);
  }

  public static Set<InfraModel> getModelsWithoutDependencies(Module module) {

    if (module.isDisposed()) {
      return Collections.emptySet();
    }
    else {
      Project project = module.getProject();
      return CachedValuesManager.getManager(project).getCachedValue(module, () -> {
        InfraFacet facet = InfraFacet.from(module);
        if (facet == null) {
          return isAllowAutoConfiguration(project)
                 ? InfraAutoConfiguredModels.getAutoConfiguredModels(module)
                 : Result.create(Collections.emptySet(), FacetFinder.getInstance(project)
                         .getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID));
        }
        else {
          Set<InfraFileSet> fileSets = InfraFileSetService.of().getAllSets(facet);
          if (fileSets.isEmpty() && isAllowAutoConfiguration(project)) {
            return InfraAutoConfiguredModels.getAutoConfiguredModels(module);
          }
          else {
            Set<InfraModel> models = createModelsWithoutDependencies(module, module);
            return Result.create(models, getModelDependenciesObjects(module, PsiModificationTracker.MODIFICATION_COUNT));
          }
        }
      });
    }
  }

  private static Set<InfraModel> createModelsWithoutDependencies(Module module, @Nullable Module contextModule) {

    InfraFacet facet = InfraFacet.from(module);
    if (facet == null) {
      return Collections.emptySet();
    }
    else {
      var models = new LinkedHashSet<InfraModel>();
      for (InfraFileSet set : InfraFileSetService.of().getAllSets(facet)) {
        if (!set.isRemoved()) {
          InfraModel model = InfraCombinedModelFactory.createModel(
                  set, contextModule == null ? module : contextModule);
          ContainerUtil.addIfNotNull(models, model);
        }
      }

      return models;
    }
  }

  public static boolean isAllowAutoConfiguration(Project project) {
    return InfraGeneralSettings.from(project).isAllowAutoConfigurationMode();
  }

  /**
   * Returns result of merging all models configured in given module.
   *
   * @param module a module.
   * @return Merged model.
   * @see #getAllModels(Module)
   */
  public InfraModel getCombinedModel(Module module) {
    return CachedValuesManager.getManager(myProject).getCachedValue(module, MODULE_COMBINED_MODEL_KEY, () -> {
      Set<InfraModel> models = getAllModels(module);
      return Result.create(new CombinedInfraModelImpl(models, module), getModelsDependencies(module, getConfigs(models)));
    }, false);
  }

  public Set<InfraModel> getInfraModelsByFile(PsiFile file) {
    return getCachedValueModelsByFile(file);
  }

  private static Set<InfraModel> getCachedValueModelsByFile(PsiFile file) {
    return CachedValuesManager.getCachedValue(file, () -> {
      Module module = ModuleUtilCore.findModuleForPsiElement(file);
      if (module == null) {
        return Result.create(Collections.emptySet(), getInfraModelDependencies(Collections.emptySet(), file).toArray());
      }
      else {
        InfraContextDescriptor descriptor = InfraMultipleContextsManager.of().getContextDescriptor(file);
        Set<InfraModel> models;
        if (descriptor.equals(InfraContextDescriptor.LOCAL_CONTEXT)) {
          models = ContainerUtil.createMaybeSingletonSet(InfraCombinedModelFactory.createSingleModel(file, module));
        }
        else if (descriptor.equals(InfraContextDescriptor.ALL_CONTEXTS)) {
          models = findModelsInScope(file, module);
        }
        else {
          Module descriptorModule = descriptor.getModule();
          Set<InfraModel> allModels = getAllModels(ModelsCreationContext.create(descriptorModule != null ? descriptorModule : module));
          models = allModels.stream().filter((model) -> {
            InfraFileSet fileSet = model.getFileSet();
            if (fileSet == null) {
              return false;
            }
            else {
              return descriptor.getId().equals(fileSet.getId()) && isConfiguredInModel(file, model);
            }
          }).collect(Collectors.toSet());
        }

        return Result.create(models, getInfraModelDependencies(models, file).toArray());
      }
    });
  }

  private static boolean isConfiguredInModel(PsiFile file, InfraModel model) {
    return isConfiguredInFileSet(model, file.getOriginalFile().getVirtualFile()) || InfraModelVisitorUtils.hasConfigFile(model, file);
  }

  @Nullable
  public InfraModel getInfraModelByFile(PsiFile file) {
    return (InfraModel) CachedValuesManager.getCachedValue(file, () -> {
      Set<InfraModel> allModels = this.getInfraModelsByFile(file);
      Object modelByFile;
      if (allModels.size() == 0) {
        modelByFile = null;
      }
      else if (allModels.size() == 1) {
        modelByFile = allModels.iterator().next();
      }
      else {
        modelByFile = new CombinedInfraModelImpl(allModels, allModels.iterator().next().getModule());
      }

      return Result.create(modelByFile, getInfraModelDependencies(allModels, file).toArray());
    });
  }

  @Override
  public void dispose() {

  }

  private static Set<Object> getInfraModelDependencies(Set<InfraModel> allModels, PsiFile file) {

    Project project = file.getProject();
    Set<Object> dependencies = new HashSet<>();
    dependencies.add(InfraModificationTrackersManager.from(project).getMultipleContextsModificationTracker());
    dependencies.add(FacetFinder.getInstance(project).getAllFacetsOfTypeModificationTracker(InfraFacet.FACET_TYPE_ID));
    dependencies.add(ProjectRootManager.getInstance(project));

    for (InfraModel model : allModels) {
      InfraFileSet fileSet = model.getFileSet();
      if (fileSet != null) {
        ContainerUtil.addIfNotNull(dependencies, fileSet.getFacet().getConfiguration());
      }
    }

    InfraContextDescriptor descriptor = InfraMultipleContextsManager.of().getContextDescriptor(file);
    if (descriptor.equals(InfraContextDescriptor.LOCAL_CONTEXT)) {
      dependencies.add(file);
    }

    return dependencies;
  }

  private static Set<InfraModel> computeAllModels(ModelsCreationContext context) {

    Module contextModule = context.getModule();
    Set<InfraModel> result = new LinkedHashSet<>(getModelsWithoutDependencies(contextModule));
    if (context.onlyCurrentModule()) {
      if (result.isEmpty()) {
        result.addAll(getModelsFromDependencies(contextModule));
        if (result.isEmpty()) {
          result.addAll(getModelsFromDependentModules(contextModule));
        }
      }
    }
    else {

      for (Module module : getRelatedModules(context)) {
        if (!module.equals(contextModule)) {
          result.addAll(getModelsWithoutDependencies(module));
        }
      }
    }

    Set<InfraModel> infraModels = Collections.unmodifiableSet(result);
    processFileSetDependencies(infraModels);
    return infraModels;
  }

  private static Set<Module> getRelatedModules(ModelsCreationContext context) {

    Module contextModule = context.getModule();
    Set<Module> relatedModules = new HashSet<>();
    if (context.isLoadModelsFromModuleDependencies()) {
      ModuleUtilCore.getDependencies(contextModule, relatedModules);
    }

    if (context.isLoadModelsFromDependentModules()) {
      Set<Module> dependentModules = new LinkedHashSet<>();
      ModuleUtilCore.collectModulesDependsOn(contextModule, dependentModules);
      relatedModules.addAll(dependentModules);
    }

    return relatedModules;
  }

  private static Set<InfraModel> getModelsFromDependencies(Module contextModule) {

    Set<Module> dependencies = new LinkedHashSet<>();
    ModuleUtilCore.getDependencies(contextModule, dependencies);
    Set<InfraModel> models = new LinkedHashSet<>();

    for (Module dep : dependencies) {
      if (!dep.equals(contextModule)) {
        models.addAll(createModelsWithoutDependencies(dep, contextModule));
      }
    }

    return models;
  }

  private static Set<InfraModel> getModelsFromDependentModules(Module contextModule) {

    Set<Module> dependentModules = new LinkedHashSet<>();
    ModuleUtilCore.collectModulesDependsOn(contextModule, dependentModules);
    Set<InfraModel> models = new LinkedHashSet<>();
    for (Module dep : dependentModules) {
      if (!dep.equals(contextModule)) {
        models.addAll(createModelsWithoutDependencies(dep, dep));
      }
    }

    return models;
  }

  private static void processFileSetDependencies(Set<InfraModel> models) {

    for (InfraModel model : models) {
      resolveDependencies(new LinkedHashSet<>(models), model);
    }

  }

  private static void resolveDependencies(Set<InfraModel> dependenciesCandidates, InfraModel model) {
    InfraFileSet fileSet = model.getFileSet();
    if (fileSet != null) {
      Set<InfraFileSet> dependencyFileSets = fileSet.getDependencyFileSets();
      if (!dependencyFileSets.isEmpty()) {
        Set<InfraModel> dependencies = new LinkedHashSet<>();

        for (InfraFileSet depend : dependencyFileSets) {
          InfraModel resolvedModel = findResolvedModel(model, depend, dependenciesCandidates.toArray(new InfraModel[0]));
          if (resolvedModel != null) {
            dependencies.add(resolvedModel);
          }
          else {
            Module module = model.getModule();
            if (module != null) {
              InfraModel candidateModel = InfraCombinedModelFactory.createModel(depend, module);
              if (candidateModel != null) {
                dependencies.add(candidateModel);
                dependenciesCandidates.add(candidateModel);
                resolveDependencies(dependenciesCandidates, candidateModel);
              }
            }
          }
        }

        model.setDependencies(dependencies);
      }
    }
  }

  @Nullable
  private static InfraModel findResolvedModel(InfraModel model, InfraFileSet depend, InfraModel... candidates) {

    for (InfraModel depModel : candidates) {
      if (depModel != model && depend.equals(depModel.getFileSet())) {
        return depModel;
      }
    }

    return null;
  }

  private static Set<InfraModel> findModelsInScope(PsiFile psiFile, Module module) {

    PsiFile originalFile = psiFile.getOriginalFile();
    VirtualFile virtualFile = originalFile.getVirtualFile();
    Set<InfraModel> models = new LinkedHashSet<>();

    for (InfraModel model : getAllModels(ModelsCreationContext.fromEverywhere(module))) {
      if (isConfiguredInFileSet(model, virtualFile)) {
        models.add(model);
      }
      else if (InfraModelVisitorUtils.hasConfigFile(model, originalFile)) {
        models.add(model);
      }
    }

    return models;
  }

  private static boolean isConfiguredInFileSet(InfraModel model, VirtualFile virtualFile) {
    InfraFileSet fileSet = model.getFileSet();
    if (fileSet == null) {
      return false;
    }
    else if (fileSet.hasFile(virtualFile)) {
      return true;
    }
    else {
      Iterator<InfraFileSet> var3 = fileSet.getDependencyFileSets().iterator();

      InfraFileSet dep;
      do {
        if (!var3.hasNext()) {
          return false;
        }

        dep = var3.next();
      }
      while (!dep.hasFile(virtualFile));

      return true;
    }
  }

}
