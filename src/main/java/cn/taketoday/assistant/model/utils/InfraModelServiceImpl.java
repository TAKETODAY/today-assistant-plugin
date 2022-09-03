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

package cn.taketoday.assistant.model.utils;

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomUtil;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.LocalModelFactory;
import cn.taketoday.assistant.context.model.CombinedInfraModel;
import cn.taketoday.assistant.context.model.CombinedInfraModelImpl;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.dom.InfraDomUtils;
import cn.taketoday.assistant.facet.InfraFileSetService;
import cn.taketoday.assistant.impl.InfraAutoConfiguredModels;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.jam.testContexts.ContextConfiguration;
import cn.taketoday.assistant.model.jam.testContexts.InfraTestContextUtil;
import cn.taketoday.assistant.model.jam.testContexts.InfraTestingAnnotationsProvider;
import cn.taketoday.assistant.model.xml.InfraModelElement;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraModelServiceImpl extends InfraModelService {

  @Override
  public CommonInfraModel getModel(InfraModelElement modelElement) {
    Project project = modelElement.getManager().getProject();
    InfraModel file = InfraManager.from(project).getInfraModelByFile(DomUtil.getFile(modelElement));
    return file != null ? file : InfraModel.UNKNOWN;
  }

  @Override

  public CommonInfraModel getModel(@Nullable PsiElement element) {
    InfraModel modelForFile;
    if (element == null) {
      return InfraModel.UNKNOWN;
    }
    if (element instanceof XmlElement) {
      PsiFile file = element.getContainingFile();
      if ((file instanceof XmlFile) && InfraDomUtils.isInfraXml((XmlFile) file) && (modelForFile = InfraManager.from(element.getProject()).getInfraModelByFile(file)) != null) {
        return modelForFile;
      }
    }
    else {
      UClass psiClass = UastUtils.findContaining(element, UClass.class);
      if (psiClass != null) {
        return getPsiClassModel(psiClass.getJavaPsi());
      }
    }
    return getModuleCombinedModel(element);
  }

  @Override

  public CommonInfraModel getPsiClassModel(PsiClass psiClass) {
    CommonInfraModel testingModel;
    if (!psiClass.isValid()) {
      return InfraModel.UNKNOWN;
    }
    else if (ProjectRootsUtil.isInTestSource(psiClass.getContainingFile().getOriginalFile()) && (testingModel = getSpringTestingModel(psiClass)) != null) {
      return testingModel;
    }
    else if (InfraUtils.isConfigurationOrMeta(psiClass)) {
      return getAnnoConfigurationSpringModel(psiClass);
    }
    else {
      VirtualFile virtualFile = psiClass.getContainingFile().getOriginalFile().getVirtualFile();
      if (virtualFile == null || (virtualFile.getFileSystem() instanceof JarFileSystem)) {
        Project project = psiClass.getProject();
        return getCombinedModelForProject(project);
      }
      return getModuleCombinedModel(psiClass);
    }
  }

  @Nullable
  public CommonInfraModel getSpringTestingModel(PsiClass psiClass) {
    if (InfraTestContextUtil.of().isTestContextConfigurationClass(psiClass)) {
      return InfraTestContextUtil.of().getTestingModel(psiClass);
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module != null && !module.isDisposed() && InfraUtils.isBeanCandidateClass(psiClass)) {
      ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(psiClass);
      Set<CommonInfraModel> tests = new HashSet<>();
      for (CommonInfraModel model : getTestingModels(module)) {
        if (InfraModelSearchers.doesBeanExist(model, params)) {
          tests.add(model);
        }
      }
      if (tests.isEmpty()) {
        return null;
      }
      return new CombinedInfraModelImpl(tests, null);
    }
    return null;
  }

  private CommonInfraModel getAnnoConfigurationSpringModel(PsiClass psiClass) {
    CommonInfraModel springModel = getModuleCombinedModel(psiClass);
    ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(psiClass);
    if (InfraModelSearchers.doesBeanExist(springModel, params)) {
      return springModel;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module == null) {
      return InfraModel.UNKNOWN;
    }
    LocalAnnotationModel model = LocalModelFactory.of().getOrCreateLocalAnnotationModel(psiClass, module, Collections.emptySet());
    return model != null ? model : InfraModel.UNKNOWN;
  }

  @Override
  public CommonInfraModel getModuleCombinedModel(PsiElement element) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null || module.isDisposed()) {
      return InfraModel.UNKNOWN;
    }
    return InfraManager.from(module.getProject()).getCombinedModel(module);
  }

  @Override
  public CommonInfraModel getModelByBean(@Nullable CommonInfraBean infraBean) {
    if (infraBean == null) {
      return InfraModel.UNKNOWN;
    }
    else if (infraBean instanceof InfraModelElement) {
      return getModel((InfraModelElement) infraBean);
    }
    else {
      Module module = infraBean.getModule();
      if (module != null) {
        return InfraManager.from(module.getProject()).getCombinedModel(module);
      }
      return InfraModel.UNKNOWN;
    }
  }

  @Override
  public boolean isTestContext(Module module, PsiFile file) {
    for (CommonInfraModel model : getTestingModels(module)) {
      if (InfraModelVisitorUtils.hasConfigFile(model, file)) {
        return true;
      }
    }
    return false;
  }

  public Set<CommonInfraModel> getTestingModels(Module module) {
    Set<CommonInfraModel> testingModels = new HashSet<>();
    JamService jamService = JamService.getJamService(module.getProject());
    for (PsiClass contextConfigurationAnno : getTestingAnnotations(module)) {
      String qualifiedName = contextConfigurationAnno.getQualifiedName();
      if (!StringUtil.isEmptyOrSpaces(qualifiedName)) {
        for (ContextConfiguration configuration : jamService.getJamClassElements(ContextConfiguration.CONTEXT_CONFIGURATION_JAM_KEY, qualifiedName, GlobalSearchScope.moduleScope(module))) {
          CommonInfraModel model = InfraTestContextUtil.of().getTestingModel(configuration.getPsiElement());
          if (model != InfraModel.UNKNOWN) {
            testingModels.add(model);
          }
        }
      }
    }
    return testingModels;
  }

  public static Collection<PsiClass> getTestingAnnotations(Module module) {
    Collection<PsiClass> classes = new HashSet<>();
    for (InfraTestingAnnotationsProvider provider : InfraTestingAnnotationsProvider.EP_NAME.getExtensionList()) {
      classes.addAll(provider.getTestingAnnotations(module));
    }
    return classes;
  }

  @Override
  public boolean isUsedConfigurationFile(PsiFile configurationFile, boolean checkTestFiles) {

    Project project = configurationFile.getProject();
    if (DumbService.isDumb(project)) {
      return false;
    }
    else {
      VirtualFile virtualFile = configurationFile.getVirtualFile();
      if (virtualFile == null) {
        return false;
      }
      else {
        Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        if (module == null) {
          return false;
        }
        else {
          ModuleFileIndex moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex();
          if (!moduleFileIndex.isInContent(virtualFile)) {
            return false;
          }
          else if (!checkTestFiles && moduleFileIndex.isInTestSourceContent(virtualFile)) {
            return true;
          }
          else {
            Ref<Boolean> result = Ref.create(Boolean.FALSE);
            ModuleUtilCore.visitMeAndDependentModules(module, (depModule) -> {
              boolean found = InfraFileSetService.of().findFileSet(depModule, configurationFile) != null || checkTestFiles && this.isTestContext(depModule, configurationFile);
              result.set(found);
              return !found;
            });
            return result.get();
          }
        }
      }
    }
  }

  private static CombinedInfraModel getCombinedModelForProject(Project project) {
    Set<InfraModel> allCombinedModels = new LinkedHashSet<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      Set<InfraModel> allModels = InfraManager.from(project).getAllModelsWithoutDependencies(module);
      allCombinedModels.addAll(allModels);
    }
    return new CombinedInfraModelImpl(allCombinedModels, null);
  }

  @Override
  public boolean hasAutoConfiguredModels(@Nullable Module module) {
    return module != null && InfraAutoConfiguredModels.hasAutoConfiguredModels(module);
  }
}
