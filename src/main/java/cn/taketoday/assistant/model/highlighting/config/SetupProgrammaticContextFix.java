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

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Set;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.InfraFileSetService;

import static cn.taketoday.assistant.InfraBundle.message;

class SetupProgrammaticContextFix extends LocalQuickFixOnPsiElement {
  public SetupProgrammaticContextFix(PsiElement element) {
    super(element);
  }

  public boolean startInWriteAction() {
    return false;
  }

  public String getText() {
    return message("setup.programmatic.context.quick.fix.setup.infra.context");
  }

  public String getFamilyName() {
    return getText();
  }

  public void invoke(Project project, PsiFile file, PsiElement startElement, PsiElement endElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    InfraFacet infraFacet = InfraFacet.from(module);
    InfraFileSet fileSet = createFileSet(startElement, infraFacet);
    int dialog = Messages.showYesNoDialog(project, message("setup.programmatic.context.quick.fix.context.with.name", fileSet.getName()),
            message("setup.programmatic.context.quick.fix.settings.displayName"), null);
    if (dialog == 0) {
      ModulesConfigurator.showFacetSettingsDialog(infraFacet, null);
    }
  }

  private static InfraFileSet createFileSet(PsiElement startElement, InfraFacet infraFacet) {
    InfraFileSetService fileSetService = InfraFileSetService.of();
    Set<InfraFileSet> existingFileSets = infraFacet.getFileSets();
    String id = fileSetService.getUniqueId(existingFileSets);
    PsiClass psiClass = PsiTreeUtil.getParentOfType(startElement, PsiClass.class);
    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(startElement, PsiMethod.class);
    String codeLocation = (psiClass != null ? psiClass.getName() : startElement.getContainingFile().getName()) + (psiMethod != null ? ":" + psiMethod.getName() : "");
    String name = fileSetService.getUniqueName("Programmatic Context " + codeLocation, existingFileSets);
    InfraFileSet fileSet = infraFacet.addFileSet(id, name);
    FacetManager.getInstance(infraFacet.getModule()).facetConfigurationChanged(infraFacet);
    return fileSet;
  }
}
