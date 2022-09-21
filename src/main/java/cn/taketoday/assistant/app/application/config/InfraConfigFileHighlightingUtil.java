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
package cn.taketoday.assistant.app.application.config;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.PropertiesBundle;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKey.Deprecation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.searches.ReferencesSearch;

import static cn.taketoday.assistant.InfraAppBundle.message;

public class InfraConfigFileHighlightingUtil {
  private final ProblemsHolder myHolder;

  public InfraConfigFileHighlightingUtil(ProblemsHolder holder) {
    myHolder = holder;
  }

  public void highlightDeprecatedConfigKey(PsiElement keyElement,
          MetaConfigKey configKey, LocalQuickFix... quickFixes) {
    Deprecation deprecation = configKey.getDeprecation();
    String reasonShortText = deprecation.getReason().getShortText();
    String reason = StringUtil.isNotEmpty(reasonShortText)
                    ? message("InfraConfigFileHighlightingUtil.deprecated", reasonShortText)
                    : message("InfraConfigFileHighlightingUtil.deprecated.configuration.property", configKey.getName());

    ProblemHighlightType problemHighlightType
            = deprecation.getLevel() == Deprecation.DeprecationLevel.ERROR
              ? ProblemHighlightType.GENERIC_ERROR
              : ProblemHighlightType.LIKE_DEPRECATED;

    myHolder.registerProblem(keyElement, reason, problemHighlightType, quickFixes);
  }

  public void highlightUnresolvedConfigKey(PsiElement keyElement,
          PsiElement elementToSearch, String qualifiedConfigKeyName, boolean isOnTheFly) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      if (indicator.isCanceled())
        return;

      indicator.setText(PropertiesBundle.message("searching.for.property.key.progress.text", qualifiedConfigKeyName));
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(keyElement);
    if (module == null)
      return;

    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependentsScope(module);
    boolean zeroOccurrences = false;
    if (isOnTheFly) {
      PsiSearchHelper.SearchCostResult cheapEnough =
              PsiSearchHelper.getInstance(keyElement.getProject()).isCheapEnoughToSearch(qualifiedConfigKeyName, scope, null, indicator);
      if (cheapEnough == PsiSearchHelper.SearchCostResult.ZERO_OCCURRENCES) {
        zeroOccurrences = true;
      }
      else if (cheapEnough == PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES) {
        return;
      }
    }

    if (zeroOccurrences ||
            ReferencesSearch.search(elementToSearch, scope, false).findFirst() == null) {
      myHolder.registerProblem(keyElement,
              message("InfraConfigFileHighlightingUtil.cannot.resolve.configuration.property", qualifiedConfigKeyName),
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }
}
