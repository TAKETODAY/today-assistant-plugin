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

package cn.taketoday.assistant.model.xml.util.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Set;

import cn.taketoday.assistant.model.xml.AbstractDomInfraBean;
import cn.taketoday.assistant.model.xml.DomInfraBeanImpl;
import cn.taketoday.assistant.model.xml.util.UtilMap;
import cn.taketoday.lang.Nullable;

public abstract class UtilMapImpl extends DomInfraBeanImpl implements UtilMap {
  @Override
  @Nullable
  public PsiClass getBeanClass(@Nullable Set<AbstractDomInfraBean> visited, boolean considerFactories) {
    String explicitMapClass = getMapClass().getStringValue();
    Project project = getContainingFile().getProject();
    String mapClass = StringUtil.isNotEmpty(explicitMapClass) ? explicitMapClass : "java.util.Map";
    return JavaPsiFacade.getInstance(project).findClass(mapClass, GlobalSearchScope.allScope(project));
  }
}
