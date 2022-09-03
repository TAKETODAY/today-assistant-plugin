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

package cn.taketoday.assistant.model.jam.testContexts;

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

public class InfraContextHierarchy extends JamBaseElement<PsiClass> {

  private static final JamAnnotationAttributeMeta.Collection<InfraContextConfiguration>
          CONFIGURATIONS_ATTRIBUTE = JamAttributeMeta.annoCollection("value", InfraContextConfiguration.ANNO_META, InfraContextConfiguration.class);
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(AnnotationConstant.CONTEXT_HIERARCHY).addAttribute(CONFIGURATIONS_ATTRIBUTE);
  public static final JamClassMeta<InfraContextHierarchy> META = new JamClassMeta<>(InfraContextHierarchy.class).addAnnotation(ANNOTATION_META);

  public InfraContextHierarchy(PsiElementRef<?> ref) {
    super(ref);
  }

  public List<InfraContextConfiguration> getContextConfigurations() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), ANNOTATION_META, CONFIGURATIONS_ATTRIBUTE);
  }
}
