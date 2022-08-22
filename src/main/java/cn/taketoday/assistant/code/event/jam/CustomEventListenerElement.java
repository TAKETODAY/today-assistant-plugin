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

package cn.taketoday.assistant.code.event.jam;

import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.assistant.AliasForElement;
import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 0:11
 */
public class CustomEventListenerElement implements EventListenerElement {
  public static final SemKey<CustomEventListenerElement> JAM_KEY = EVENT_LISTENER_ROOT_JAM_KEY.subKey("CustomEventListener");
  public static final JamMethodMeta<CustomEventListenerElement> META = new JamMethodMeta<>(null, CustomEventListenerElement.class, JAM_KEY);
  public static final SemKey<JamMemberMeta<PsiMethod, CustomEventListenerElement>> META_KEY = META.getMetaKey().subKey("CustomEventListener");

  private static final HashMap<String, JamAnnotationMeta> annotationMetaMap = new HashMap<>();

  private final PsiAnchor myPsiAnchor;
  private final String[] classesAttributes;
  private final JamAnnotationMeta myAnnotationMeta;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final NullableLazyValue<EventListenerElement> myDefiningMetaAnnotation;

  public CustomEventListenerElement(String anno, PsiMethod psiMethod) {
    this(getMeta(anno), psiMethod);
  }

  private static synchronized JamAnnotationMeta getMeta(String anno) {
    JamAnnotationMeta meta = annotationMetaMap.get(anno);
    if (meta == null) {
      meta = new JamAnnotationMeta(anno);
      annotationMetaMap.put(anno, meta);
    }
    return meta;
  }

  protected CustomEventListenerElement(JamAnnotationMeta annotationMeta, PsiMethod psiMethod) {
    this.classesAttributes = new String[] { "value", "event" };
    this.myDefiningMetaAnnotation = new NullableLazyValue<>() {
      @Nullable
      public EventListenerElement compute() {
        PsiMethod psiElement = getPsiElement();
        PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(psiElement,
                myAnnotationMeta.getAnnoName(), AnnotationConstant.EVENT_LISTENER);
        if (definingMetaAnnotation == null) {
          return null;
        }
        PsiClass annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true);
        if (annotationType == null) {
          return null;
        }

        return JamService.getJamService(psiElement.getProject())
                .getJamElement(annotationType, JamEventListenerElement.METHOD_META);
      }
    };
    this.myAnnotationMeta = annotationMeta;
    this.myPsiAnchor = PsiAnchor.create(psiMethod);
    this.myPsiAnnotation = this.myAnnotationMeta.getAnnotationRef(psiMethod);
  }

  @Override
  @Nullable
  public PsiMethod getPsiElement() {
    return (PsiMethod) this.myPsiAnchor.retrieve();
  }

  @Override
  @Nullable
  public PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  @Override
  public List<PsiClass> getEventListenerClasses() {
    SmartList<PsiClass> smartList = new SmartList<>();
    for (String classAttrName : classesAttributes) {
      AliasForElement aliasFor = getAliasAttribute(classAttrName);
      if (aliasFor != null) {
        for (var attributeElement : getClassCollectionMeta(aliasFor).getJam(myPsiAnnotation)) {
          ContainerUtil.addIfNotNull(smartList, attributeElement.getValue());
        }
        return smartList;
      }
    }
    EventListenerElement definingContextConfiguration = getDefiningEventListener();
    if (definingContextConfiguration != null) {
      return definingContextConfiguration.getEventListenerClasses();
    }
    return Collections.emptyList();
  }

  private static JamClassAttributeMeta.Collection getClassCollectionMeta(AliasForElement aliasFor) {
    return new JamClassAttributeMeta.Collection(aliasFor.getMethodName());
  }

  private EventListenerElement getDefiningEventListener() {
    return this.myDefiningMetaAnnotation.getValue();
  }

  private AliasForElement getAliasAttribute(String attrName) {
    PsiMethod element = getPsiElement();
    if (element == null) {
      return null;
    }
    return AliasForUtils.findAliasFor(element, this.myAnnotationMeta.getAnnoName(), AnnotationConstant.EVENT_LISTENER, attrName);
  }

  @Override
  public boolean isValid() {
    return this.myPsiAnchor.retrieve() != null;
  }
}
