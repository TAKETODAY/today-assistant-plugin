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

package cn.taketoday.assistant.model.config.autoconfigure.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.semantic.SemRegistrar;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;

public class InfraApplication extends JamBaseElement<PsiClass> {

  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(
          InfraConfigConstant.INFRA_APPLICATION, EnableAutoConfiguration.EXCLUDE_ARCHETYPE);

  public static final JamClassMeta<InfraApplication> META = new JamClassMeta<>(InfraApplication.class)
          .addAnnotation(ANNOTATION_META);

  public InfraApplication(PsiElementRef<?> ref) {
    super(ref);
  }

  public static void register(SemRegistrar registrar) {
    META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(InfraConfigConstant.INFRA_APPLICATION));
  }

}
