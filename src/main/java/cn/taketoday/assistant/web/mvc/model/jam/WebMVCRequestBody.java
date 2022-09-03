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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamParameterMeta;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiParameter;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;

public class WebMVCRequestBody extends JamBaseElement<PsiParameter> {

  private static final JamBooleanAttributeMeta REQUIRED_META = JamAttributeMeta.singleBoolean("required", true);
  private static final JamAnnotationMeta ANNOTATION_META
          = new JamAnnotationMeta(InfraMvcConstant.REQUEST_BODY).addAttribute(REQUIRED_META);
  public static final JamMemberMeta<PsiParameter, WebMVCRequestBody> META = new JamParameterMeta<>(WebMVCRequestBody.class)
          .addAnnotation(ANNOTATION_META);

  public WebMVCRequestBody(PsiElementRef<?> ref) {
    super(ref);
  }

  public boolean isRequired() {
    return ANNOTATION_META.getAttribute(getPsiElement(), REQUIRED_META).getValue();
  }
}
