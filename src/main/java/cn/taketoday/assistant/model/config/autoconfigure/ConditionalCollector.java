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
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemService;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.Conditional;
import cn.taketoday.assistant.model.config.autoconfigure.conditions.jam.ConditionalOnJamElement;
import cn.taketoday.assistant.model.jam.InfraOrder;
import cn.taketoday.lang.Nullable;

final class ConditionalCollector {
  private static final SemKey<JamMemberMeta> MEMBER_META_KEY = JamService.getMetaKey(ConditionalOnJamElement.CONDITIONAL_JAM_ELEMENT_KEY);
  private static final Integer DEFAULT_ORDER = InfraOrder.LOWEST_PRECEDENCE;
  private final PsiModifierListOwner myOwner;

  private ConditionalCollector(PsiModifierListOwner owner) {
    this.myOwner = owner;
  }

  static ConditionalCollector forClass(PsiClass psiClass) {
    return new ConditionalCollector(psiClass);
  }

  static ConditionalCollector forMethod(PsiMethod psiMethod) {
    return new ConditionalCollector(psiMethod);
  }

  Collection<ConditionalOnJamElement> getConditionals() {
    List<JamMemberMeta> allConditionalMeta = SemService.getSemService(this.myOwner.getProject()).getSemElements(MEMBER_META_KEY, this.myOwner);
    if (allConditionalMeta.isEmpty()) {
      return Collections.emptyList();
    }
    else if (allConditionalMeta.size() == 1) {
      ConditionalOnJamElement jamElement = (ConditionalOnJamElement) allConditionalMeta.get(0).getJamElement(this.myOwner);
      return Collections.singletonList(jamElement);
    }
    else {
      MultiMap<Integer, ConditionalOnJamElement> sorted = new MultiMap<>(new TreeMap<>(Integer::compareTo));
      for (JamMemberMeta meta : allConditionalMeta) {
        ConditionalOnJamElement jamElement = (ConditionalOnJamElement) meta.getJamElement(this.myOwner);
        int order = calcOrder(meta, jamElement);
        sorted.putValue(order, jamElement);
      }
      return sorted.values();
    }
  }

  private int calcOrder(JamMemberMeta<PsiModifierListOwner, ConditionalOnJamElement> meta, ConditionalOnJamElement jamElement) {
    if (jamElement instanceof Conditional) {
      return getOrderValue((Conditional) jamElement);
    }
    Conditional conditional = findConditionalForJam(meta);
    return getOrderValue(conditional);
  }

  @Nullable
  private Conditional findConditionalForJam(JamMemberMeta<?, ?> meta) {
    JamAnnotationMeta annotationMeta = ContainerUtil.getFirstItem(meta.getAnnotations());
    if (annotationMeta == null) {
      throw new IllegalStateException("no annotation registered for JAM: " + meta);
    }
    String annotationFqn = annotationMeta.getAnnoName();
    PsiClass annotationPsiClass = JavaPsiFacade.getInstance(this.myOwner.getProject()).findClass(annotationFqn, this.myOwner.getResolveScope());
    if (annotationPsiClass != null) {
      return Conditional.CLASS_META.getJamElement(annotationPsiClass);
    }
    return null;
  }

  private static int getOrderValue(@Nullable Conditional conditional) {
    PsiClass conditionClass;
    InfraOrder order;
    if (conditional != null && (conditionClass = ContainerUtil.getFirstItem(conditional.getValue())) != null && (order = InfraOrder.CLASS_META.getJamElement(conditionClass)) != null) {
      return ObjectUtils.notNull(order.getValue(), DEFAULT_ORDER);
    }
    return DEFAULT_ORDER;
  }
}
