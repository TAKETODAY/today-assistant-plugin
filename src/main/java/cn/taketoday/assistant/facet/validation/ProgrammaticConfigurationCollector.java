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

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.SuppressionUtil;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.facet.InfraFrameworkDetector;
import cn.taketoday.assistant.model.highlighting.config.InfraFacetProgrammaticInspection;

import static cn.taketoday.assistant.InfraBundle.message;

public class ProgrammaticConfigurationCollector {
  private static final Logger LOG = Logger.getInstance(ProgrammaticConfigurationCollector.class);
  private static final String CONTEXT_BASE_CLASS = "cn.taketoday.context.support.ClassPathXmlApplicationContext";

  private final Project project;
  private final InspectionProfile profile;
  private final boolean myInspectionEnabled;
  private final List<PsiElement> results = new SmartList<>();
  private final DetectionExcludesConfiguration myDetectionExcludesConfiguration;

  private static final LocalInspectionTool PROGRAMMATIC_CONFIG_INSPECTION = new InfraFacetProgrammaticInspection();

  public ProgrammaticConfigurationCollector(Project project) {
    this.project = project;
    this.myDetectionExcludesConfiguration = DetectionExcludesConfiguration.getInstance(project);
    ProjectInspectionProfileManager profileManager = ProjectInspectionProfileManager.getInstance(project);
    this.profile = profileManager.getCurrentProfile();
    this.myInspectionEnabled = this.profile.isToolEnabled(HighlightDisplayKey.find(PROGRAMMATIC_CONFIG_INSPECTION.getID()));
  }

  public boolean isEnabledInProject() {
    return this.myInspectionEnabled;
  }

  public List<PsiElement> getResults() {
    return this.results;
  }

  public void collect(ProgressIndicator indicator) {
    collect(indicator, GlobalSearchScopesCore.projectProductionScope(this.project));
  }

  public void collect(ProgressIndicator indicator, GlobalSearchScope searchScope) {
    Collection<PsiClass> applicationContextInheritors = findAllApplicationContextInheritors();
    if (applicationContextInheritors.isEmpty()) {
      return;
    }
    indicator.setText(message("scanning.for.programmatic.contexts"));
    indicator.setFraction(0.0d);
    long start = System.currentTimeMillis();
    int i = 0;
    for (PsiClass inheritor : applicationContextInheritors) {
      collectCtorReferences(inheritor, searchScope);
      int i2 = i;
      i++;
      indicator.setFraction(i2 / applicationContextInheritors.size());
    }
    long end = System.currentTimeMillis();
    LOG.debug("collect: " + (end - start) + "ms");
  }

  private Collection<PsiClass> findAllApplicationContextInheritors() {
    long start = System.currentTimeMillis();
    GlobalSearchScope librariesScope = ProjectScope.getLibrariesScope(this.project);
    PsiClass contextBaseClass = JavaPsiFacade.getInstance(this.project)
            .findClass(CONTEXT_BASE_CLASS, librariesScope);
    if (contextBaseClass == null) {
      return Collections.emptyList();
    }
    Query<PsiClass> allContextImplementations = ClassInheritorsSearch.search(contextBaseClass, GlobalSearchScopesCore.projectProductionScope(this.project).union(librariesScope), true);
    Collection<PsiClass> all = allContextImplementations.findAll();
    all.add(contextBaseClass);
    long end = System.currentTimeMillis();
    Logger logger = LOG;
    all.size();
    logger.debug("findAllApplicationContextInheritors: " + (end - start) + "ms #" + logger);
    return all;
  }

  private void collectCtorReferences(PsiClass contextPsiClass, SearchScope searchScope) {
    Query<PsiReference> search = ReferencesSearch.search(contextPsiClass, searchScope);
    search.forEach(reference -> {
      PsiElement element = reference.getElement();
      PsiElement expression = element.getParent();
      if (expression instanceof PsiAnonymousClass) {
        expression = expression.getParent();
      }
      if ((expression instanceof PsiNewExpression) && !skipConfigInspectionFor(expression, PROGRAMMATIC_CONFIG_INSPECTION)) {
        this.results.add(expression.getNavigationElement());
        return true;
      }
      return true;
    });
  }

  private boolean skipConfigInspectionFor(PsiElement place, LocalInspectionTool tool) {
    if (this.myDetectionExcludesConfiguration.isExcludedFromDetection(place.getContainingFile().getVirtualFile(), InfraFrameworkDetector.getSpringFrameworkType())) {
      return true;
    }
    HighlightDisplayKey toolHighlightDisplayKey = HighlightDisplayKey.find(tool.getID());
    return !this.profile.isToolEnabled(toolHighlightDisplayKey, place) || SuppressionUtil.inspectionResultSuppressed(place, tool);
  }
}
