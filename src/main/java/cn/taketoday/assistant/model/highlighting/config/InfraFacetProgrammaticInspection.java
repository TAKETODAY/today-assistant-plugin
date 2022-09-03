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

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;

import java.util.List;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.facet.validation.ProgrammaticConfigurationCollector;
import cn.taketoday.assistant.util.InfraUtils;

public final class InfraFacetProgrammaticInspection extends AbstractBaseJavaLocalInspectionTool {

  public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
    if (!ApplicationManager.getApplication().isInternal()) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    Module module = ModuleUtilCore.findModuleForFile(holder.getFile());
    if (!InfraLibraryUtil.hasLibrary(module)) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    return super.buildVisitor(holder, isOnTheFly);
  }

  public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
    Project project = manager.getProject();
    ProgrammaticConfigurationCollector collector = new ProgrammaticConfigurationCollector(project);
    collector.collect(new EmptyProgressIndicator(), GlobalSearchScope.fileScope(file));
    List<PsiElement> results = collector.getResults();
    if (results.isEmpty()) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    boolean hasTodayFacet = InfraUtils.hasFacet(module);
    SmartList<ProblemDescriptor> smartList = new SmartList<>();
    for (PsiElement element : results) {
      smartList.add(manager.createProblemDescriptor(element, InfraBundle.message("facet.programmatic.inspection.context"),
              hasTodayFacet ? new SetupProgrammaticContextFix(element) : null, ProblemHighlightType.WEAK_WARNING, isOnTheFly));
    }
    return smartList.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }
}
