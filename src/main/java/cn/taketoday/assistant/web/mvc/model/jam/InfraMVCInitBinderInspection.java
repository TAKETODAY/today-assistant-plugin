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

package cn.taketoday.assistant.web.mvc.model.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

import cn.taketoday.assistant.model.highlighting.jam.AbstractInfraJavaInspection;
import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

import static cn.taketoday.assistant.InfraAppBundle.message;

public class InfraMVCInitBinderInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkMethod(PsiMethod method, InspectionManager manager, boolean isOnTheFly) {
    PsiClass containingClass;
    if (!isRelevantMethod(method) || (containingClass = method.getContainingClass()) == null || !InfraControllerUtils.isController(containingClass)) {
      return null;
    }
    PsiAnnotation annotation = method.getModifierList().findAnnotation(InfraMvcConstant.INIT_BINDER);
    return new ProblemDescriptor[] {
            manager.createProblemDescriptor(annotation,
                    message("InfraMVCInitBinderInspection.method.annotated.with.initbinder.must.return.void"), (LocalQuickFix) null,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly)
    };
  }

  private static boolean isRelevantMethod(PsiMethod method) {
    return method.hasModifierProperty("public") && !method.hasModifierProperty("static") && !PsiType.VOID.equals(method.getReturnType()) && AnnotationUtil.isAnnotated(method,
            InfraMvcConstant.INIT_BINDER, 0);
  }
}
