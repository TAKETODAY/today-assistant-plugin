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

package cn.taketoday.assistant.web.mvc.pathVariables;

import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.microservices.jvm.pathvars.usages.AnnotationParamSearcherUtils;
import com.intellij.microservices.url.parameters.PathVariableDeclaringReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMember;
import com.intellij.semantic.SemKey;
import com.intellij.util.Plow;
import com.intellij.util.Processor;

import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UPolyadicExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastLiteralUtils;
import org.jetbrains.uast.UastUtils;

import java.util.List;

import cn.taketoday.assistant.web.mvc.client.exchange.HttpExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeMapping;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.lang.Nullable;
import one.util.streamex.StreamEx;

public final class WebMvcPathVariableDeclarationSearcher {

  @Nullable
  public static PathVariableDeclaringReference findPathVariableDeclaration(String pathVariableName, PsiElement context) {
    return collectDeclarations(context).find(reference -> {
      return reference.getValue().equals(pathVariableName);
    });
  }

  public static Plow<PathVariableDeclaringReference> collectDeclarations(@Nullable PsiElement context) {
    return collectDeclarations(UastUtils.findContaining(context, UMethod.class));
  }

  public static Plow<PathVariableDeclaringReference> collectDeclarations(@Nullable UMethod method) {
    return Plow.of(processor -> {
      if (!processRequestMapping(processor, method, RequestMapping.METHOD_JAM_KEY)) {
        return false;
      }
      UClass clazz = UastUtils.getContainingUClass(method);
      if (processRequestMapping(processor, clazz, RequestMapping.CLASS_JAM_KEY) && processExchangeMappings(processor, method, InfraExchangeMapping.MAPPING_JAM_KEY)) {
        return processExchangeMappings(processor, clazz, HttpExchange.CLASS_JAM_KEY);
      }
      return false;
    });
  }

  private static <T extends PsiMember> boolean processRequestMapping(
          Processor<PathVariableDeclaringReference> processor,
          @Nullable UDeclaration modifierListOwner, SemKey<? extends RequestMapping<T>> jamKey) {
    PsiElement jvmDeclaration;
    if (modifierListOwner == null || (jvmDeclaration = modifierListOwner.getJavaPsi()) == null) {
      return false;
    }
    RequestMapping<? extends PsiMember> requestMapping = JamService.getJamService(jvmDeclaration.getProject()).getJamElement(jamKey, jvmDeclaration);
    if (requestMapping != null) {
      return walkUrlAttributes(requestMapping.getMappingUrls(), processor);
    }
    return true;
  }

  public static StreamEx<PathVariableDeclaringReference> getReferencesForUrlPsiElement(UExpression expression) {
    if (expression == null) {
      return StreamEx.empty();
    }
    PsiLanguageInjectionHost sourceInjectionHost = UastLiteralUtils.getSourceInjectionHost(expression);
    if (sourceInjectionHost != null) {
      return StreamEx.of(sourceInjectionHost.getReferences())
              .select(PathVariableDeclaringReference.class);
    }
    else if (expression instanceof UReferenceExpression) {
      return StreamEx.of(AnnotationParamSearcherUtils.deepReferencesSearch(expression))
              .select(PathVariableDeclaringReference.class);
    }
    else if (expression instanceof UPolyadicExpression) {
      return StreamEx.of(((UPolyadicExpression) expression).getOperands())
              .flatMap(WebMvcPathVariableDeclarationSearcher::getReferencesForUrlPsiElement);
    }
    else {
      return StreamEx.empty();
    }
  }

  private static boolean processExchangeMappings(Processor<PathVariableDeclaringReference> processor,
          @Nullable UDeclaration modifierListOwner, SemKey<? extends InfraExchangeMapping<?>> jamKey) {
    PsiElement jvmDeclaration;
    if (modifierListOwner == null || (jvmDeclaration = modifierListOwner.getJavaPsi()) == null) {
      return false;
    }
    InfraExchangeMapping<?> exchangeMapping = JamService.getJamService(jvmDeclaration.getProject()).getJamElement(jamKey, jvmDeclaration);
    if (exchangeMapping != null) {
      return walkUrlAttributes(exchangeMapping.getUrls(), processor);
    }
    return true;
  }

  private static boolean walkUrlAttributes(List<JamStringAttributeElement<String>> requestMappingAttributes,
          Processor<PathVariableDeclaringReference> processor) {
    return StreamEx.of(requestMappingAttributes)
            .map(urlAttribute -> {
              return UastContextKt.toUElement(urlAttribute.getPsiElement(), UExpression.class);
            })
            .nonNull()
            .flatMap(WebMvcPathVariableDeclarationSearcher::getReferencesForUrlPsiElement)
            .allMatch(processor::process);
  }
}
