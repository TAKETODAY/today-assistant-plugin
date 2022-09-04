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

package cn.taketoday.assistant.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraModificationTrackersManager;
import cn.taketoday.assistant.app.InfraApplicationService;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.core.StrategiesManager;
import cn.taketoday.assistant.facet.InfraAutodetectedFileSet;
import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFacetConfiguration;
import cn.taketoday.assistant.model.config.autoconfigure.AutoConfigClassCollector;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContextBase;
import cn.taketoday.assistant.model.config.autoconfigure.jam.EnableAutoConfiguration;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/28 00:11
 */
public class DefaultInfraModelProvider implements InfraModelProvider {

  @Override
  public String getName() {
    return "Infrastructure-Provider";
  }

  @Override
  public List<? extends InfraAutodetectedFileSet> getFilesets(InfraFacet facet) {
    Module currentFacetModule = facet.getModule();
    if (!InfraLibraryUtil.hasLibrary(currentFacetModule)) {
      return Collections.emptyList();
    }

    Project project = currentFacetModule.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(facet, () -> {
      Module module = facet.getModule();
      List<PsiClass> applications = InfraApplicationService.of().getInfraApplications(module);
      if (applications.size() > 1) {
        return CachedValueProvider.Result.create(Collections.emptyList(), PsiModificationTracker.MODIFICATION_COUNT);
      }
      else {
        PsiClass applicationClass = ContainerUtil.getFirstItem(applications);
        InfraAutodetectedFileSet fileSet =
                applicationClass != null && applicationClass.getQualifiedName() != null
                ? getApplicationFileSet(applicationClass, facet)
                : getStarterFileSet(facet);
        if (fileSet == null) {
          return CachedValueProvider.Result.create(Collections.emptyList(), PsiModificationTracker.MODIFICATION_COUNT);
        }
        else {
          InfraFacetConfiguration configuration = facet.getConfiguration();
          configuration.registerAutodetectedFileSet(fileSet);
          List<InfraAutodetectedFileSet> fileSets = new SmartList<>(fileSet);

          Object[] outerModelsTracker = InfraModificationTrackersManager.from(module.getProject()).getOuterModelsDependencies();
          return CachedValueProvider.Result.create(fileSets, ArrayUtil.append(outerModelsTracker, configuration.getSettingsModificationTracker()));
        }
      }
    });
  }

  private static InfraAutodetectedFileSet getApplicationFileSet(PsiClass applicationClass, InfraFacet facet) {
    InfraApplicationFileSet fileSet = new InfraApplicationFileSet(applicationClass, facet);

    List<VirtualFile> configFiles = InfraConfigurationFileService.of()
            .findConfigFiles(facet.getModule(), false, contributor -> {
              return contributor.accept(fileSet);
            });

    for (VirtualFile configFile : configFiles) {
      fileSet.addFile(configFile);
    }

    List<VirtualFile> imports = InfraConfigurationFileService.of().collectImports(facet.getModule(), configFiles);

    for (VirtualFile importedFile : imports) {
      fileSet.addFile(importedFile);
    }

    return fileSet;
  }

  private static InfraAutodetectedFileSet getStarterFileSet(InfraFacet facet) {
    Module module = facet.getModule();
    List<PsiClass> moduleAutoConfigs = StrategiesManager.from(module).getClassesListValue(
            AnnotationConstant.EnableAutoConfiguration, module.getModuleScope(false));
    PsiClass contextClass = ContainerUtil.getFirstItem(moduleAutoConfigs);
    if (contextClass == null) {
      return null;
    }
    else {
      ConditionalOnEvaluationContextBase context = new ConditionalOnEvaluationContextBase(contextClass, module, null, NotNullLazyValue.lazy(Collections::emptyList), null);
      List<PsiClass> autoConfigs = AutoConfigClassCollector.collectConfigurationClasses((EnableAutoConfiguration) null, context);
      InfraAutodetectedFileSet bootFileSet = new InfraStarterFileSet(facet);
      autoConfigs.forEach((psiClass) -> {
        bootFileSet.addFile(psiClass.getContainingFile().getVirtualFile());
      });
      return bootFileSet;
    }
  }

  static class InfraApplicationFileSet extends InfraAutodetectedFileSet {
    static final String ID_PREFIX = "today_infrastructure_";
    private static final LayeredIcon ICON = new LayeredIcon(Icons.FileSet, Icons.TodayOverlay);

    InfraApplicationFileSet(PsiClass application, InfraFacet facet) {
      super(ID_PREFIX + application.getQualifiedName(), application.getName(), facet, ICON);
      addFile(application.getContainingFile().getVirtualFile());
    }
  }

  static class InfraStarterFileSet extends InfraAutodetectedFileSet {
    static final String ID_PREFIX = "starter_today_infrastructure";
    private static final LayeredIcon ICON = new LayeredIcon(Icons.FileSet, Icons.TodayOverlay);

    InfraStarterFileSet(InfraFacet facet) {
      super(ID_PREFIX, "Starter", facet, ICON);
    }

  }

}
