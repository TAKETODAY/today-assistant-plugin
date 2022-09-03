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

package cn.taketoday.assistant.model.extensions.myBatis;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;

import cn.taketoday.assistant.beans.stereotype.InfraStereotypeElement;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;

@Presentation(typeName = InfraMyBatisMapper.MAPPER_DEFINITION)
public class InfraMyBatisMapper extends InfraStereotypeElement {
  public static final String MAPPER_DEFINITION = "@Mapper";
  public static final JamClassMeta<InfraMyBatisMapper> META = new JamClassMeta<>(null, InfraMyBatisMapper.class,
          JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("SpringMyBatisMapper"));
  public static final String MAPPER_ANNOTATION = "org.apache.ibatis.annotations.Mapper";
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(MAPPER_ANNOTATION);

  static {
    addPomTargetProducer(META);
    META.addAnnotation(ANNO_META);
  }

  public InfraMyBatisMapper(PsiClass psiClass) {
    super(MAPPER_ANNOTATION, PsiElementRef.real(psiClass));
  }
}
