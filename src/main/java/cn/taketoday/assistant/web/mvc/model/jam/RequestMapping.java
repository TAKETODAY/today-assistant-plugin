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

import com.intellij.jam.JamElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamChildrenQuery;
import com.intellij.microservices.url.inlay.UrlPathInlayHint;
import com.intellij.microservices.url.inlay.UrlPathInlayHintsProviderSemElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;

import java.util.List;

import cn.taketoday.assistant.web.mvc.jam.RequestMappingDeclaration;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

public interface RequestMapping<T extends PsiMember> extends RequestMappingDeclaration<T>, JamElement, UrlPathInlayHintsProviderSemElement {
  String VALUE_ATTRIBUTE = "value";
  String PATH_ATTRIBUTE = "path";
  String METHOD_ATTRIBUTE = "method";
  String CONSUMES_ATTRIBUTE = "consumes";
  String PRODUCES_ATTRIBUTE = "produces";
  String PARAMS_ATTRIBUTE = "params";
  String HEADERS_ATTRIBUTE = "headers";
  SemKey<RequestMapping<PsiClass>> CLASS_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("RequestMappingClass",
          UrlPathInlayHintsProviderSemElement.INLAY_HINT_SEM_KEY);
  SemKey<Method> METHOD_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("RequestMappingMethod", UrlPathInlayHintsProviderSemElement.INLAY_HINT_SEM_KEY);
  JamChildrenQuery<WebMVCModelAttribute> MODEL_ATTRIBUTE_METHODS_QUERY = JamChildrenQuery.annotatedMethods(WebMVCModelAttribute.ANNOTATION_META,
          WebMVCModelAttribute.METHOD_META);

  List<JamStringAttributeElement<String>> getMappingUrls();

  @Override
  List<String> getUrls();

  @Override
  RequestMethod[] getMethods();

  List<String> getConsumes();

  List<String> getProduces();

  List<String> getParams();

  List<String> getHeaders();

  List<WebMVCModelAttribute> getModelAttributes();

  @Override
  T getPsiElement();

  @Nullable
  PsiAnnotation getIdentifyingAnnotation();

  default List<UrlPathInlayHint> getInlayHints() {
    return RequestMappingUtil.getUrlPathInlayHints(this);
  }

  interface Method extends RequestMapping<PsiMethod> {
    JamChildrenQuery<WebMVCModelAttribute> MODEL_ATTRIBUTE_PARAMETERS_QUERY = JamChildrenQuery.annotatedParameters(WebMVCModelAttribute.ANNOTATION_META,
            WebMVCModelAttribute.PARAMETER_META);
    JamChildrenQuery<MVCPathVariable> PATH_VARIABLE_PARAMETERS_QUERY = JamChildrenQuery.annotatedParameters(
            MVCPathVariable.ANNOTATION_META, MVCPathVariable.META);

    RequestMethod[] getLocalMethods();

    List<MVCPathVariable> getPathVariables();

    @Nullable
    default RequestMapping<PsiClass> getClassLevelMapping() {
      PsiClass containingClass = PsiTreeUtil.getParentOfType(getPsiElement(), PsiClass.class);
      if (containingClass == null) {
        return null;
      }
      return RequestMappingUtil.getClassLevelMapping(containingClass);
    }
  }
}
