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

package cn.taketoday.assistant.model.highlighting;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.util.InspectionValidator;
import com.intellij.openapi.compiler.util.InspectionValidatorUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraInspectionsRegistry;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.InfraModelVisitorUtils;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public class InfraValidator extends InspectionValidator {

  public InfraValidator() {
    super("Infra Model Validator", InfraBundle.message("model.inspection.validator.description.text"),
            message("model.inspection.validator.progress.text"));
  }

  private static boolean isAvailableOnModule(Module module) {
    return InfraUtils.hasFacet(module) || InfraModelService.of().hasAutoConfiguredModels(module);
  }

  public Class<? extends LocalInspectionTool>[] getInspectionToolClasses(CompileContext context) {
    return InfraInspectionsRegistry.getInstance().getSpringInspectionClasses();
  }

  public boolean isAvailableOnScope(CompileScope scope) {
    for (Module module : scope.getAffectedModules()) {
      if (isAvailableOnModule(module)) {
        return true;
      }
    }
    return false;
  }

  public Collection<VirtualFile> getFilesToProcess(Project project, CompileContext context) {
    Set<VirtualFile> files = new LinkedHashSet<>();
    for (Module module : context.getCompileScope().getAffectedModules()) {
      Set<InfraModel> models = InfraManager.from(project).getAllModels(module);
      for (InfraModel model : models) {
        for (PsiFile configFile : InfraModelVisitorUtils.getConfigFiles(model)) {
          VirtualFile file = configFile.getVirtualFile();
          ContainerUtil.addIfNotNull(files, file);
        }
      }
    }
    for (InfraInspectionsRegistry.AdditionalFilesContributor contributor : getAdditionalFilesContributors()) {
      files.addAll(contributor.getAdditionalFilesToProcess(project, context));
    }
    InspectionValidatorUtil.expandCompileScopeIfNeeded(files, context);
    return files;
  }

  public Map<ProblemDescriptor, HighlightDisplayLevel> checkAdditionally(PsiFile topFile) {
    Map<ProblemDescriptor, HighlightDisplayLevel> additionalHighlighting = new LinkedHashMap<>();
    for (InfraInspectionsRegistry.AdditionalFilesContributor contributor : getAdditionalFilesContributors()) {
      additionalHighlighting.putAll(contributor.checkAdditionally(topFile));
    }
    return additionalHighlighting;
  }

  private static InfraInspectionsRegistry.AdditionalFilesContributor[] getAdditionalFilesContributors() {
    return InfraInspectionsRegistry.AdditionalFilesContributor.EP_NAME.getExtensions();
  }
}
