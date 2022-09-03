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


package cn.taketoday.assistant.spel;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.impl.light.LightMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.spring.el.contextProviders.SpringElContextsExtension;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UAnnotationUtils;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import kotlin.Pair;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
final class EventListenerContextVariables extends SpringElContextsExtension {

  @Override
  public Collection<? extends PsiVariable> getContextVariables(PsiElement context) {
    PsiElement element = context.getContext();
    if (element == null) {
      return Collections.emptyList();
    }
    else {
      Pair<PsiAnnotation, String> annotationEntry = UAnnotationUtils.getContainingAnnotationEntry(UastContextKt.toUElement(element));
      if (annotationEntry == null) {
        return Collections.emptyList();
      }
      else {
        PsiAnnotation annotation = annotationEntry.getFirst();
        Module module = ModuleUtilCore.findModuleForPsiElement(annotation);
        if (!InfraLibraryUtil.hasLibrary(module)) {
          return Collections.emptyList();
        }
        else {
          String annoFqn = annotation.getQualifiedName();
          if (annoFqn == null
                  || !"cn.taketoday.context.event.EventListener".equals(annoFqn)
                  && !AliasedAttributeInjectionContext.isCustomAnnotation(module, annoFqn, "cn.taketoday.context.event.EventListener")) {
            return Collections.emptyList();

          }
          else {
            SmartList<PsiVariable> variables = new SmartList<>();
            PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
            if (method != null) {
              ContainerUtil.addIfNotNull(variables, getEventExpressionRootObject(method));
              PsiParameter[] parameters = method.getParameterList().getParameters();
              if (parameters.length == 1) {
                variables.add(parameters[0]);
              }
            }

            return variables;
          }
        }
      }
    }
  }

  @Nullable
  private static PsiVariable getEventExpressionRootObject(PsiMethod method) {
    PsiClass rootObjectClass = InfraUtils.findLibraryClass(ModuleUtilCore.findModuleForPsiElement(method), "cn.taketoday.context.event.EventExpressionRootObject");
    if (rootObjectClass == null) {
      return null;
    }
    else {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      PsiClassType type = PsiTypesUtil.getClassType(parameters.length != 1 ? rootObjectClass : getRootObjectDelegateClass(rootObjectClass, parameters[0]));
      return new ElContextVariable("root", type, rootObjectClass);
    }
  }

  private static PsiClass getRootObjectDelegateClass(PsiClass rootObjectClass, PsiParameter parameter) {
    return new LightClass(rootObjectClass) {

      @Override
      public PsiMethod[] getMethods() {
        Set<PsiMethod> methods = new HashSet<>();
        PsiMethod[] psiMethods = super.getMethods();

        for (PsiMethod method : psiMethods) {
          if ("getEvent".equals(method.getName())) {
            methods.add(new LightMethod(this.getManager(), method, rootObjectClass) {
              @Override
              public PsiType getReturnType() {
                return parameter.getType();
              }
            });
          }
          else {
            methods.add(method);
          }
        }

        return methods.toArray(PsiMethod.EMPTY_ARRAY);
      }
    };
  }
}
