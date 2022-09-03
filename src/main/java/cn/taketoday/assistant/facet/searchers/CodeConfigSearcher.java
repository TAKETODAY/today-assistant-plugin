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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.config.ConfigFileSearcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.Configuration;
import cn.taketoday.assistant.beans.stereotype.InfraJamModel;
import cn.taketoday.lang.Nullable;

public class CodeConfigSearcher extends ConfigFileSearcher {
  private final GlobalSearchScope myScope;

  public CodeConfigSearcher(Module context) {
    this(context, true);
  }

  public CodeConfigSearcher(Module context, boolean withDependenciesAndLibraries) {
    super(context, context.getProject());
    GlobalSearchScope moduleScope;
    if (withDependenciesAndLibraries) {
      moduleScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(context, false);
    }
    else {
      moduleScope = context.getModuleScope(false);
    }
    GlobalSearchScope scope = moduleScope;
    this.myScope = ConfigSearcherScopeModifier.runModifiers(context, scope);
  }

  public Set<PsiFile> search(@Nullable Module module, Project project) {
    if (module == null) {
      return Collections.emptySet();
    }
    Set<PsiFile> files = new HashSet<>();
    for (Configuration configuration : InfraJamModel.from(module).getConfigurations(this.myScope)) {
      PsiFile file = configuration.getContainingFile();
      ContainerUtil.addIfNotNull(files, file);
    }
    return files;
  }
}
