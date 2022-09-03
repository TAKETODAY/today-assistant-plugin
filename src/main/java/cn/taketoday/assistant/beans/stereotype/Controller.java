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
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.semantic.SemKey;
import com.intellij.util.Function;

import java.util.Collection;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;

@Presentation(typeName = PresentationConstant.CONTROLLER)
public class Controller extends InfraMetaStereotypeComponent {
  public static final SemKey<JamMemberMeta<PsiClass, Controller>> META_KEY = JamService.ALIASING_MEMBER_META_KEY.subKey("InfraControllerMeta");
  public static final SemKey<Controller> JAM_KEY = JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("InfraController");
  public static final JamClassMeta<Controller> META = new JamClassMeta<>(null, Controller.class, JAM_KEY);

  private static final Function<Module, Collection<String>> ANNOTATIONS
          = module -> getAnnotations(module, AnnotationConstant.CONTROLLER);

  public Controller(PsiClass psiClass) {
    this(AnnotationConstant.CONTROLLER, psiClass);
  }

  public Controller(Pair<String, PsiClass> pair) {
    super(pair.getFirst(), pair.getSecond());
  }

  public Controller(String anno, PsiClass psiClass) {
    super(anno, psiClass);
  }

  public static Function<Module, Collection<String>> getControllerAnnotations() {
    return ANNOTATIONS;
  }

}
