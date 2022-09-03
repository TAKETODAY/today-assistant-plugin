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

package cn.taketoday.assistant.app.run.lifecycle.beans.model;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xml.util.PsiElementPointer;

import java.util.Set;

import javax.swing.Icon;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.lang.Nullable;

public interface LiveBean {

  interface LiveResourcePointer extends PsiElementPointer {
  }

  String getId();

  @Nullable
  String getScope();

  @Nullable
  String getType();

  @Nullable
  LiveResource getResource();

  Set<LiveBean> getDependencies();

  Set<LiveBean> getInjectedInto();

  boolean isInnerBean();

  String getName();

  String getClassName();

  @Nullable
  PsiClass findBeanClass(Project project, GlobalSearchScope globalSearchScope);

  boolean matches(PsiClass psiClass);

  PsiElementPointer findBeanPointer(@Nullable PsiClass psiClass, @Nullable PsiElement psiElement, @Nullable CommonInfraModel infraModel);

  Icon getIcon();
}
