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

package cn.taketoday.assistant.beans;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import com.intellij.spring.facet.searchers.ConfigSearcherScopeModifier;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 14:54
 */
public class PackagesConfigSearcherScopeModifier extends ConfigSearcherScopeModifier {

  @Override
  public GlobalSearchScope modifyScope(Module module, GlobalSearchScope originalScope) {
    PsiPackage psiPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage("cn.taketoday");
    if (psiPackage != null) {
      return originalScope.intersectWith(GlobalSearchScope.notScope(PackageScope.packageScope(psiPackage, true)));
    }
    else {
      return originalScope;
    }
  }
}
