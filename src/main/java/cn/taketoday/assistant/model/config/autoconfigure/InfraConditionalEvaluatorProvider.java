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

import com.intellij.jam.JamService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.ConditionalEvaluationContext;
import cn.taketoday.assistant.model.ConditionalEvaluator;
import cn.taketoday.assistant.model.ConditionalEvaluatorProvider;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContext;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionalOnEvaluationContextBase;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnJamElement;
import cn.taketoday.lang.Nullable;

public class InfraConditionalEvaluatorProvider implements ConditionalEvaluatorProvider {
  @Nullable
  public ConditionalEvaluator getConditionalEvaluator(BeanPointer<?> pointer) {
    PsiElement psiElement = pointer.getPsiElement();
    if (psiElement == null) {
      return null;
    }
    if (psiElement instanceof PsiMethod method) {
      return getMethodConditionalEvaluator(method);
    }
    if (psiElement instanceof PsiClass) {
      return getClassConditionalEvaluator((PsiClass) psiElement);
    }
    PsiElement navigationElement = psiElement.getNavigationElement();
    if (navigationElement instanceof PsiClass psiClass) {
      return getClassConditionalEvaluator(psiClass);
    }
    return null;
  }

  private static ConditionalEvaluator getMethodConditionalEvaluator(PsiMethod psiMethod) {
    if (!hasConditionals(psiMethod)) {
      PsiClass psiClass = psiMethod.getContainingClass();
      if (psiClass != null) {
        return getClassConditionalEvaluator(psiClass);
      }
      return null;
    }
    return new ConditionalEvaluator() {
      public boolean isActive(ConditionalEvaluationContext context) {
        Module module;
        PsiClass psiClass2;
        CommonInfraModel model = context.getModel();
        if (model == null || (module = model.getModule()) == null || (psiClass2 = psiMethod.getContainingClass()) == null) {
          return false;
        }
        NotNullLazyValue<List<VirtualFile>> configFilesCache = AbstractAutoConfigDependentModelsProvider.createConfigFilesCache(module, model.getActiveProfiles());
        ConditionalOnEvaluationContextBase sharedContext = new ConditionalOnEvaluationContextBase(psiClass2, module, model.getActiveProfiles(), configFilesCache, null);
        AutoConfigMethodConditionEvaluator evaluator = new AutoConfigMethodConditionEvaluator(psiMethod,
                false, sharedContext);
        sharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, model);
        if (!evaluator.isActive()) {
          return false;
        }
        AutoConfigClassConditionEvaluator classEvaluator = new AutoConfigClassConditionEvaluator(
                psiClass2,
                false, sharedContext);
        return classEvaluator.isActive();
      }
    };
  }

  private static ConditionalEvaluator getClassConditionalEvaluator(PsiClass psiClass) {
    if (!hasConditionals(psiClass)) {
      return null;
    }
    return new ConditionalEvaluator() {
      public boolean isActive(ConditionalEvaluationContext context) {
        Module module;
        CommonInfraModel model = context.getModel();
        if (model == null || (module = model.getModule()) == null) {
          return false;
        }
        NotNullLazyValue<List<VirtualFile>> configFilesCache = AbstractAutoConfigDependentModelsProvider.createConfigFilesCache(module, model.getActiveProfiles());
        ConditionalOnEvaluationContextBase sharedContext = new ConditionalOnEvaluationContextBase(psiClass, module, model.getActiveProfiles(), configFilesCache, null);
        AutoConfigClassConditionEvaluator evaluator = new AutoConfigClassConditionEvaluator(psiClass,
                false,
                sharedContext);
        sharedContext.putUserData(ConditionalOnEvaluationContext.MODEL_KEY, model);
        return evaluator.isActive();
      }
    };
  }

  private static boolean hasConditionals(PsiElement psiElement) {
    return JamService.getJamService(psiElement.getProject()).getJamElement(ConditionalOnJamElement.CONDITIONAL_JAM_ELEMENT_KEY, psiElement) != null;
  }
}
