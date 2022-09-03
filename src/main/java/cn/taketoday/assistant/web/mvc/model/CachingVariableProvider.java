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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.HashMap;

import cn.taketoday.lang.Nullable;

final class CachingVariableProvider implements VariableProvider {
  private final Key<CachedValue<Iterable<PsiVariable>>> VAR_CACHE_KEY;

  private final VariableProvider[] providers;

  public VariableProvider[] getProviders() {
    return this.providers;
  }

  public CachingVariableProvider(VariableProvider... providers) {
    this.providers = providers;
    this.VAR_CACHE_KEY = Key.create("VAR_CACHE_KEY");
  }

  @Override

  public Iterable<PsiVariable> getVariables(Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, this.VAR_CACHE_KEY, new CachedValueProvider() {

      @Nullable
      public CachedValueProvider.Result<Iterable<PsiVariable>> compute() {
        HashMap set = new HashMap();
        for (VariableProvider provider : getProviders()) {
          Iterable<PsiVariable> variables = provider.getVariables(project);
          for (PsiVariable variable : variables) {
            String name = variable.getName();
            if (name != null) {
              set.put(name, variable);
            }
          }
        }
        return Result.create(set.values(), new Object[] { PsiModificationTracker.MODIFICATION_COUNT });
      }
    }, false);
  }
}
