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
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.xml.highlighting.DomElementAnnotationsManager;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFacet;

class CreateTodayFacetFix implements LocalQuickFix {
  private final Module myModule;

  public CreateTodayFacetFix(Module module) {
    this.myModule = module;
  }

  public String getFamilyName() {
    return InfraBundle.message("infra.facet.inspection.create.facet");
  }

  public boolean startInWriteAction() {
    return false;
  }

  public void applyFix(Project project, ProblemDescriptor descriptor) {
    InfraFacet facet = FacetManager.getInstance(this.myModule).findFacet(InfraFacet.FACET_TYPE_ID, "Spring");
    if (facet != null) {
      return;
    }
    InfraFacet infraFacet = WriteAction.compute(() -> FacetManager.getInstance(this.myModule)
            .addFacet(InfraFacet.getFacetType(), "Spring", null));
    DomElementAnnotationsManager.getInstance(project).dropAnnotationsCache();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    EditorNotifications.getInstance(project).updateAllNotifications();
    ModulesConfigurator.showFacetSettingsDialog(infraFacet, null);
  }
}
