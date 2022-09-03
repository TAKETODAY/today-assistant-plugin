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
package cn.taketoday.assistant.model.jam.utils.filters;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;

public class IncludeCustomFilter extends InfraContextFilter.IncludeClasses {

  public IncludeCustomFilter(Collection<PsiClass> classes) {
    super(classes);
  }

  @Override
  public Set<InfraStereotypeElement> includeStereotypes(Module module, Set<PsiPackage> packages) {
    // todo: we can't invoke custom filters.

    return Collections.emptySet();
  }
}
