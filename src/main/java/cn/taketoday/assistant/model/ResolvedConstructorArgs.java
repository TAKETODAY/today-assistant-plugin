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

package cn.taketoday.assistant.model;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.model.xml.beans.ConstructorArgDefinition;
import cn.taketoday.lang.Nullable;

/**
 * @author Dmitry Avdeev
 */
public interface ResolvedConstructorArgs {

  boolean isResolved();

  @Nullable
  PsiMethod getResolvedMethod();

  List<PsiMethod> getCheckedMethods();

  /**
   * @return null if {@link #getResolvedMethod()} returns null.
   */
  @Nullable
  Map<ConstructorArgDefinition, PsiParameter> getResolvedArgs();

  @Nullable
  Map<ConstructorArgDefinition, PsiParameter> getResolvedArgs(PsiMethod method);

  @Nullable
  Map<PsiParameter, Collection<BeanPointer<?>>> getAutowiredParams(PsiMethod method);

  List<PsiMethod> getCandidates();
}
