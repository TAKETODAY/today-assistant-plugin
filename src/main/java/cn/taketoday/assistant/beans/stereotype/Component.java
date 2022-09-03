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
import com.intellij.openapi.module.Module;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.semantic.SemRegistrar;
import com.intellij.util.Function;

import java.util.Collection;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.lang.Nullable;

@Presentation(typeName = PresentationConstant.COMPONENT)
public class Component extends InfraMetaStereotypeComponent {

  public static final JamClassMeta<Component> META = new JamClassMeta<>(null, Component.class,
          JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("ComponentBean"));

  static {
    addPomTargetProducer(META);
  }

  private static final Function<Module, Collection<String>> ANNOTATIONS
          = module -> getAnnotations(module, AnnotationConstant.COMPONENT);

  public static Function<Module, Collection<String>> getAnnotations() {
    return ANNOTATIONS;
  }

  public Component(PsiClass psiClass) {
    super(AnnotationConstant.COMPONENT, psiClass);
  }

  @Nullable
  public static Component from(PsiElement element) {
    return JamService.getJamService(element.getProject()).getJamElement(element, META);
  }

  public static void register(SemRegistrar registrar, PsiClassPattern prototype) {
    META.register(registrar, prototype.withAnnotation(AnnotationConstant.COMPONENT));
  }

}
