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

package cn.taketoday.assistant.web.mvc.client.exchange;

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;

import cn.taketoday.lang.Nullable;

public class PutExchange extends InfraExchangeMapping<PsiMethod> {
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraExchangeConstant.SPRING_PUT_EXCHANGE, STANDARD_METHOD_ARCHETYPE);
  private static final SemKey<PutExchange> PUT_JAM_KEY = MAPPING_JAM_KEY.subKey("PutExchange");
  public static final JamMethodMeta<PutExchange> META = new JamMethodMeta<>(null, PutExchange.class, PUT_JAM_KEY).addAnnotation(ANNOTATION_META);

  public PutExchange(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  @Nullable
  public String getHttpMethod() {
    return "PUT";
  }

  @Override
  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }
}
