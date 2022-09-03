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

package cn.taketoday.assistant.context.model;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.Query;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentFactoryMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.assistant.model.ModelSearchParameters;

final class BeanClassSearchInheritorsCache {
  private static final Key<CachedValue<Map<PsiClass, Collection<ModelSearchParameters.BeanClass>>>> WITHOUT_TESTS = Key.create("BeanClassSearchInheritorsCache");
  private static final Key<CachedValue<Map<PsiClass, Collection<ModelSearchParameters.BeanClass>>>> INCLUDE_TESTS = Key.create("BeanClassSearchInheritorsCache_Tests");

  public static Collection<ModelSearchParameters.BeanClass> getInheritorSearchParameters(Module module, boolean isInTestSource, ModelSearchParameters.BeanClass parameters) {
    PsiClass searchClass = PsiTypesUtil.getPsiClass(parameters.getSearchType());
    if (searchClass == null) {
      return Collections.emptyList();
    }
    Map<PsiClass, Collection<ModelSearchParameters.BeanClass>> cache = getCache(module, isInTestSource);
    return cache.get(searchClass);
  }

  private static Map<PsiClass, Collection<ModelSearchParameters.BeanClass>> getCache(Module module, boolean isInTestSource) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, getCacheKey(isInTestSource), () -> {
      Map<PsiClass, Collection<ModelSearchParameters.BeanClass>> map = ConcurrentFactoryMap.createMap(searchClass -> {
        if (canSearchForInheritors(searchClass)) {
          SmartList smartList = new SmartList();
          GlobalSearchScope inheritorsSearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, isInTestSource);
          Query<PsiClass> inheritorsQuery = ClassInheritorsSearch.search(searchClass, inheritorsSearchScope, true, true, false);
          inheritorsQuery.forEach(psiClass -> {
            if (canBeInstantiated(psiClass)) {
              ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byClass(psiClass);
              smartList.add(searchParameters);
            }
          });
          return smartList.isEmpty() ? Collections.emptyList() : smartList;
        }
        return Collections.emptyList();
      });
      return CachedValueProvider.Result.create(map, PsiModificationTracker.MODIFICATION_COUNT);
    }, false);
  }

  private static Key<CachedValue<Map<PsiClass, Collection<ModelSearchParameters.BeanClass>>>> getCacheKey(boolean isInTestSource) {
    return isInTestSource ? INCLUDE_TESTS : WITHOUT_TESTS;
  }

  private static boolean canSearchForInheritors(PsiClass psiClass) {
    return !"java.lang.Object".equals(psiClass.getQualifiedName());
  }

  private static boolean canBeInstantiated(PsiClass inheritor) {
    return !inheritor.isInterface() && !inheritor.hasModifierProperty("abstract") && !inheritor.hasModifierProperty("private") && inheritor.getQualifiedName() != null;
  }
}
