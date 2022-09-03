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

package cn.taketoday.assistant.model.jam.dependsOn;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.PresentationConstant;
import cn.taketoday.assistant.model.BeanPointer;
import cn.taketoday.assistant.model.jam.converters.InfraBeanReferenceJamConverter;
import cn.taketoday.lang.Nullable;

@Presentation(typeName = PresentationConstant.DEPENDS_ON)
public class InfraJamDependsOn extends CommonModelElement.PsiBase implements JamElement {
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final PsiMember myPsiMember;
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = JamService.ANNO_META_KEY.subKey("SpringJamDependsOn");
  private static final SemKey<InfraJamDependsOn> JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("SpringJamDependsOn");
  private static final JamStringAttributeMeta.Collection<BeanPointer<?>> VALUE_ATTR_META = JamAttributeMeta.collectionString("value", new InfraBeanReferenceJamConverter(null));
  private static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype().addAttribute(VALUE_ATTR_META);
  public static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.DEPENDS_ON, ARCHETYPE, JAM_ANNO_META_KEY);
  public static final JamMemberMeta<PsiMember, InfraJamDependsOn> META = new JamMemberMeta<>(null, InfraJamDependsOn.class, JAM_KEY);

  static {
    META.addAnnotation(ANNO_META);
  }

  public InfraJamDependsOn(PsiMember psiMember) {
    this.myPsiMember = psiMember;
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiMember);
  }

  public InfraJamDependsOn(PsiAnnotation annotation) {
    this.myPsiMember = PsiTreeUtil.getParentOfType(annotation, PsiMember.class, true);
    this.myPsiAnnotation = PsiElementRef.real(annotation);
  }

  public PsiMember getPsiElement() {
    return this.myPsiMember;
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  public List<JamStringAttributeElement<BeanPointer<?>>> getDependsOnAttributes() {
    return VALUE_ATTR_META.getJam(this.myPsiAnnotation);
  }
}
