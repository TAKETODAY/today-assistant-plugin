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

package cn.taketoday.assistant.navigation;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider;
import com.intellij.xml.util.PsiElementPointer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraManager;
import cn.taketoday.assistant.context.model.InfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.util.InfraUtils;

final class GotoInfraBeanProvider extends GoToSymbolProvider {

  protected void addNames(Module module, Set<String> result) {
    Set<String> names = new HashSet<>();
    for (InfraModel infraModel : getModels(module)) {
      for (BeanPointer pointer : infraModel.getAllCommonBeans()) {
        String name = pointer.getName();
        if (StringUtil.isNotEmpty(name)) {
          names.add(name);
          for (String alias : pointer.getAliases()) {
            if (StringUtil.isNotEmpty(alias)) {
              names.add(alias);
            }
          }
        }
      }
    }
    result.addAll(names);
  }

  protected void addItems(Module module, String name, List<NavigationItem> result) {
    for (InfraModel infraModel : getModels(module)) {
      for (PsiElementPointer pointer : InfraModelSearchers.findBeans(infraModel, name)) {
        PsiElement element = pointer.getPsiElement();
        if (element instanceof NavigationItem) {
          result.add((NavigationItem) element);
        }
      }
    }
  }

  protected boolean acceptModule(Module module) {
    return InfraUtils.hasFacet(module) || InfraModelService.of().hasAutoConfiguredModels(module);
  }

  private static Set<InfraModel> getModels(Module module) {
    return InfraManager.from(module.getProject()).getAllModelsWithoutDependencies(module);
  }
}
