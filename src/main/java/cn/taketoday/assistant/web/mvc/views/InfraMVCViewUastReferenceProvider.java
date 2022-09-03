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

package cn.taketoday.assistant.web.mvc.views;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastInjectionHostReferenceProvider;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULambdaExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UReturnExpression;
import org.jetbrains.uast.UastUtils;

import java.util.Objects;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.WebMvcFunctionalRoutingConstant;
import one.util.streamex.StreamEx;

public final class InfraMVCViewUastReferenceProvider extends UastInjectionHostReferenceProvider {
  static String REACTOR_MONO = "reactor.core.publisher.Mono";

  private static final UCallExpressionPattern MONO_RETURN_PATTERN = UastPatterns.callExpression()
          .withResolvedMethod(PsiJavaPatterns.psiMethod().withName(PsiJavaPatterns.string().oneOf("thenReturn", "defaultIfEmpty"))
                  .definedInClass(PsiJavaPatterns.psiClass().withQualifiedName(REACTOR_MONO)), false);
  private static final UCallExpressionPattern LAMBDA_IN_MONO_MAP_PATTERN = UastPatterns.callExpression()
          .withResolvedMethod(PsiJavaPatterns.psiMethod().withName("map").definedInClass(PsiJavaPatterns.psiClass().withQualifiedName(REACTOR_MONO)), false);
  private static final PatternCondition<PsiMethod> MVC_REQUEST_HANDLER_PATTERN = new PatternCondition<PsiMethod>("mvcRequestHandler") {

    public boolean accepts(PsiMethod method, ProcessingContext context) {
      PsiClass psiClass;
      if (InfraControllerUtils.isRequestHandlerCandidate(method)
              && !AnnotationUtil.isAnnotated(method, InfraMvcConstant.RESPONSE_BODY, 0)
              && (psiClass = method.getContainingClass()) != null
              && InfraControllerUtils.isRequestHandler(method)) {
        return !InfraControllerUtils.isJamRequestHandler(method) || !InfraControllerUtils.hasClassLevelResponseBody(psiClass);
      }
      return false;
    }
  };
  public static final UExpressionPattern<UExpression, UExpressionPattern.Capture<UExpression>> VIEW_PATTERN = UastPatterns.injectionHostOrReferenceExpression()
          .withSourcePsiCondition(InfraLibraryUtil.IS_WEB_MVC_PROJECT)
          .andOr(
                  UastPatterns.uExpression()
                          .setterParameter(
                                  PsiJavaPatterns.psiMethod().withName("setViewName").inClass(PsiJavaPatterns.psiClass().withQualifiedName(PsiJavaPatterns.string()
                                          .oneOf(InfraMvcConstant.MODEL_AND_VIEW, InfraMvcConstant.VIEW_CONTROLLER_REGISTRATION)))
                          ), UastPatterns.uExpression()
                          .methodCallParameter(0, PsiJavaPatterns.psiMethod().withName("view").inClass(PsiJavaPatterns.psiClass().withQualifiedName(WebMvcFunctionalRoutingConstant.RENDERING))),
                  UastPatterns.uExpression().methodCallParameter(0,
                          PsiJavaPatterns.psiMethod().withName("render")
                                  .inClass(PsiJavaPatterns.psiClass().withQualifiedName(WebMvcFunctionalRoutingConstant.BODY_BUILDER))),
                  UastPatterns.uExpression().constructorParameter(0, InfraMvcConstant.MODEL_AND_VIEW),
                  UastPatterns.uExpression().filterWithContext((uLiteral, ctx) -> {
                    UReturnExpression parent = StreamEx.iterate(uLiteral, Objects::nonNull, UElement::getUastParent)
                            .limit(5L)
                            .takeWhile(ex -> {
                              return !(ex instanceof UCallExpression) || isReactiveCall((UCallExpression) ex);
                            })
                            .select(UReturnExpression.class)
                            .findFirst()
                            .orElse(null);
                    return isReturnStatement(parent, ctx);
                  })
          );

  private final InfraMVCViewReferenceProvider myProvider;

  private static Boolean isReturnStatement(UReturnExpression returnExpression, ProcessingContext ctx) {
    UCallExpression uastParent;
    if (returnExpression != null) {
      UElement jumpTarget = returnExpression.getJumpTarget();
      if (jumpTarget instanceof UMethod method) {
        return MVC_REQUEST_HANDLER_PATTERN.accepts(method.getJavaPsi(), ctx);
      }
      else if ((jumpTarget instanceof ULambdaExpression) && (uastParent = ObjectUtils.tryCast(jumpTarget.getUastParent(),
              UCallExpression.class)) != null && LAMBDA_IN_MONO_MAP_PATTERN.accepts(uastParent)) {
        return isReturnStatement(UastUtils.getParentOfType(uastParent, UReturnExpression.class), ctx);
      }
    }
    return false;
  }

  private InfraMVCViewUastReferenceProvider(boolean soft) {
    this.myProvider = new InfraMVCViewReferenceProvider(soft);
  }

  public static void register(PsiReferenceRegistrar registrar) {
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, VIEW_PATTERN, new InfraMVCViewUastReferenceProvider(false), 100.0d);
  }

  public PsiReference[] getReferencesForInjectionHost(UExpression uExpression, PsiLanguageInjectionHost host, ProcessingContext context) {
    return this.myProvider.getReferencesByElement(host, context);
  }

  public static boolean isReactiveCall(UCallExpression e) {
    return MONO_RETURN_PATTERN.accepts(e);
  }
}
