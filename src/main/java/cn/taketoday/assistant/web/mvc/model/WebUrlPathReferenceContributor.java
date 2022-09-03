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

package cn.taketoday.assistant.web.mvc.model;

import com.intellij.microservices.jvm.url.UastReferenceInjectorUtils;
import com.intellij.microservices.jvm.url.UastUrlPathReferenceProvider;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.microservices.url.references.UrlPathReferenceInjector;
import com.intellij.microservices.url.references.UrlPksParser;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceProvider;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.psi.util.PartiallyKnownString;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UastUtils;

import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.client.WebClientModel;
import cn.taketoday.assistant.web.mvc.client.rest.RestOperationsConstants;
import cn.taketoday.assistant.web.mvc.client.rest.RestOperationsModel;
import cn.taketoday.assistant.web.mvc.client.rest.RestTemplatesModel;
import cn.taketoday.assistant.web.mvc.client.rest.TestRestTemplateModel;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import kotlin.Triple;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;

public final class WebUrlPathReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    UrlPathReferenceInjector injector = UastReferenceInjectorUtils.uastUrlPathReferenceInjectorForScheme(
            UrlConstants.HTTP_SCHEMES, InfraMvcUrlPathSpecification.parser);
    registerRestOperationsProviders(registrar, injector);
  }

  private UastUrlPathReferenceProvider webClientUrlReferenceProvider(
          UrlPathReferenceInjector<UExpression> injector, WebClientModel clientModel, boolean withPath) {
    return new UastUrlPathReferenceProvider(
            new Function2<UExpression, PsiElement, PsiReference[]>() {
              @Override
              public PsiReference[] invoke(UExpression uElement, PsiElement host) {
                injector.withDefaultRootContextProviderFactory(new Function1<UExpression, UrlPathContext>() {
                  @Override
                  public UrlPathContext invoke(UExpression expression) {
                    UrlPathContext context;
                    List list;
                    String it;
                    Intrinsics.checkNotNullParameter(expression, "expression");
                    UCallExpression uCallExpression$default = UastUtils.getUCallExpression(expression, 0);
                    if (uCallExpression$default != null) {
                      if (withPath) {
                        String it2 = clientModel.findBaseUrl(uCallExpression$default);
                        if (it2 == null) {
                          return UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, null);
                        }
                        UrlPksParser.ParsedPksUrl parsedUrl = injector.getUrlParser().parseFullUrl(new PartiallyKnownString(it2));
                        if (parsedUrl == null) {
                          return UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, null);
                        }

                        PartiallyKnownString scheme = parsedUrl.getScheme();
                        if (scheme != null && (it = scheme.getConcatenationOfKnown()) != null) {
                          list = CollectionsKt.listOf(it);
                        }
                        else {
                          list = UrlConstants.HTTP_SCHEMES;
                        }
                        PartiallyKnownString authority = parsedUrl.getAuthority();
                        Triple triple = new Triple(list, authority != null ? authority.getConcatenationOfKnown() : null, parsedUrl.getUrlPath());
                        List schemes = (List) triple.component1();
                        String authority2 = (String) triple.component2();
                        UrlPath path = (UrlPath) triple.component3();
                        context = UrlPathContext.Companion.supportingSchemes(schemes, null).subContext(path)
                                .withAuthorities(SetsKt.setOfNotNull(authority2));
                      }
                      else {
                        context = UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, null);
                      }
                      String method = clientModel.findHttpMethod(uCallExpression$default);
                      return context.withMethods(CollectionsKt.listOfNotNull(method));
                    }
                    return UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, null);
                  }

                });
                return injector.buildAbsoluteOrRelativeReferences(uElement, host);
              }

            }
    );
  }

  private void registerRestOperationsProviders(PsiReferenceRegistrar registrar, UrlPathReferenceInjector<UExpression> urlPathReferenceInjector) {
    Map<String, RestTemplatesModel> clientToModel = MapsKt.mapOf(
            TuplesKt.to(InfraMvcConstant.REST_OPERATIONS, RestOperationsModel.INSTANCE),
            TuplesKt.to(InfraMvcConstant.TEST_REST_TEMPLATE, TestRestTemplateModel.INSTANCE),
            TuplesKt.to(InfraMvcConstant.ASYNC_REST_OPERATIONS, null)
    );
    for (Map.Entry<String, RestTemplatesModel> entry : clientToModel.entrySet()) {
      String clientClass = entry.getKey();
      RestTemplatesModel clientModel = entry.getValue();
      UastReferenceProvider webClientUrlReferenceProvider =
              clientModel != null
              ? webClientUrlReferenceProvider(urlPathReferenceInjector, clientModel, false)
              : new UastUrlPathReferenceProvider(urlPathReferenceInjector::buildFullUrlReference);
      UExpressionPattern.Capture uExpression = UastPatterns.uExpression();
      PatternCondition<PsiElement> patternCondition = InfraLibraryUtil.IS_WEB_MVC_PROJECT;
      UExpressionPattern.Capture uExpression2 = UastPatterns.uExpression();
      UCallExpressionPattern callExpression = UastPatterns.callExpression();
      ElementPattern oneOf = StandardPatterns.string().oneOf(RestOperationsConstants.EXECUTE_METHOD, RestOperationsConstants.EXCHANGE_METHOD);
      UCallExpressionPattern withMethodName = callExpression.withMethodName(oneOf);
      ElementPattern definedInClass = PsiJavaPatterns.psiMethod().definedInClass(PsiJavaPatterns.psiClass().withQualifiedName(clientClass));
      UastReferenceRegistrar.registerUastReferenceProvider(registrar,
              uExpression.withSourcePsiCondition(patternCondition).withUastParentOrSelf(uExpression2.callParameter(0, withMethodName.withAnyResolvedMethod(definedInClass))),
              webClientUrlReferenceProvider, 0.0d);
    }
    Map<String, List<String>> restOperationsMethods = MapsKt.mapOf(
            TuplesKt.to("GET", CollectionsKt.listOf("getForEntity", "getForObject")),
            TuplesKt.to("HEAD", CollectionsKt.listOf("headForHeaders")),
            TuplesKt.to("POST", CollectionsKt.listOf("postForObject", "postForLocation", "postForEntity")),
            TuplesKt.to("PATCH", CollectionsKt.listOf("patchForObject")),
            TuplesKt.to("PUT", CollectionsKt.listOf("put")),
            TuplesKt.to("DELETE", CollectionsKt.listOf("delete")),
            TuplesKt.to("OPTIONS", CollectionsKt.listOf("optionsForAllow"))
    );
    ElementPattern definedInClass2 = PsiJavaPatterns.psiMethod().definedInClass(PsiJavaPatterns.psiClass()
            .withQualifiedName(StandardPatterns.string().oneOf(InfraMvcConstant.REST_OPERATIONS, InfraMvcConstant.ASYNC_REST_OPERATIONS, InfraMvcConstant.TEST_REST_TEMPLATE)));
    for (Map.Entry<String, List<String>> restOperationsMethod : restOperationsMethods.entrySet()) {
      String httpMethod = restOperationsMethod.getKey();
      UExpressionPattern.Capture uExpression3 = UastPatterns.uExpression();
      PatternCondition<PsiElement> patternCondition2 = InfraLibraryUtil.IS_WEB_MVC_PROJECT;
      UExpressionPattern.Capture uExpression4 = UastPatterns.uExpression();
      UCallExpressionPattern callExpression2 = UastPatterns.callExpression();
      ElementPattern oneOf2 = StandardPatterns.string().oneOf(restOperationsMethod.getValue());
      UastReferenceRegistrar.registerUastReferenceProvider(registrar,
              uExpression3.withSourcePsiCondition(patternCondition2)
                      .withUastParentOrSelf(uExpression4.callParameter(0, callExpression2.withMethodName(oneOf2).withAnyResolvedMethod(definedInClass2))),
              new UastUrlPathReferenceProvider(new Function2<UExpression, PsiElement, PsiReference[]>() {

                @Override
                public PsiReference[] invoke(UExpression uElement, PsiElement host) {
                  PsiReference[] buildAbsoluteOrRelativeReferences;
                  Intrinsics.checkNotNullParameter(uElement, "uElement");
                  Intrinsics.checkNotNullParameter(host, "host");
                  buildAbsoluteOrRelativeReferences = buildAbsoluteOrRelativeReferences(uElement, host, httpMethod);
                  return buildAbsoluteOrRelativeReferences;
                }

              }), 0.0d);
    }
  }

  public PsiReference[] buildAbsoluteOrRelativeReferences(UExpression uElement, PsiElement host, String method) {
    UrlPathReferenceInjector injector = UastReferenceInjectorUtils.uastUrlPathReferenceInjectorForScheme(UrlConstants.HTTP_SCHEMES, null)
            .withDefaultRootContextProviderFactory(new Function1<UExpression, UrlPathContext>() {
              @Override
              public UrlPathContext invoke(UExpression it) {
                return UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, method);
              }
            });
    return injector.buildAbsoluteOrRelativeReferences(uElement, host);
  }

  public PsiReference[] buildUrlMappingReferences(UExpression uElement, PsiElement host, String method) {
    UrlPathReferenceInjector injector = UastReferenceInjectorUtils.uastUrlPathReferenceInjectorForScheme
                    (UrlConstants.HTTP_SCHEMES, InfraMvcUrlPathSpecification.INSTANCE.getParser())
            .withDefaultRootContextProviderFactory(new Function1<UExpression, UrlPathContext>() {
              @Override
              public UrlPathContext invoke(UExpression it) {
                return UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, method);
              }
            });
    return injector.buildReferences(uElement).forPsiElement(host);
  }
}
