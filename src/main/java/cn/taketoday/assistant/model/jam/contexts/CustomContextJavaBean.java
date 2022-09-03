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

package cn.taketoday.assistant.model.jam.contexts;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ref.AnnotationChildLink;
import com.intellij.semantic.SemKey;

import java.util.List;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.model.jam.javaConfig.ContextJavaBean;
import cn.taketoday.lang.Nullable;

public class CustomContextJavaBean extends ContextJavaBean {
  public static final SemKey<JamAnnotationMeta> JAM_ANNO_META_KEY = BEAN_ANNOTATION_KEY.subKey("CustomContextJavaBean");
  public static final SemKey<JamMemberMeta<PsiMethod, CustomContextJavaBean>> META_KEY = ContextJavaBean.METHOD_META.getMetaKey().subKey("CustomContextJavaBean");
  public static final SemKey<CustomContextJavaBean> JAM_KEY = ContextJavaBean.BEAN_JAM_KEY.subKey("CustomContextJavaBean");
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final AnnotationChildLink myAnnotationChildLink;

  public CustomContextJavaBean(String anno, PsiMethod psiMethod) {
    super(psiMethod);
    this.myAnnotationChildLink = new AnnotationChildLink(anno);
    this.myPsiAnnotation = this.myAnnotationChildLink.createChildRef(getPsiElement());
  }

  @Override
  public List<JamStringAttributeElement<String>> getBeanNameAttributeValue() {
    InfraAliasFor nameAttributeAliasFor = getNameAttributeAliasFor();
    if (nameAttributeAliasFor != null) {
      return JamAttributeMeta.collectionString(nameAttributeAliasFor.getMethodName()).getJam(this.myPsiAnnotation);
    }
    InfraAliasFor valueAttributeAliasFor = getValueAttributeAliasFor();
    if (valueAttributeAliasFor != null) {
      return JamAttributeMeta.collectionString(valueAttributeAliasFor.getMethodName()).getJam(this.myPsiAnnotation);
    }
    return super.getBeanNameAttributeValue();
  }

  @Nullable
  private InfraAliasFor getNameAttributeAliasFor() {
    return AliasForUtils.findAliasFor(getPsiElement(), this.myAnnotationChildLink.getAnnotationQualifiedName(), AnnotationConstant.COMPONENT, "name");
  }

  @Nullable
  private InfraAliasFor getValueAttributeAliasFor() {
    return AliasForUtils.findAliasFor(getPsiElement(), this.myAnnotationChildLink.getAnnotationQualifiedName(), AnnotationConstant.COMPONENT, "value");
  }

  public boolean isValid() {
    return getPsiElement().isValid();
  }

  @Override
  @Nullable
  public PsiAnnotation getPsiAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }
}
