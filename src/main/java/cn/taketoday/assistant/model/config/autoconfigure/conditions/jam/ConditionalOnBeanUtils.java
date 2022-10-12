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

package cn.taketoday.assistant.model.config.autoconfigure.conditions.jam;

import com.intellij.jam.JamService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;

import java.util.List;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.CommonInfraBean;
import cn.taketoday.assistant.model.ModelSearchParameters;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.ConditionOutcome;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.assistant.model.utils.InfraModelSearchers;
import cn.taketoday.lang.Nullable;

final class ConditionalOnBeanUtils {

  static PsiClassType getContainerType(PsiClass container, PsiType psiType) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(container.getProject());
    PsiSubstitutor substitutor = PsiSubstitutor.EMPTY.put(container.getTypeParameters()[0], psiType);
    return facade.getElementFactory().createType(container, substitutor, PsiUtil.getLanguageLevel(container));
  }

  static List<BeanPointer<?>> findBeansByType(CommonInfraModel infraModel, PsiType psiType) {
    ModelSearchParameters.BeanClass searchParameters = ModelSearchParameters.byType(psiType).withInheritors().effectiveBeanTypes();
    return InfraModelSearchers.findBeans(infraModel, searchParameters);
  }

  @Nullable
  static PsiType getBeanType(PsiMethod psiMethod) {
    ContextJavaBean bean = JamService.getJamService(psiMethod.getProject()).getJamElement(ContextJavaBean.BEAN_JAM_KEY, psiMethod);
    if (bean == null) {
      return null;
    }
    PsiType psiType = psiMethod.getReturnType();
    if (!(psiType instanceof PsiClassType)) {
      return null;
    }
    return psiType;
  }

  @Nullable
  static CommonInfraBean getSpringBean(PsiElement psiElement) {
    return JamService.getJamService(psiElement.getProject()).getJamElement(JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY, psiElement);
  }

  static ConditionOutcome getMissingModelOutcome() {
    return ConditionOutcome.noMatch("Evaluation context doesn't contain model to search in");
  }
}
