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
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.spring.constants.SpringAnnotationsConstants;
import com.intellij.spring.constants.SpringCorePresentationConstants;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.stereotype.SpringMetaStereotypeComponent;
import com.intellij.util.Function;

import java.util.Collection;

@Presentation(typeName = SpringCorePresentationConstants.CONTROLLER)
public class Controller extends SpringMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, Controller>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("InfraControllerMeta");
  public static final SemKey<Controller> JAM_KEY = JamPsiMemberSpringBean.PSI_MEMBER_SPRING_BEAN_JAM_KEY.subKey("InfraController");
  public static final JamClassMeta<Controller> META = new JamClassMeta<>(null, Controller.class, JAM_KEY);

  private static final Function<Module, Collection<String>> ANNOTATIONS = module -> {
    return getAnnotations(module, SpringAnnotationsConstants.CONTROLLER);
  };

  public Controller(PsiClass psiClass) {
    this(SpringAnnotationsConstants.CONTROLLER, psiClass);
  }

  public Controller(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public static Function<Module, Collection<String>> getControllerAnnotations() {
    return ANNOTATIONS;
  }

}
