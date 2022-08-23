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

import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.targets.DecapitalizedAliasingPsiTarget;
import com.intellij.semantic.SemService;
import com.intellij.spring.model.jam.JamPsiClassSpringBean;
import com.intellij.spring.model.jam.javaConfig.ContextJavaBean;
import com.intellij.spring.model.jam.javaConfig.SpringJavaBean;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/23 01:53
 */
public class InfraStereotypeElement extends JamPsiClassSpringBean {
  private static final Map<String, JamAnnotationMeta> annotationMetaMap = new HashMap<>();
  private JamAnnotationMeta myMeta;

  private static final JamStringAttributeMeta.Single<String> NAME_VALUE_META = JamAttributeMeta.singleString("value");

  public List<? extends SpringJavaBean> getBeans() {
    List<SpringJavaBean> beans = new ArrayList<>();

    JamCommonUtil.processSuperClassList(getPsiElement(), new HashSet<>(), psiClass -> {
      for (PsiMethod method : psiClass.getMethods()) {
        beans.addAll(SemService.getSemService(method.getProject()).getSemElements(ContextJavaBean.BEAN_JAM_KEY, method));
      }
      return true;
    });

    return beans;
  }

  protected InfraStereotypeElement(@Nullable String anno, PsiElementRef<PsiClass> psiElementRef) {
    super(psiElementRef);
    if (anno != null)
      myMeta = getMeta(anno);
  }

  @NotNull
  private static synchronized JamAnnotationMeta getMeta(@NotNull String anno) {
    JamAnnotationMeta meta = annotationMetaMap.get(anno);
    if (meta == null) {
      meta = new JamAnnotationMeta(anno);
      annotationMetaMap.put(anno, meta);
    }
    return meta;
  }

  public static <Psi extends PsiModifierListOwner, Jam extends SpringStereotypeElement> void addPomTargetProducer(JamMemberMeta<Psi, Jam> classMeta) {
    classMeta.addPomTargetProducer((stereotypeElement, consumer) -> consumer.consume(stereotypeElement.getPsiTarget()));
  }

  public PsiTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getNamedStringAttributeElement();
    if (namedAttributeValue == null || StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getAliasingPsiTarget();
    }

    return new JamPomTarget(this, namedAttributeValue);
  }

  private PsiTarget getAliasingPsiTarget() {
    return new DecapitalizedAliasingPsiTarget(getPsiElement());
  }

  @Nullable
  private JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return myMeta == null ? null : myMeta.getAttribute(getPsiElement(), NAME_VALUE_META);
  }

  @Nullable
  public PsiAnnotation getPsiAnnotation() {
    return myMeta == null ? null : myMeta.getAnnotation(getPsiElement());
  }

  @Nullable
  protected JamAnnotationMeta getMeta() {
    return myMeta;
  }

  @Override
  public String getBeanName() {
    assert isValid();
    final String definedName = getAnnotationDefinedBeanName();

    if (!StringUtil.isEmptyOrSpaces(definedName))
      return definedName;

    return super.getBeanName();
  }

  @Nullable
  private String getAnnotationDefinedBeanName() {
    final JamStringAttributeElement<String> namedStringAttributeElement = getNamedStringAttributeElement();

    return namedStringAttributeElement == null ? null : namedStringAttributeElement.getStringValue();
  }

  @Override
  @NotNull
  public PsiElement getIdentifyingPsiElement() {
    return PomService.convertToPsi(getPsiManager().getProject(), getPsiTarget());
  }

  @Override
  public String toString() {
    final String beanName = getBeanName();
    return beanName == null ? "" : beanName;
  }
}
