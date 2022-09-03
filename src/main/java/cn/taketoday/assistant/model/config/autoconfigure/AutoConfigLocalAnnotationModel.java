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

package cn.taketoday.assistant.model.config.autoconfigure;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.SmartList;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import cn.taketoday.assistant.context.model.LocalAnnotationModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ConditionalEvaluationContext;
import cn.taketoday.assistant.model.DelegateConditionalBeanPointer;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.jam.JamBeanPointer;
import cn.taketoday.lang.Nullable;

class AutoConfigLocalAnnotationModel extends LocalAnnotationModel {
  private final boolean myNonStrictEvaluation;
  private final ConditionalOnEvaluationContext mySharedContext;

  AutoConfigLocalAnnotationModel(PsiClass aClass, Module module, Set<String> activeProfiles, boolean nonStrictEvaluation,
          ConditionalOnEvaluationContext sharedContext) {
    super(aClass, module, activeProfiles);
    this.myNonStrictEvaluation = nonStrictEvaluation;
    this.mySharedContext = sharedContext;
  }

  @Nullable
  protected LocalAnnotationModel getLocalAnnotationModel(PsiClass configClass) {
    boolean passesConditionClassMatch = AutoConfigClassSorter.passesConditionalClassMatch(configClass);
    if (!passesConditionClassMatch) {
      return null;
    }
    AutoConfigClassConditionEvaluator evaluator = new AutoConfigClassConditionEvaluator(configClass, this.myNonStrictEvaluation, this.mySharedContext);
    if (!evaluator.isActive()) {
      return null;
    }
    return new AutoConfigLocalAnnotationModel(configClass, getModule(), getActiveProfiles(), this.myNonStrictEvaluation, this.mySharedContext);
  }

  public Collection<BeanPointer<?>> getLocalBeans() {
    PsiElement firstElement;
    Collection<BeanPointer<?>> beanPointers = super.getLocalBeans();
    if (beanPointers.isEmpty()) {
      return beanPointers;
    }
    SmartList<BeanPointer<?>> smartList = new SmartList<>();
    Iterator<BeanPointer<?>> iterator = beanPointers.iterator();
    boolean classBeanPointerProcessed = false;
    BeanPointer<?> beanPointer = iterator.next();
    if ((beanPointer instanceof JamBeanPointer jamBeanPointer)
            && (firstElement = beanPointer.getPsiElement()) != null
            && (getConfig().equals(firstElement.getNavigationElement()) || getConfig().equals(firstElement))) {

      Condition<ConditionalEvaluationContext> condition = context -> {
        AutoConfigClassConditionEvaluator classEvaluator = new AutoConfigClassConditionEvaluator(getConfig(), false, this.mySharedContext);
        this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, context.getModel());
        try {
          boolean isActive = classEvaluator.isActive();
          this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, null);
          return isActive;
        }
        catch (Throwable th) {
          this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, null);
          throw th;
        }
      };
      smartList.add(DelegateConditionalBeanPointer.createPointer(jamBeanPointer, condition));
      classBeanPointerProcessed = true;
    }
    if (!classBeanPointerProcessed) {
      iterator = beanPointers.iterator();
    }
    while (iterator.hasNext()) {
      beanPointer = iterator.next();
      if (beanPointer instanceof JamBeanPointer) {
        PsiElement element = beanPointer.getPsiElement();
        if (element instanceof PsiMethod psiMethod) {
          Condition<ConditionalEvaluationContext> condition2 = context2 -> {
            boolean passesConditionClassMatch = AutoConfigClassSorter.passesConditionalClassMatch(psiMethod);
            if (!passesConditionClassMatch) {
              return false;
            }
            AutoConfigMethodConditionEvaluator evaluator = new AutoConfigMethodConditionEvaluator(psiMethod, false, this.mySharedContext);
            this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, context2.getModel());
            try {
              if (!evaluator.isActive()) {
                return false;
              }
              AutoConfigClassConditionEvaluator classEvaluator = new AutoConfigClassConditionEvaluator(getConfig(), false, this.mySharedContext);
              boolean isActive = classEvaluator.isActive();
              this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, null);
              return isActive;
            }
            finally {
              this.mySharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, null);
            }
          };
          beanPointer = DelegateConditionalBeanPointer.createPointer((JamBeanPointer) beanPointer, condition2);
        }
      }
      smartList.add(beanPointer);
    }
    return smartList;
  }
}
