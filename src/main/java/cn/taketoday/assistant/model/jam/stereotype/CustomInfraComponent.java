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
package cn.taketoday.assistant.model.jam.stereotype;

import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;

import cn.taketoday.assistant.beans.stereotype.InfraMetaStereotypeComponent;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.lang.Nullable;

public class CustomInfraComponent extends InfraMetaStereotypeComponent {

  public static final SemKey<JamMemberMeta<PsiClass, CustomInfraComponent>> META_KEY =
          JamService.ALIASING_MEMBER_META_KEY.subKey("CustomComponentMeta");

  public static final SemKey<CustomInfraComponent> JAM_KEY =
          JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("CustomComponentMeta");

  public CustomInfraComponent(PsiClass psiClassAnchor) {
    super(null, psiClassAnchor);
  }

  public CustomInfraComponent(@Nullable String anno, PsiClass psiClassAnchor) {
    super(anno, psiClassAnchor);
  }
}