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

package cn.taketoday.assistant.model.jam.transaction;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.converters.InfraBeanReferenceJamConverter;
import cn.taketoday.lang.Nullable;

public class TransactionalComponent extends JamBaseElement<PsiClass> {
  public static final JamClassMeta<TransactionalComponent> META = new JamClassMeta<>(TransactionalComponent.class);
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.TRANSACTIONAL);
  private static final JamStringAttributeMeta.Single<BeanPointer<?>> VALUE_ATTR_META = JamAttributeMeta.singleString("value",
          new InfraBeanReferenceJamConverter(AnnotationConstant.PLATFORM_TRANSACTION_MANAGER));

  static {
    META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
  }

  public TransactionalComponent(PsiElementRef<?> ref) {
    super(ref);
  }

  public JamStringAttributeElement<BeanPointer<?>> getValueAttributeElement() {
    return ANNO_META.getAttribute(getPsiElement(), VALUE_ATTR_META);
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNO_META.getAnnotation(getPsiElement());
  }
}
