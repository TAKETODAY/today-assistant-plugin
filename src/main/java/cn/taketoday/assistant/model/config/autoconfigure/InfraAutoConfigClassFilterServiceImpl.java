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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class InfraAutoConfigClassFilterServiceImpl extends InfraAutoConfigClassFilterService {

  @Override
  public List<PsiClass> filterByConditionalOnClass(Module module, List<PsiClass> configs) {
    GlobalSearchScope searchScope = GlobalSearchScope.moduleRuntimeScope(module, false);
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
    List<PsiClass> enabled = new ArrayList<>(configs.size() / 2);
    for (PsiClass config : configs) {
      AutoConfigureMetadataIndex.AutoConfigureMetadata metadata = AutoConfigureMetadataIndex.findMetadata(config);
      if (metadata != null) {
        boolean foundAll = true;
        Iterator<String> it = metadata.getConditionalOnClass().iterator();
        while (true) {
          if (!it.hasNext()) {
            break;
          }
          String conditionalClass = it.next();
          PsiClass aClass = javaPsiFacade.findClass(conditionalClass, searchScope);
          if (aClass == null) {
            foundAll = false;
            break;
          }
        }
        if (foundAll) {
          enabled.add(config);
        }
      }
      else if (AutoConfigClassSorter.passesConditionalClassMatch(config)) {
        enabled.add(config);
      }
    }
    return enabled;
  }
}
