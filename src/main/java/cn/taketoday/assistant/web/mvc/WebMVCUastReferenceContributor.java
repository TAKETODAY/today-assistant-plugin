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

package cn.taketoday.assistant.web.mvc;

import com.intellij.microservices.jvm.pathvars.PathVariableReferenceProvider;
import com.intellij.microservices.jvm.pathvars.usages.PathVariableSemParametersUsageSearcher;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.util.Function;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeConstant;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlResolverKt;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.xml.CorsHeadersVariantsConverter;
import cn.taketoday.assistant.web.mvc.model.xml.CorsMappingAllowedMethodsConverter;
import cn.taketoday.assistant.web.mvc.pathVariables.MVCRequestMappingReferenceProvider;
import cn.taketoday.assistant.web.mvc.pathVariables.WebMvcPathVariableDeclaration;
import cn.taketoday.assistant.web.mvc.views.InfraMVCViewUastReferenceProvider;

final class WebMVCUastReferenceContributor extends PsiReferenceContributor {
  private static final String CORS_REGISTRATION = "cn.taketoday.web.config.CorsRegistration";

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    InfraMVCViewUastReferenceProvider.register(registrar);
    WebControllerModelVariablesCollector.registerModelVariablesReferenceProvider(registrar);
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, WebMvcPathVariableDeclaration.PATH_VARIABLE_DECLARATION_PATTERN, WebMvcPathVariableDeclaration.PROVIDER, 100.0d);
    MVCRequestMappingReferenceProvider.register(registrar);
    WebMvcUrlResolverKt.getQueryParameterSupport().registerReferences(registrar);
    registerCors(registrar);
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, UastPatterns.uExpression().withSourcePsiCondition(InfraLibraryUtil.IS_WEB_MVC_PROJECT)
                    .annotationParams(InfraExchangeConstant.SPRING_EXCHANGE_METHOD_ANNOTATIONS, StandardPatterns.string().oneOf(RequestMapping.VALUE_ATTRIBUTE, "url")),
            new PathVariableReferenceProvider(InfraExchangeUrlPathSpecification.INSTANCE, PathVariableSemParametersUsageSearcher.INSTANCE), 100.0d);
  }

  private static void registerCors(PsiReferenceRegistrar registrar) {
    registerCorsRegistrationVariants(registrar, "allowedHeaders", reference -> CorsHeadersVariantsConverter.getVariants(true));
    registerCorsRegistrationVariants(registrar, "exposedHeaders", reference2 -> CorsHeadersVariantsConverter.getVariants(false));
    registerCorsRegistrationVariants(registrar, "allowedMethods", reference3 -> CorsMappingAllowedMethodsConverter.getVariants());
  }

  private static void registerCorsRegistrationVariants(PsiReferenceRegistrar registrar, String methodName, Function<? super PsiReference, Object[]> variantsFunction) {
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, UastPatterns.injectionHostUExpression().withSourcePsiCondition(InfraLibraryUtil.IS_WEB_MVC_PROJECT)
                    .inCall(UastPatterns.callExpression().withMethodName(methodName).withReceiver(PsiJavaPatterns.psiClass().inheritorOf(false, CORS_REGISTRATION))),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider((uElement, referencePsiElement) -> {
              return new PsiReference[] { new PsiReferenceBase.Immediate<PsiElement>(referencePsiElement, referencePsiElement) {

                public Object[] getVariants() {
                  return variantsFunction.fun(this);
                }
              } };
            }), 100.0d);
  }
}
