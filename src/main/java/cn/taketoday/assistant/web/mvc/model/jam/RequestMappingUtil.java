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

import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.jvm.url.AnnotationUrlPathInlayHint;
import com.intellij.microservices.url.inlay.PsiElementUrlPathInlayHint;
import com.intellij.microservices.url.inlay.UrlPathInlayHint;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public final class RequestMappingUtil {

  static RequestMethod[] getAllRequestMethods(RequestMapping.Method requestMapping) {
    RequestMethod[] methods = requestMapping.getLocalMethods();
    RequestMapping<PsiClass> classLevelMapping = requestMapping.getClassLevelMapping();
    if (classLevelMapping == null) {
      return methods;
    }
    Set<RequestMethod> allRequestMethods = new LinkedHashSet<>();
    ContainerUtil.addAll(allRequestMethods, classLevelMapping.getMethods());
    ContainerUtil.addAll(allRequestMethods, methods);
    return allRequestMethods.toArray(RequestMethod.EMPTY_ARRAY);
  }

  @Nullable
  public static RequestMapping<PsiClass> getClassLevelMapping(PsiClass psiClass) {
    return JamService.getJamService(psiClass.getProject()).getJamElement(RequestMapping.CLASS_JAM_KEY, psiClass);
  }

  static List<UrlPathInlayHint> getUrlPathInlayHints(RequestMapping<?> requestMapping) {
    StreamEx map = StreamEx.of(requestMapping.getMappingUrls()).map((attribute) -> {
              return (Pair.NonNull) ObjectUtils.doIfNotNull(attribute.getPsiElement(), (element) -> {
                return Pair.createNonNull(attribute, element);
              });
            })
            .nonNull()
            .map(attributeAndElement -> {
              UrlPathContext context = getUrlPathContext(requestMapping);
              return new PsiElementUrlPathInlayHint((PsiElement) attributeAndElement.second, withLastSegments(context, ((JamStringAttributeElement) attributeAndElement.first).getStringValue()));
            });
    List<UrlPathInlayHint> valueElements = map.map(UrlPathInlayHint.class::cast).toList();
    if (valueElements.isEmpty() && requestMapping.getMappingUrls().isEmpty()) {
      return Objects.requireNonNullElse(ObjectUtils.doIfNotNull(requestMapping.getIdentifyingAnnotation(), annotation -> {
        return Collections.singletonList(new AnnotationUrlPathInlayHint(annotation, getUrlPathContext(requestMapping)));
      }), List.of());
    }
    return valueElements;
  }

  public static UrlPathContext getUrlPathContext(RequestMapping<?> requestMapping) {
    return InfraMvcUrlPathSpecification.INSTANCE.getUrlPathContext(requestMapping.getPsiElement());
  }

  private static UrlPathContext withLastSegments(UrlPathContext urlPathContext, @Nullable String segments) {
    if (segments != null) {
      return urlPathContext.withoutLastAppendedText().subContext(InfraMvcUrlPathSpecification.INSTANCE.parsePath(segments));
    }
    return urlPathContext;
  }
}
