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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 22:16
 */
public class ComponentScans extends JamBaseElement<PsiClass> {
  public static final JamAnnotationAttributeMeta.Collection<ComponentScan> SCANS_ATTR =
          JamAttributeMeta.annoCollection("value", ComponentScan.ANNOTATION_META, ComponentScan.class);

  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(AnnotationConstant.COMPONENT_SCANS).addAttribute(
          SCANS_ATTR);

  public static final JamClassMeta<ComponentScans> META = new JamClassMeta<>(ComponentScans.class).addAnnotation(ANNOTATION_META);

  public ComponentScans(PsiElementRef<?> ref) {
    super(ref);
  }

  public List<ComponentScan> getComponentScans() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNOTATION_META, SCANS_ATTR);
  }

}
