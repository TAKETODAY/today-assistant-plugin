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
import com.intellij.facet.ui.ValidationResult;

import java.util.Map;
import java.util.Set;

import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.facet.InfraFileSet;

public final class FileSetDependenciesValidator extends FacetEditorValidator {
  private final Set<InfraFileSet> myFileSets;

  public FileSetDependenciesValidator(Set<InfraFileSet> fileSets) {
    this.myFileSets = fileSets;
  }

  public ValidationResult check() {
    return validateDependencies();
  }

  private ValidationResult validateDependencies() {
    InfraFileSetCycleChecker checker = new InfraFileSetCycleChecker(this.myFileSets);
    if (!checker.hasCycles()) {
      return ValidationResult.OK;
    }
    Map.Entry<InfraFileSet, InfraFileSet> cycle = checker.getCircularDependency();
    return new ValidationResult(InfraBundle.message("fileset.circular.dependencies", getFileSetDisplay(cycle.getKey()), getFileSetDisplay(cycle.getValue())));
  }

  private static String getFileSetDisplay(InfraFileSet springFileSet) {
    return springFileSet.getName() + " (" + springFileSet.getFacet().getModule().getName() + ")";
  }
}
