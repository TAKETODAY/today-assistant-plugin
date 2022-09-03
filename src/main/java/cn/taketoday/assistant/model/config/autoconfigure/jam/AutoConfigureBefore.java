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

import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiElementRef;

import java.util.List;

import cn.taketoday.assistant.model.config.autoconfigure.InfraConfigConstant;

public class AutoConfigureBefore extends AutoConfigureByClassesOrderBase {
  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraConfigConstant.AUTO_CONFIGURE_BEFORE, ARCHETYPE);
  public static final JamClassMeta<AutoConfigureBefore> META = new JamClassMeta<>(AutoConfigureBefore.class).addAnnotation(ANNOTATION_META);

  @Override
  public List getClasses() {
    return super.getClasses();
  }

  public AutoConfigureBefore(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  protected JamAnnotationMeta getAnnotationMeta() {
    return ANNOTATION_META;
  }
}
