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

package cn.taketoday.assistant.model.highlighting.jam;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.MakeVoidQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.util.SmartList;

import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class InfraScheduledMethodInspection extends AbstractInfraJavaInspection {

  public ProblemDescriptor[] checkMethod(PsiMethod psiMethod, InspectionManager manager, boolean isOnTheFly) {
    PsiElement nameIdentifier;
    Intrinsics.checkNotNullParameter(psiMethod, "psiMethod");
    Intrinsics.checkNotNullParameter(manager, "manager");
    if (MetaAnnotationUtil.isMetaAnnotated(psiMethod, CollectionsKt.listOf(AnnotationConstant.SCHEDULED))) {
      Collection smartList = new SmartList();
      if (!Intrinsics.areEqual(PsiType.VOID, psiMethod.getReturnType())) {
        smartList.add(new MakeVoidQuickFix(null));
      }
      PsiParameterList parameterList = psiMethod.getParameterList();
      Intrinsics.checkNotNullExpressionValue(parameterList, "psiMethod.parameterList");
      if (parameterList.getParametersCount() > 0) {
        smartList.add(new MethodParametersRemovingFix(psiMethod));
      }
      if ((!smartList.isEmpty()) && (nameIdentifier = psiMethod.getNameIdentifier()) != null) {
        ProblemsHolder $this$apply = new ProblemsHolder(manager, psiMethod.getContainingFile(), isOnTheFly);
        String message = InfraBundle.message("ScheduledMethodInspection.incorrect.signature");
        Object[] array = smartList.toArray(new LocalQuickFix[0]);
        LocalQuickFix[] localQuickFixArr = (LocalQuickFix[]) array;
        $this$apply.registerProblem(nameIdentifier, message, Arrays.copyOf(localQuickFixArr, localQuickFixArr.length));
        return $this$apply.getResultsArray();
      }
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }
}
