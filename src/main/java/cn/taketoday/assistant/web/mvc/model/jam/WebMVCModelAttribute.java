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

package cn.taketoday.assistant.web.mvc.model.jam;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamParameterMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.PsiType;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.util.Consumer;
import com.intellij.util.PairConsumer;

import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;
import cn.taketoday.lang.Nullable;

@Presentation(typeName = WebMvcPresentationConstant.MODEL_ATTRIBUTE)
public class WebMVCModelAttribute extends JamBaseElement<PsiModifierListOwner> {
  private static final JamStringAttributeMeta.Single<String> VALUE_META = JamAttributeMeta.singleString(RequestMapping.VALUE_ATTRIBUTE);
  private static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("name");
  private static final PairConsumer<WebMVCModelAttribute, Consumer<PomTarget>> POM_CONSUMER = (attribute, consumer) -> {
    consumer.consume(attribute.getPsiTarget());
  };

  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraMvcConstant.MODEL_ATTRIBUTE).addAttribute(VALUE_META).addAttribute(NAME_META);
  public static final JamMethodMeta<WebMVCModelAttribute> METHOD_META = new JamMethodMeta<>(WebMVCModelAttribute.class)
          .addAnnotation(ANNOTATION_META).addPomTargetProducer(POM_CONSUMER);
  public static final JamParameterMeta<WebMVCModelAttribute> PARAMETER_META = new JamParameterMeta<>(WebMVCModelAttribute.class)
          .addAnnotation(ANNOTATION_META)
          .addPomTargetProducer(POM_CONSUMER);

  public WebMVCModelAttribute(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public String getName() {
    String value = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META).getStringValue();
    if (value != null) {
      return value;
    }
    String name = ANNOTATION_META.getAttribute(getPsiElement(), NAME_META).getStringValue();
    if (name != null) {
      return name;
    }
    return InfraControllerUtils.getVariableName(getType());
  }

  @Nullable
  public PsiType getType() {
    PsiModifierListOwner psiMethod = getPsiElement();
    if (psiMethod instanceof PsiMethod method) {
      return method.getReturnType();
    }
    if (psiMethod instanceof PsiParameter) {
      return ((PsiParameter) psiMethod).getType();
    }
    return null;
  }

  public PsiTarget getPsiTarget() {
    JamStringAttributeElement<String> value = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META);
    PsiAnnotationMemberValue psiElement = value.getPsiElement();

    if (psiElement instanceof PsiLiteral) {
      return new JamPomTarget(this, value);
    }
    JamStringAttributeElement<String> name = ANNOTATION_META.getAttribute(getPsiElement(), NAME_META);
    if (name instanceof PsiLiteral) {
      return new JamPomTarget(this, name);
    }
    return new AliasingPsiTarget((PsiNamedElement) getPsiElement());
  }
}
