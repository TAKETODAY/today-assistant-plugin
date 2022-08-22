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

package cn.taketoday.assistant.beans.mock;

import com.intellij.jam.JamClassAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.spring.boot.model.testing.jam.mock.MockBean;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;
import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/21 16:18
 */
public class JamMockBean implements MockBean {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY;
  public static final SemKey<JamMockBean> JAM_KEY;
  public static final JamClassMeta<JamMockBean> META;
  protected static final JamClassAttributeMeta.Collection VALUE_ATTR_META;
  protected static final JamClassAttributeMeta.Collection CLASSES_ATTR_META;
  public static final JamAnnotationArchetype ARCHETYPE;
  public static final JamAnnotationMeta ANNO_META;
  private final PsiClass myPsiClass;
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public JamMockBean(PsiClass psiClass) {
    super();
    this.myPsiClass = psiClass;
    this.myPsiAnnotation = ANNO_META.getAnnotationRef(psiClass);
  }

  public JamMockBean(PsiAnnotation annotation) {
    super();
    this.myPsiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class, true);
    this.myPsiAnnotation = PsiElementRef.real(annotation);
  }

  public @Nullable PsiClass getPsiElement() {
    return this.myPsiClass;
  }

  public @Nullable PsiAnnotation getAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  public List<PsiClass> getMockClasses() {
    List<PsiClass> imported = new SmartList();
    this.addMockClasses(imported, CLASSES_ATTR_META);
    this.addMockClasses(imported, VALUE_ATTR_META);
    return imported;
  }

  private void addMockClasses(List<PsiClass> imported, JamClassAttributeMeta.Collection meta) {
    for (JamClassAttributeElement jamClassAttributeElement : meta.getJam(this.myPsiAnnotation)) {
      ContainerUtil.addIfNotNull(imported, jamClassAttributeElement.getValue());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    else if (o != null && this.getClass() == o.getClass()) {
      JamMockBean bean = (JamMockBean) o;
      return Objects.equals(this.myPsiClass, bean.myPsiClass) && Objects.equals(this.myPsiAnnotation, bean.myPsiAnnotation);
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.myPsiClass, this.myPsiAnnotation);
  }

  static {
    JAM_ANNO_META_KEY = MOCK_BEAN_JAM_ANNOTATION_KEY.subKey("JamMockBean", new SemKey[0]);
    JAM_KEY = MOCK_BEAN_JAM_KEY.subKey("JamMockBean", new SemKey[0]);
    META = new JamClassMeta<>(null, JamMockBean.class, JAM_KEY);
    VALUE_ATTR_META = new JamClassAttributeMeta.Collection("value");
    CLASSES_ATTR_META = new JamClassAttributeMeta.Collection("event");
    ARCHETYPE = (new JamAnnotationArchetype()).addAttribute(VALUE_ATTR_META).addAttribute(CLASSES_ATTR_META);
    ANNO_META = new JamAnnotationMeta("cn.taketoday.framework.test.mock.mockito.MockBean", ARCHETYPE, JAM_ANNO_META_KEY);
    META.addAnnotation(ANNO_META);
  }
}
