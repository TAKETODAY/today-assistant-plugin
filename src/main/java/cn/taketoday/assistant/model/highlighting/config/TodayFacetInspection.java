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

package cn.taketoday.assistant.model.highlighting.config;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;

import javax.swing.JComponent;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.model.highlighting.dom.InfraBeanInspectionBase;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.assistant.util.InfraUtils;

public final class TodayFacetInspection extends InfraBeanInspectionBase {
  public boolean checkTestFiles;

  @Override
  public void checkFileElement(DomFileElement<Beans> domFileElement, DomElementAnnotationHolder holder) {
    VirtualFile virtualFile;
    Module module;
    LocalQuickFix createTodayFacetFix;
    PsiFile file = domFileElement.getFile();
    if (InfraModelService.of().isUsedConfigurationFile(file, this.checkTestFiles) || (module = ModuleUtilCore.findModuleForFile((virtualFile = file.getVirtualFile()),
            file.getProject())) == null) {
      return;
    }
    if (InfraUtils.hasFacet(module)) {
      createTodayFacetFix = new ConfigureFileSetFix(module, virtualFile);
    }
    else {
      createTodayFacetFix = new CreateTodayFacetFix(module);
    }
    LocalQuickFix fix = createTodayFacetFix;
    holder.createProblem(domFileElement, HighlightSeverity.WARNING, InfraBundle.message("infra.facet.inspection.context.not.configured.for.file"), fix);
  }

  public JComponent createOptionsPanel() {
    return new SingleCheckboxOptionsPanel(InfraBundle.message("infra.facet.inspection.check.test.files"), this, "checkTestFiles");
  }
}
