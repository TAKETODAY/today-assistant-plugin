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
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomService;
import com.intellij.xml.config.ConfigFileSearcher;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.model.xml.beans.Beans;
import cn.taketoday.lang.Nullable;

public class XmlConfigSearcher extends ConfigFileSearcher {
  private final GlobalSearchScope myScope;

  public XmlConfigSearcher(Module module) {
    this(module, true);
  }

  public XmlConfigSearcher(Module module, boolean withDependenciesAndLibraries) {
    super(module, module.getProject());
    GlobalSearchScope notTestsScope = GlobalSearchScope.notScope(module.getModuleTestsWithDependentsScope());
    GlobalSearchScope moduleContentWithoutTests = module.getModuleContentScope().intersectWith(notTestsScope);
    GlobalSearchScope scope = withDependenciesAndLibraries ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false) : GlobalSearchScope.moduleScope(module);
    this.myScope = ConfigSearcherScopeModifier.runModifiers(module, scope.union(moduleContentWithoutTests).intersectWith(notTestsScope));
  }

  public Set<PsiFile> search(@Nullable Module module, Project project) {
    if (module == null) {
      return Collections.emptySet();
    }
    List<DomFileElement<Beans>> elements = DomService.getInstance().getFileElements(Beans.class, project, this.myScope);
    return ContainerUtil.map2Set(elements, DomFileElement::getFile);
  }
}
