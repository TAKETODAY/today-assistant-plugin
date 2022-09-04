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

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.semantic.SemKey;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

public class InfraContextImport extends CommonModelElement.PsiBase implements InfraImport {
  private static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = IMPORT_JAM_ANNOTATION_KEY.subKey("InfraContextImport");
  private static final SemKey<InfraContextImport> JAM_KEY = IMPORT_JAM_KEY.subKey("InfraContextImport");
  private static final JamClassAttributeMeta.Collection VALUE_ATTR_META = new JamClassAttributeMeta.Collection("value");
  private static final JamAnnotationArchetype ARCHETYPE = new JamAnnotationArchetype().addAttribute(VALUE_ATTR_META);
  private static final JamAnnotationMeta ANNO_META = new JamAnnotationMeta(AnnotationConstant.CONTEXT_IMPORT, ARCHETYPE, JAM_ANNO_META_KEY);
  public static final JamClassMeta<InfraContextImport> META = new JamClassMeta<>(null, InfraContextImport.class, JAM_KEY).addAnnotation(ANNO_META);
  private final PsiClass myPsiElement;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public InfraContextImport(PsiClass psiClass) {
    this.myPsiElement = psiClass;
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  @Override
  public List<PsiClass> getImportedClasses() {
    SmartList<PsiClass> smartList = new SmartList<>();
    for (JamClassAttributeElement jamClassAttributeElement : ANNO_META.getAttribute(getPsiElement(), VALUE_ATTR_META)) {
      ContainerUtil.addIfNotNull(smartList, jamClassAttributeElement.getValue());
    }
    return smartList;
  }

  public boolean processImportedClasses(Processor<Pair<PsiClass, ? extends PsiElement>> processor) {
    List<JamClassAttributeElement> valueAttributeElements = ANNO_META.getAttribute(getPsiElement(), VALUE_ATTR_META);
    boolean useAnnotationAsElement = valueAttributeElements.size() == 1;
    PsiAnnotationMemberValue annotation = ANNO_META.getAnnotation(this.myPsiElement);
    for (JamClassAttributeElement element : valueAttributeElements) {
      PsiClass value = element.getValue();
      if (value != null) {
        if (!processor.process(Pair.create(value, useAnnotationAsElement ? annotation : element.getPsiElement()))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public PsiClass getPsiElement() {
    PsiClass psiClass = this.myPsiElement;
    return psiClass;
  }

  public PsiElement getIdentifyingPsiElement() {
    PsiAnnotation annotation = ANNO_META.getAnnotation(this.myPsiElement);
    return annotation != null ? annotation : getPsiElement();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }
}
