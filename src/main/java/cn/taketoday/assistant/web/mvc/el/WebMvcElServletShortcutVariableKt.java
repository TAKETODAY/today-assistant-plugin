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
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;

import cn.taketoday.assistant.web.mvc.model.VariableProvider;
import cn.taketoday.assistant.web.mvc.model.WebMvcVariablesService;
import cn.taketoday.lang.Nullable;

public final class WebMvcElServletShortcutVariableKt {

  public static boolean processShortcuts(PsiElement scope, ELElementProcessor processor) {
    PsiVariable variable = getShortcut(processor.getNameHint(), scope);
    if (variable != null) {
      return processor.processVariable(variable);
    }
    return true;
  }

  private static PsiVariable getShortcut(String name, PsiElement scope) {
    VariableProvider provider = getProvider(name);
    if (provider != null) {
      return new InfraMvcElServletShortcutVariable(scope, name, provider);
    }
    return null;
  }

  @Nullable
  private static VariableProvider getProvider(String name) {
    if (name != null) {
      WebMvcVariablesService variablesService = WebMvcVariablesService.of();
      return switch (name) {
        case "sessionScope" -> variablesService::getSessionVariables;
        case "requestScope" -> variablesService::getRequestVariables;
        case "applicationScope" -> variablesService::getApplicationVariables;
        default -> null;
      };
    }
    return null;
  }

  public static PsiClassType createMapType(Project project) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
    PsiType createTypeFromText = javaPsiFacade.getElementFactory().createTypeFromText("java.util.Map<java.lang.String, java.lang.Object>", null);
    return (PsiClassType) createTypeFromText;
  }

}
