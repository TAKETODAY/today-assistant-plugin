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

import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.semantic.SemKey;

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public class BeanEventListenerElement implements EventListenerElement {
  public static final SemKey<BeanEventListenerElement> SEM_KEY = EVENT_LISTENER_ROOT_JAM_KEY.subKey("BeanEventListener");
  public static final JamMethodMeta<BeanEventListenerElement> METHOD_META = new JamMethodMeta<>(null, BeanEventListenerElement.class, SEM_KEY);
  private final PsiAnchor myPsiMethodAnchor;

  public BeanEventListenerElement(PsiMethod myPsiMethod) {
    this.myPsiMethodAnchor = PsiAnchor.create(myPsiMethod);
  }

  @Override
  public List<PsiClass> getEventListenerClasses() {
    PsiMethod element = (PsiMethod) this.myPsiMethodAnchor.retrieve();
    if (element == null || element.getParameterList().getParametersCount() != 1) {
      return Collections.emptyList();
    }
    PsiParameter parameter = element.getParameterList().getParameters()[0];
    if (!(parameter.getType() instanceof PsiClassType)) {
      return Collections.emptyList();
    }
    PsiClass parameterClass = ((PsiClassType) parameter.getType()).resolve();
    return Collections.singletonList(parameterClass);
  }

  @Override
  @Nullable
  public PsiMethod getPsiElement() {
    return (PsiMethod) this.myPsiMethodAnchor.retrieve();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return null;
  }

  @Override
  public boolean isValid() {
    return this.myPsiMethodAnchor.retrieve() != null;
  }
}
