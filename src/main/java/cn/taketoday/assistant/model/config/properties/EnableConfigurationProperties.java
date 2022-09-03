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

package cn.taketoday.assistant.model.config.properties;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.app.InfraClassesConstants;

public class EnableConfigurationProperties extends JamBaseElement<PsiClass> {

  private static final JamClassAttributeMeta.Collection VALUE_META = JamAttributeMeta.classCollection("value");
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraClassesConstants.ENABLE_CONFIGURATION_PROPERTIES)
          .addAttribute(VALUE_META);
  public static final JamClassMeta<EnableConfigurationProperties> CLASS_META = new JamClassMeta<>(EnableConfigurationProperties.class)
          .addAnnotation(ANNOTATION_META);

  public EnableConfigurationProperties(PsiElementRef<?> ref) {
    super(ref);
  }

  public PsiAnnotation getAnnotation() {
    return AnnotationUtil.findAnnotation(getPsiElement(), true, ANNOTATION_META.getAnnoName());
  }

  public List<PsiClass> getValue() {
    List<JamClassAttributeElement> valueAttributeElements = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META);
    SmartList<PsiClass> smartList = new SmartList<>();
    for (JamClassAttributeElement element : valueAttributeElements) {
      ContainerUtil.addIfNotNull(smartList, element.getValue());
    }
    return smartList;
  }
}
