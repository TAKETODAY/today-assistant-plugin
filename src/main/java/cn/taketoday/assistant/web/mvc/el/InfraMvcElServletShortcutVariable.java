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

package cn.taketoday.assistant.web.mvc.el;

import com.intellij.javaee.el.ELElementProcessor;
import com.intellij.javaee.el.ImplicitVariableWithCustomResolve;
import com.intellij.javaee.el.psi.ELExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;

import java.util.Collection;

import cn.taketoday.assistant.model.utils.light.InfraImplicitVariable;
import cn.taketoday.assistant.web.mvc.model.VariableProvider;

final class InfraMvcElServletShortcutVariable extends InfraImplicitVariable implements ImplicitVariableWithCustomResolve {
  private final VariableProvider variableProvider;

  public InfraMvcElServletShortcutVariable(PsiElement scope, String name, VariableProvider variableProvider) {
    super(name, WebMvcElServletShortcutVariableKt.createMapType(scope.getProject()), scope);
    this.variableProvider = variableProvider;
  }

  public boolean process(ELExpression element, ELElementProcessor processor) {
    Iterable<PsiVariable> variables = this.variableProvider.getVariables(element.getProject());
    if (!(variables instanceof Collection<PsiVariable> collection) || !collection.isEmpty()) {
      for (PsiVariable variable : variables) {
        if (!processor.processVariable(variable)) {
          return true;
        }
      }
    }
    return false;
  }
}
