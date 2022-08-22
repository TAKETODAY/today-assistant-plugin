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

package cn.taketoday.assistant.code.event.jam;

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemKey;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public class JamEventListenerElement implements EventListenerElement {

  public static final SemKey<JamEventListenerElement> SEM_KEY = EVENT_LISTENER_ROOT_JAM_KEY.subKey("EventListener");
  public static final JamMethodMeta<JamEventListenerElement> METHOD_META = new JamMethodMeta<>(null, JamEventListenerElement.class, SEM_KEY);
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.EVENT_LISTENER);
  private static final JamStringAttributeMeta.Single<String> CONDITION_ATTR_META = JamAttributeMeta.singleString(EventListenerElement.CONDITION_ATTR_NAME);
  protected static final JamClassAttributeMeta.Collection EVENT_ATTR_META = new JamClassAttributeMeta.Collection("event");
  protected static final JamClassAttributeMeta.Collection VALUE_ATTR_META = new JamClassAttributeMeta.Collection("value");

  private final PsiAnchor myPsiMethodAnchor;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  static {
    METHOD_META.addAnnotation(ANNO_META);
    ANNO_META.addAttribute(CONDITION_ATTR_META);
    ANNO_META.addAttribute(VALUE_ATTR_META);
    ANNO_META.addAttribute(EVENT_ATTR_META);
  }

  public JamEventListenerElement(PsiMethod psiMethod) {
    this.myPsiMethodAnchor = PsiAnchor.create(psiMethod);
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiMethod);
  }

  @Override
  public boolean isValid() {
    return this.myPsiMethodAnchor.retrieve() != null;
  }

  @Override
  @Nullable
  public PsiMethod getPsiElement() {
    return (PsiMethod) this.myPsiMethodAnchor.retrieve();
  }

  @Override
  public List<PsiClass> getEventListenerClasses() {
    List<PsiClass> psiClasses = new ArrayList<>();
    for (JamClassAttributeElement jamClassAttributeElement : EVENT_ATTR_META.getJam(this.myPsiAnnotation)) {
      ContainerUtil.addIfNotNull(psiClasses, jamClassAttributeElement.getValue());
    }
    for (JamClassAttributeElement jamClassAttributeElement2 : VALUE_ATTR_META.getJam(this.myPsiAnnotation)) {
      ContainerUtil.addIfNotNull(psiClasses, jamClassAttributeElement2.getValue());
    }
    return psiClasses;
  }

  public JamStringAttributeElement<String> getConditionAttributeElement() {
    return ANNO_META.getAttribute(getPsiElement(), CONDITION_ATTR_META);
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return ANNO_META.getAnnotation(getPsiElement());
  }
}
