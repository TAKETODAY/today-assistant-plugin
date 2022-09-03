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
import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.model.config.jam.StringLiteralPsiClassConverter;

abstract class AutoConfigureByClassesOrderBase extends JamBaseElement<PsiModifierListOwner> {
  private static final JamStringAttributeMeta.Collection<PsiClass> NAME_ATTRIBUTE = JamAttributeMeta.collectionString(InfraMetadataConstant.NAME, new StringLiteralPsiClassConverter());
  private static final JamClassAttributeMeta.Collection VALUE_ATTRIBUTE = JamAttributeMeta.classCollection("value");
  protected static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype().addAttribute(NAME_ATTRIBUTE).addAttribute(VALUE_ATTRIBUTE);

  protected abstract JamAnnotationMeta getAnnotationMeta();

  protected AutoConfigureByClassesOrderBase(PsiElementRef<?> ref) {
    super(ref);
  }

  public List<PsiClass> getClasses() {
    PsiElementRef<PsiAnnotation> annotationRef = getAnnotationMeta().getAnnotationRef(getPsiElement());
    List<JamClassAttributeElement> valueElements = VALUE_ATTRIBUTE.getJam(annotationRef);
    if (!valueElements.isEmpty()) {
      return ContainerUtil.mapNotNull(valueElements, JamClassAttributeElement::getValue);
    }
    List<JamStringAttributeElement<PsiClass>> nameElements = NAME_ATTRIBUTE.getJam(annotationRef);
    return ContainerUtil.mapNotNull(nameElements, JamStringAttributeElement::getValue);
  }
}
