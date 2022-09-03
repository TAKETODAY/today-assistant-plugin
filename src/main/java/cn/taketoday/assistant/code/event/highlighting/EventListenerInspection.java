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

package cn.taketoday.assistant.code.event.highlighting;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.IntentionWrapper;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.actions.JvmElementActionFactories;
import com.intellij.lang.jvm.actions.MemberRequestsKt;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;

import org.jetbrains.uast.UAnnotated;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UAnnotationKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastVisibility;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraBundle;
import cn.taketoday.assistant.code.AbstractInfraLocalInspection;
import cn.taketoday.lang.Nullable;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public final class EventListenerInspection extends AbstractInfraLocalInspection {

  public EventListenerInspection() {
    super(UMethod.class);
  }

  @Override
  @Nullable
  public ProblemDescriptor[] checkMethod(UMethod method, InspectionManager manager, boolean isOnTheFly) {
    Intrinsics.checkNotNullParameter(method, "method");
    Intrinsics.checkNotNullParameter(manager, "manager");

    PsiElement srcPsi = method.getSourcePsi();
    if (srcPsi == null) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, srcPsi.getContainingFile(), isOnTheFly);
    if (method.getUastParameters().size() > 1) {
      PsiElement psiElement = getListenerElement(method);
      if (psiElement != null) {
        String message = InfraBundle.message("event.listener.method.parameters.count");
        LocalQuickFix[] wrapToQuickFixes = IntentionWrapper.wrapToQuickFixes(
                new IntentionAction[0], psiElement.getContainingFile());
        holder.registerProblem(psiElement, message,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, wrapToQuickFixes.clone());
      }
    }
    if (method.getVisibility() != UastVisibility.PUBLIC) {
      String message = InfraBundle.message("event.listener.method.visibility.public");

      PsiElement namePsiElement = getListenerElement(method);
      if (namePsiElement != null) {
        Collection<IntentionAction> intentionActions = JvmElementActionFactories.createModifierActions(method, MemberRequestsKt.modifierRequest(JvmModifier.PUBLIC, true));
        IntentionAction[] array2 = intentionActions.toArray(new IntentionAction[0]);
        LocalQuickFix[] wrapToQuickFixes2 = IntentionWrapper.wrapToQuickFixes(array2, namePsiElement.getContainingFile());
        holder.registerProblem(namePsiElement, message,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, Arrays.copyOf(wrapToQuickFixes2, wrapToQuickFixes2.length));
      }
    }

    if (method.isStatic()) {
      PsiElement element = getListenerElement(method);
      if (element != null) {
        ProblemHighlightType problemHighlightType3 = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        List<IntentionAction> actions = JvmElementActionFactories.createModifierActions(method, MemberRequestsKt.modifierRequest(JvmModifier.STATIC, false));
        Object[] array3 = actions.toArray(new IntentionAction[0]);
        LocalQuickFix[] wrapToQuickFixes3 = IntentionWrapper.wrapToQuickFixes((IntentionAction[]) array3, element.getContainingFile());
        String message = InfraBundle.message("event.listener.method.visibility.nonstatic");
        holder.registerProblem(element, message, problemHighlightType3, Arrays.copyOf(wrapToQuickFixes3, wrapToQuickFixes3.length));
      }
    }
    return holder.getResultsArray();
  }

  @Nullable
  private PsiElement getListenerElement(UMethod method) {
    UAnnotation annotation = getListenerAnnotation(method);
    return UAnnotationKt.getNamePsiElement(annotation);
  }

  @Nullable
  private UAnnotation getListenerAnnotation(UAnnotated annotated) {
    for (UAnnotation uAnnotation : annotated.getUAnnotations()) {
      if (isListenerAnnotation(uAnnotation)) {
        return uAnnotation;
      }
    }
    return null;
  }

  private void reportIfEventListener(UMethod method, ProblemsHolder holder,
          @InspectionMessage String message, Function0<? extends List<? extends IntentionAction>> function0) {
    UAnnotation obj = null;
    for (UAnnotation next : method.getUAnnotations()) {
      if (isListenerAnnotation(next)) {
        obj = next;
        break;
      }
    }

    PsiElement listenerAnnotation = UAnnotationKt.getNamePsiElement(obj);
    if (listenerAnnotation != null) {
      ProblemHighlightType problemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
      List<? extends IntentionAction> invoke = function0.invoke();
      IntentionAction[] array = invoke.toArray(new IntentionAction[0]);
      LocalQuickFix[] wrapToQuickFixes = IntentionWrapper.wrapToQuickFixes(array, listenerAnnotation.getContainingFile());
      holder.registerProblem(listenerAnnotation, message, problemHighlightType, Arrays.copyOf(wrapToQuickFixes, wrapToQuickFixes.length));
    }
  }

  public boolean isListenerAnnotation(UAnnotation it) {
    PsiModifierListOwner resolve = it.resolve();
    if (resolve instanceof PsiClass psiClass) {
      if (!Objects.equals(psiClass.getQualifiedName(), AnnotationConstant.EVENT_LISTENER)) {
        return MetaAnnotationUtil.isMetaAnnotated(resolve, List.of(AnnotationConstant.EVENT_LISTENER));
      }
      return true;
    }
    return false;
  }
}
