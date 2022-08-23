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

import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.spring.model.jam.stereotype.SpringJamComponentScanArchetype;

import cn.taketoday.assistant.AnnotationConstant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 16:25
 */
public class ComponentScan extends SpringJamComponentScanArchetype {

  private static final SemKey<ComponentScan> JAM_KEY = COMPONENT_SCAN_JAM_KEY.subKey("ComponentScan");
  public static final SemKey<ComponentScan> REPEATABLE_ANNO_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("ComponentScan");

  public static final JamAnnotationMeta ANNOTATION_META =
          new JamAnnotationMeta(AnnotationConstant.COMPONENT_SCAN, ARCHETYPE, META_KEY);

  public static final JamClassMeta<ComponentScan> META =
          new JamClassMeta<>(null, ComponentScan.class, JAM_KEY).addAnnotation(ANNOTATION_META);

  public ComponentScan(PsiClass psiElement) {
    super(psiElement);
  }

  public ComponentScan(PsiAnnotation annotation) {
    super(annotation);
  }

  @Override

  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }

}
