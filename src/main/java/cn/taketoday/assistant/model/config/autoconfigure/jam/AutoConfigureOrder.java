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

package cn.taketoday.assistant.model.config.autoconfigure.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamNumberAttributeElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamFieldMeta;
import com.intellij.jam.reflect.JamMemberArchetype;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamNumberAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;
import cn.taketoday.lang.Nullable;

public class AutoConfigureOrder extends JamBaseElement<PsiModifierListOwner> {
  public static final String ORDER_ATTRIBUTE_NAME = "value";
  private static final JamNumberAttributeMeta.Single<Integer> VALUE_ATTR_META = JamAttributeMeta.singleInteger("value");
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(InfraConfigConstant.AUTO_CONFIGURE_ORDER).addAttribute(VALUE_ATTR_META);
  private static final JamMemberArchetype<PsiModifierListOwner, AutoConfigureOrder> ARCHETYPE = new JamMemberArchetype().addAnnotation(ANNO_META);
  public static final JamClassMeta<AutoConfigureOrder> CLASS_META = new JamClassMeta<>(ARCHETYPE, AutoConfigureOrder.class);
  public static final JamFieldMeta<AutoConfigureOrder> FIELD_META = new JamFieldMeta<>(ARCHETYPE, AutoConfigureOrder.class);
  public static final JamMethodMeta<AutoConfigureOrder> METHOD_META = new JamMethodMeta<>(ARCHETYPE, AutoConfigureOrder.class);

  public AutoConfigureOrder(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public Integer getValue() {
    JamNumberAttributeElement<Integer> jam = VALUE_ATTR_META.getJam(ANNO_META.getAnnotationRef(getPsiElement()));
    if (jam.getPsiElement() != null) {
      return jam.getValue();
    }
    PsiAnnotation autoConfigureAnnotation = jam.getParentAnnotationElement().getPsiElement();
    if (autoConfigureAnnotation == null) {
      return null;
    }
    PsiAnnotationMemberValue annotationAttributeValue = autoConfigureAnnotation.findAttributeValue("value");
    return JamCommonUtil.getObjectValue(annotationAttributeValue, Integer.class);
  }
}
