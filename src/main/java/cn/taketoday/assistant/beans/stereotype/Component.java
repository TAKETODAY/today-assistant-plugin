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

package cn.taketoday.assistant.beans.stereotype;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.semantic.SemRegistrar;
import com.intellij.spring.constants.SpringCorePresentationConstants;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;

import cn.taketoday.assistant.AnnotationConstant;

@Presentation(typeName = SpringCorePresentationConstants.COMPONENT)
public class Component extends SpringStereotypeElement {

  public static final JamClassMeta<Component> META = new JamClassMeta<>(null, Component.class,
          JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("ComponentBean"));

  static {
    addPomTargetProducer(META);
  }

  public Component(PsiClass psiClass) {
    super(AnnotationConstant.COMPONENT, PsiElementRef.real(psiClass));
  }

  public static void register(SemRegistrar registrar, PsiClassPattern prototype) {
    META.register(registrar, prototype.withAnnotation(AnnotationConstant.COMPONENT));
  }

}
