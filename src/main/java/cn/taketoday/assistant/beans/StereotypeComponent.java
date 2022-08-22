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

package cn.taketoday.assistant.beans;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;

@Presentation(typeName = StereotypeComponent.MAPPER_DEFINITION)
public class StereotypeComponent extends SpringStereotypeElement {

  public static final String MAPPER_DEFINITION = "@Component";
  public static final JamClassMeta<StereotypeComponent> META = new JamClassMeta<>(null, StereotypeComponent.class,
          JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("TodayComponent"));
  public static final String ANNOTATION = "cn.taketoday.stereotype.Component";
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(ANNOTATION);

  static {
    addPomTargetProducer(META);
    META.addAnnotation(ANNO_META);
  }

  public StereotypeComponent(PsiClass psiClass) {
    super(ANNOTATION, PsiElementRef.real(psiClass));
  }
}
