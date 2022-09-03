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

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.url.AnnotationValueUrlPathInlayProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;

import cn.taketoday.lang.Nullable;

public abstract class HttpExchange<T extends PsiMember> extends InfraExchangeMapping<T> implements AnnotationValueUrlPathInlayProvider {
  private static final JamStringAttributeMeta.Single<String> METHOD_META = JamAttributeMeta.singleString("method");
  private static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype(STANDARD_METHOD_ARCHETYPE).addAttribute(METHOD_META);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraExchangeConstant.SPRING_HTTP_EXCHANGE, ARCHETYPE);
  public static final SemKey<HttpExchange<?>> HTTP_EXCHANGE_JAM_KEY = MAPPING_JAM_KEY.subKey("SpringHttpExchange");
  public static final SemKey<ClassMapping> CLASS_JAM_KEY = HTTP_EXCHANGE_JAM_KEY.subKey("ClassHttpExchange");
  public static final SemKey<MethodMapping> METHOD_JAM_KEY = HTTP_EXCHANGE_JAM_KEY.subKey("MethodHttpExchange");

  public HttpExchange(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  @Nullable
  public String getHttpMethod() {
    return (String) ((JamStringAttributeElement) ANNOTATION_META.getAttribute(getPsiElementRef(), METHOD_META)).getValue();
  }

  @Override
  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }

  public static class ClassMapping extends HttpExchange<PsiClass> {
    public static final JamClassMeta<ClassMapping> META = new JamClassMeta<>(null, ClassMapping.class, CLASS_JAM_KEY).addAnnotation(ANNOTATION_META);

    public ClassMapping(PsiElementRef<?> ref) {
      super(ref);
    }
  }

  public static class MethodMapping extends HttpExchange<PsiMethod> {
    public static final JamMethodMeta<MethodMapping> META = new JamMethodMeta<>(null, MethodMapping.class, METHOD_JAM_KEY).addAnnotation(ANNOTATION_META);

    public MethodMapping(PsiElementRef<?> ref) {
      super(ref);
    }
  }
}
