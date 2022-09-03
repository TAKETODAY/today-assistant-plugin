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

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemService;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 22:18
 */
public class ContextImport extends CommonModelElement.PsiBase implements CommonModelElement, JamElement {
  private static final SemKey<JamAnnotationMeta> IMPORT_JAM_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("ContextImport");
  private static final SemKey<ContextImport> IMPORT_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("ContextImport");
  private static final SemKey<JamMemberMeta> IMPORT_META_KEY = JamService.getMetaKey(IMPORT_JAM_KEY);
  private static final String VALUE_ATTR_NAME = "value";

  private static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY;
  private static final SemKey<ContextImport> JAM_KEY;
  private static final JamClassAttributeMeta.Collection VALUE_ATTR_META;
  private static final JamAnnotationArchetype ARCHETYPE;
  private static final JamAnnotationMeta ANNO_META;
  public static final JamClassMeta<ContextImport> META;
  private final PsiClass myPsiElement;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public ContextImport(PsiClass psiClass) {
    super();
    this.myPsiElement = psiClass;
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  public List<PsiClass> getImportedClasses() {
    SmartList<PsiClass> imported = new SmartList<>();
    for (JamClassAttributeElement element : ANNO_META.getAttribute(myPsiElement, VALUE_ATTR_META)) {
      ContainerUtil.addIfNotNull(imported, element.getValue());
    }
    return imported;
  }

  public boolean processImportedClasses(Processor<Pair<PsiClass, ? extends PsiElement>> processor) {
    List<JamClassAttributeElement> valueAttributeElements = ANNO_META.getAttribute(this.myPsiElement, VALUE_ATTR_META);
    boolean useAnnotationAsElement = valueAttributeElements.size() == 1;
    PsiElement annotationElement = ANNO_META.getAnnotation(this.myPsiElement);

    for (JamClassAttributeElement element : valueAttributeElements) {
      PsiClass value = element.getValue();
      if (value != null && processor.process(Pair.create(value, useAnnotationAsElement ? annotationElement : element.getPsiElement()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public PsiClass getPsiElement() {
    return this.myPsiElement;
  }

  public PsiElement getIdentifyingPsiElement() {
    PsiAnnotation annotation = ANNO_META.getAnnotation(this.myPsiElement);
    return annotation != null ? annotation : this.myPsiElement;
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  @Nullable
  public static ContextImport from(PsiElement element) {
    return SemService.getSemService(element.getProject()).getSemElement(IMPORT_JAM_KEY, element);
  }

  static {
    JAM_ANNO_META_KEY = IMPORT_JAM_ANNOTATION_KEY.subKey("ContextImport");
    JAM_KEY = IMPORT_JAM_KEY.subKey("ContextImport");
    VALUE_ATTR_META = new JamClassAttributeMeta.Collection(VALUE_ATTR_NAME);
    ARCHETYPE = (new JamAnnotationArchetype()).addAttribute(VALUE_ATTR_META);
    ANNO_META = new JamAnnotationMeta(AnnotationConstant.CONTEXT_IMPORT, ARCHETYPE, JAM_ANNO_META_KEY);
    META = new JamClassMeta<>(null, ContextImport.class, JAM_KEY).addAnnotation(ANNO_META);
  }
}

