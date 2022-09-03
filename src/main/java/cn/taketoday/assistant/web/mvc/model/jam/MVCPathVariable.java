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
import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamBooleanAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamParameterMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.pathvars.PathVariableSemJamBase;
import com.intellij.microservices.url.parameters.PathVariableSem;
import com.intellij.microservices.url.parameters.PathVariableSemElementSupportKt;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.semantic.SemKey;

import java.util.Objects;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.WebUrlPathSpecificationProviderKt;
import cn.taketoday.assistant.web.mvc.pathVariables.MVCRequestMappingReferenceProvider;
import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;

@Presentation(typeName = WebMvcPresentationConstant.PATH_VARIABLE, icon = "cn.taketoday.assistant.Icons.SpringProperty")
public class MVCPathVariable extends PathVariableSemJamBase implements JamElement, PathVariableSem {
  private static final SemKey<MVCPathVariable> SEM_KEY = JamService.JAM_ELEMENT_KEY.subKey(
          "MVCPathVariable", PathVariableSemElementSupportKt.PATH_VARIABLE_SEM_KEY);
  private static final JamStringAttributeMeta.Single<String> VALUE_META = JamAttributeMeta.singleString(RequestMapping.VALUE_ATTRIBUTE);
  private static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("name");
  private static final JamBooleanAttributeMeta REQUIRED_META = JamAttributeMeta.singleBoolean("required", true);
  static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(InfraMvcConstant.PATH_VARIABLE).addAttribute(VALUE_META).addAttribute(NAME_META).addAttribute(REQUIRED_META);
  public static final JamMemberMeta<PsiParameter, MVCPathVariable> META = new JamParameterMeta<>(null, MVCPathVariable.class, SEM_KEY).addAnnotation(ANNOTATION_META);
  private final PsiAnchor myPsiAnchor;

  public MVCPathVariable(PsiElementRef<?> ref) {
    super(WebUrlPathSpecificationProviderKt.getFrameworkUrlPathSpecification(),
            new MVCRequestMappingReferenceProvider.MyPathVariableDefinitionsSearcher(), META);
    this.myPsiAnchor = PsiAnchor.create(Objects.requireNonNull(ref.getPsiElement()));
  }

  public boolean isActualNameHolder() {
    String value = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META).getStringValue();
    if (value != null) {
      return false;
    }
    String name = ANNOTATION_META.getAttribute(getPsiElement(), NAME_META).getStringValue();
    return name == null;
  }

  public boolean isRequired() {
    return ANNOTATION_META.getAttribute(getPsiElement(), REQUIRED_META).getValue();
  }

  public String getName() {
    String value = ANNOTATION_META.getAttribute(getPsiElement(), VALUE_META).getStringValue();
    if (value != null) {
      return value;
    }
    String name = ANNOTATION_META.getAttribute(getPsiElement(), NAME_META).getStringValue();
    if (name != null) {
      return name;
    }
    return getPsiElement().getName();
  }

  public PsiType getType() {
    return getPsiElement().getType();
  }

  protected PsiParameter getPsiElement() {
    return (PsiParameter) this.myPsiAnchor.retrieveOrThrow();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MVCPathVariable variable = (MVCPathVariable) o;
    return Objects.equals(this.myPsiAnchor, variable.myPsiAnchor);
  }

  public int hashCode() {
    return Objects.hash(this.myPsiAnchor);
  }
}
