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

package cn.taketoday.assistant.facet.searchers;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;

public abstract class ConfigSearcherScopeModifier {
  private static final ExtensionPointName<ConfigSearcherScopeModifier> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.configSearcherScopeModifier");

  public abstract GlobalSearchScope modifyScope(Module module, GlobalSearchScope globalSearchScope);

  public static GlobalSearchScope runModifiers(Module module, GlobalSearchScope originalScope) {
    GlobalSearchScope scope = originalScope;
    for (ConfigSearcherScopeModifier modifier : EP_NAME.getExtensions()) {
      scope = modifier.modifyScope(module, scope);
    }
    return scope;
  }
}
