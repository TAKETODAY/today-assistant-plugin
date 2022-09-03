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
package cn.taketoday.assistant.model.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamNumberAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamFieldMeta;
import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamNumberAttributeMeta;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

/**
 * {@value AnnotationConstant#ORDER}
 */
public class InfraOrder extends JamBaseElement<PsiModifierListOwner> {

  /**
   * cn.taketoday.core.Ordered
   */
  public static final Integer LOWEST_PRECEDENCE = Integer.MAX_VALUE;

  /**
   * cn.taketoday.core.Ordered
   */
  public static final Integer HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

  private static final JamNumberAttributeMeta.Single<Integer> VALUE_ATTR_META =
          JamAttributeMeta.singleInteger("value");

  private static final JamAnnotationMeta ANNO_META =
          new JamAnnotationMeta(AnnotationConstant.ORDER).
                  addAttribute(VALUE_ATTR_META);

  private static final JamMemberArchetype<PsiModifierListOwner, InfraOrder> ARCHETYPE =
          new JamMemberArchetype<PsiModifierListOwner, InfraOrder>()
                  .addAnnotation(ANNO_META);

  public static final JamClassMeta<InfraOrder> CLASS_META =
          new JamClassMeta<>(ARCHETYPE, InfraOrder.class);
  public static final JamFieldMeta<InfraOrder> FIELD_META =
          new JamFieldMeta<>(ARCHETYPE, InfraOrder.class);
  public static final JamMethodMeta<InfraOrder> METHOD_META =
          new JamMethodMeta<>(ARCHETYPE, InfraOrder.class);

  public InfraOrder(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public Integer getValue() {
    JamNumberAttributeElement<Integer> jam = VALUE_ATTR_META.getJam(ANNO_META.getAnnotationRef(getPsiElement()));
    if (jam.getPsiElement() != null) {
      return jam.getValue();
    }
    return Integer.MAX_VALUE;
  }
}
