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
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiParameter;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.lang.Nullable;

public class WebMVCCookieValue extends JamBaseElement<PsiParameter> {
  private static final JamStringAttributeMeta.Single<String> VALUE_META = JamAttributeMeta.singleString(RequestMapping.VALUE_ATTRIBUTE);
  private static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("name");
  private static final JamBooleanAttributeMeta REQUIRED_META = JamAttributeMeta.singleBoolean("required", true);
  private static final JamStringAttributeMeta.Single<String> DEFAULT_VALUE_META = JamAttributeMeta.singleString("defaultValue");
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraMvcConstant.COOKIE_VALUE).addAttribute(VALUE_META).addAttribute(NAME_META).addAttribute(REQUIRED_META)
          .addAttribute(DEFAULT_VALUE_META);
  public static final JamMemberMeta<PsiParameter, WebMVCCookieValue> META = new JamParameterMeta<>(WebMVCCookieValue.class)
          .addAnnotation(ANNOTATION_META);

  public WebMVCCookieValue(PsiElementRef<?> ref) {
    super(ref);
  }

  public boolean isRequired() {
    if (getDefaultValue() != null) {
      return false;
    }
    return ANNOTATION_META.getAttribute(getPsiElement(), REQUIRED_META).getValue().booleanValue();
  }

  @Nullable
  public String getName() {
    String value = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META).getStringValue();
    if (value != null) {
      return value;
    }
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_META).getStringValue();
  }

  @Nullable
  public String getDefaultValue() {
    return ANNOTATION_META.getAttribute(getPsiElement(), DEFAULT_VALUE_META).getStringValue();
  }
}
