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

import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.SlowFacetEditorValidator;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.psi.PsiFile;

import java.util.Collection;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFileSet;

public class UnmappedConfigurationFilesValidator extends FacetEditorValidator implements SlowFacetEditorValidator {
  private static final int MAX_FILES_TO_SHOW = 5;
  private final Set<InfraFileSet> myFileSets;
  private final Module myModule;

  public UnmappedConfigurationFilesValidator(Set<InfraFileSet> fileSets, Module module) {
    this.myFileSets = fileSets;
    this.myModule = module;
  }

  public ValidationResult check() {
    if (DumbService.isDumb(this.myModule.getProject()) || !ProjectInspectionProfileManager.getInstance(this.myModule.getProject()).isCurrentProfileInitialized()) {
      return ValidationResult.OK;
    }
    InfraUnmappedConfigurationFilesCollector collector = new InfraUnmappedConfigurationFilesCollector(this.myFileSets, this.myModule);
    if (!collector.isEnabledInProject()) {
      return ValidationResult.OK;
    }
    collector.collect();
    Collection<PsiFile> unmappedFiles = collector.getResults().get(this.myModule);
    if (unmappedFiles.isEmpty()) {
      return ValidationResult.OK;
    }
    else if (unmappedFiles.size() <= MAX_FILES_TO_SHOW) {
      String filesText = StringUtil.join(unmappedFiles, psiFile -> psiFile.getVirtualFile().getPresentableName(), "<br/>");
      return new ValidationResult(InfraBundle.message("unmapped.configuration.files.full", unmappedFiles.size(), filesText));
    }
    else {
      return new ValidationResult(InfraBundle.message("unmapped.configuration.files.short", unmappedFiles.size()));
    }
  }
}
