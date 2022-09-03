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

package cn.taketoday.assistant.web.mvc.code;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;

import java.util.List;

import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public class WebMVCCantBeStaticExtension implements Condition<PsiElement> {
  private static final List<String> NON_STATIC_METHOD_ANNOTATIONS = List.of(
          InfraMvcConstant.REQUEST_MAPPING,
          InfraMvcConstant.MODEL_ATTRIBUTE,
          InfraMvcConstant.EXCEPTION_HANDLER
  );

  public boolean value(PsiElement member) {
    if (member instanceof PsiMethod psiMethod) {
      if (!psiMethod.hasModifierProperty("public")) {
        return false;
      }
      PsiModifierList modifierList = psiMethod.getModifierList();
      if (modifierList.getAnnotations().length == 0) {
        return false;
      }
      return AnnotationUtil.isAnnotated(psiMethod, NON_STATIC_METHOD_ANNOTATIONS, 0) || InfraControllerUtils.isJamRequestHandler(psiMethod);
    }
    return false;
  }
}
