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
package cn.taketoday.assistant.model.scope;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.context.model.InfraModel;

public abstract class InfraCustomBeanScope {

  public static final ExtensionPointName<InfraCustomBeanScope> EP_NAME =
          ExtensionPointName.create("cn.taketoday.assistant.customBeanScope");

  public abstract String getScopeClassName();

  /**
   * @param scopes All custom scopes.
   * @param models All relevant models.
   * @param scopeClass Scope class determined by {@link #getScopeClassName()}.
   * @param psiElement Current element.
   * @return {@code true} to continue processing, {@code false} otherwise.
   */
  public abstract boolean process(List<BeanScope> scopes,
          Set<InfraModel> models, PsiClass scopeClass, PsiElement psiElement);
}
