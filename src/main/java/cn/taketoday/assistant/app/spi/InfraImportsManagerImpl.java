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

package cn.taketoday.assistant.app.spi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairProcessor;

import java.util.List;

import cn.taketoday.assistant.core.StrategiesManager;
import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.lang.Nullable;

public final class InfraImportsManagerImpl extends InfraImportsManager {
  private final Module module;

  public InfraImportsManagerImpl(Module module) {
    this.module = module;
  }

  @Override

  public List<PsiClass> getClasses(boolean includeTests, String key) {
    Project project = this.module.getProject();
    return InfraImportsFileIndex.getClasses(project, getScope(includeTests), key);
  }

  @Override
  public boolean processValues(boolean includeTests, @Nullable String valueHint, PairProcessor<PsiElement, PsiClass> pairProcessor) {
    Project project = this.module.getProject();
    return InfraImportsFileIndex.processValues(project, getScope(includeTests), valueHint, pairProcessor);
  }

  @Override
  public List<PsiClass> getAutoConfigurationClasses(boolean includeTests) {
    return getAutoConfigurationClasses(getScope(includeTests));
  }

  @Override
  public List<PsiClass> getAutoConfigurationClasses(GlobalSearchScope scope) {
    List<PsiClass> result = StrategiesManager.from(this.module)
            .getClassesListValue(InfraConfigConstant.ENABLE_AUTO_CONFIGURATION, scope);
    Project project = this.module.getProject();
    result.addAll(InfraImportsFileIndex.getClasses(project, scope, InfraConfigConstant.AUTO_CONFIGURATION));
    return result;
  }

  private GlobalSearchScope getScope(boolean includeTests) {
    return GlobalSearchScope.moduleRuntimeScope(this.module, includeTests);
  }
}
