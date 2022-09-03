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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElementKt;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.assistant.core.StrategiesManager;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;

import static cn.taketoday.assistant.InfraBundle.message;

public final class TodayFacetUastInspection extends AbstractInfraLocalInspection {

  public TodayFacetUastInspection() {
    super(UClass.class);
  }

  public ProblemDescriptor[] checkClass(UClass uClass, InspectionManager manager, boolean isOnTheFly) {
    LocalQuickFix createTodayFacetFix;
    PsiClass psiClass = uClass.getJavaPsi();
    if (!InfraLibraryUtil.hasLibrary(psiClass.getProject())) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (psiClass.hasModifierProperty("static")) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (!InfraUtils.isConfigurationOrMeta(psiClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    PsiElement sourcePsi = uClass.getSourcePsi();
    if (sourcePsi == null) {
      return null;
    }
    PsiFile containingFile = sourcePsi.getContainingFile();
    if (InfraModelService.of().isUsedConfigurationFile(containingFile, false)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    VirtualFile virtualFile = containingFile.getVirtualFile();
    Module module = ModuleUtilCore.findModuleForFile(virtualFile, psiClass.getProject());
    if (module == null) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (!StrategiesManager.from(module)
            .processClassesListValues(false, psiClass.getQualifiedName(), (property, aClass) -> !aClass.isEquivalentTo(psiClass))) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    ModelSearchParameters.BeanClass params = ModelSearchParameters.byClass(psiClass);
    for (CommonInfraModel springModel : InfraManager.from(psiClass.getProject()).getAllModels(module)) {
      if (InfraModelSearchers.doesBeanExist(springModel, params)) {
        return ProblemDescriptor.EMPTY_ARRAY;
      }
    }
    if (InfraUtils.hasFacet(module)) {
      createTodayFacetFix = new ConfigureFileSetFix(module, virtualFile);
    }
    else {
      createTodayFacetFix = new CreateTodayFacetFix(module);
    }
    LocalQuickFix fix = createTodayFacetFix;
    PsiElement identifier = UElementKt.getSourcePsiElement(uClass.getUastAnchor());
    PsiElement highlightElement = identifier != null ? identifier : sourcePsi;
    ProblemDescriptor descriptor = manager.createProblemDescriptor(highlightElement,
            message("infra.facet.inspection.context.not.configured.for.file"), fix,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
    return new ProblemDescriptor[] { descriptor };
  }
}
