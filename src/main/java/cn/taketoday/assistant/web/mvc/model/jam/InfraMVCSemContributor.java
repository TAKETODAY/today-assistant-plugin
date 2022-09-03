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

import com.intellij.microservices.url.parameters.PathVariableSemElementSupportKt;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.UastSemRegistrar;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.semantic.SemService;
import com.intellij.util.ArrayUtil;

import org.jetbrains.uast.UParameter;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.client.exchange.DeleteExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.GetExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.HeadExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.HttpExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.OptionsExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.PatchExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.PostExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.PutExchange;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeClient;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeConstant;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlResolverKt;

final class InfraMVCSemContributor extends SemContributor {

  public void registerSemProviders(SemRegistrar registrar, Project project) {
    SemService semService = SemService.getSemService(project);

    InfraRequestMapping.register(registrar);

    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiClass().nonAnnotationType().withoutModifiers("private"),
            CustomRequestMapping.ClassMapping.META_KEY, CustomRequestMapping.ClassMapping.JAM_KEY,
            SemContributorUtil.createFunction(CustomRequestMapping.ClassMapping.JAM_KEY, CustomRequestMapping.ClassMapping.class,
                    SemContributorUtil.getCustomMetaAnnotations(InfraMvcConstant.REQUEST_MAPPING), pair -> new CustomRequestMapping.ClassMapping(pair.first, pair.second),
                    CustomRequestMapping.ClassMapping.createMetaConsumer()));

    SemContributorUtil.registerMetaComponents(semService, registrar, PsiJavaPatterns.psiMethod(), CustomRequestMapping.MethodMapping.META_KEY, CustomRequestMapping.MethodMapping.JAM_KEY,
            SemContributorUtil.createFunction(CustomRequestMapping.MethodMapping.JAM_KEY, CustomRequestMapping.MethodMapping.class,
                    SemContributorUtil.getCustomMetaAnnotations(InfraMvcConstant.REQUEST_MAPPING), pair2 -> new CustomRequestMapping.MethodMapping(pair2.first, pair2.second),
                    CustomRequestMapping.MethodMapping.createMetaConsumer(),
                    AliasForUtils.getAnnotationMetaProducer(InfraRequestMapping.REQUEST_MAPPING_ANNO_META.getMetaKey(), InfraRequestMapping.MethodMapping.META)));

    WebMVCModelAttribute.METHOD_META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraMvcConstant.MODEL_ATTRIBUTE));
    WebMVCModelAttribute.PARAMETER_META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.MODEL_ATTRIBUTE));
    MVCPathVariable.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.PATH_VARIABLE));

    UastSemRegistrar.registerUastSemProvider(registrar, PathVariableSemElementSupportKt.PATH_VARIABLE_SEM_KEY, UastPatterns.capture(UParameter.class).filter(parameter -> {
      return parameter.findAnnotation(InfraMvcConstant.PATH_VARIABLE) != null;
    }), UastSemRegistrar.uastSemElementProvider(UParameter.class, (parameter2, element) -> {
      PsiElement psi = parameter2.getJavaPsi();
      if (psi == null) {
        return null;
      }
      return MVCPathVariable.META.getJamElement((PsiParameter) psi);
    }));

    WebMVCRequestParam.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.REQUEST_PARAM));
    WebMVCRequestHeader.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.REQUEST_HEADER));
    WebMVCCookieValue.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.COOKIE_VALUE));
    WebMVCRequestBody.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.REQUEST_BODY));
    WebMvcUrlResolverKt.getQueryParameterSupport().registerQueryParameterSem(registrar);
    WebMVCMatrixVariable.META.register(registrar, PsiJavaPatterns.psiParameter().withAnnotation(InfraMvcConstant.MATRIX_VARIABLE));
    HttpExchange.ClassMapping.META.register(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().isInterface().withAnnotation(InfraExchangeConstant.SPRING_HTTP_EXCHANGE));
    HttpExchange.MethodMapping.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_HTTP_EXCHANGE));
    GetExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_GET_EXCHANGE));
    HeadExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_HEAD_EXCHANGE));
    DeleteExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_DELETE_EXCHANGE));
    OptionsExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_OPTIONS_EXCHANGE));
    PatchExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_PATCH_EXCHANGE));
    PostExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_POST_EXCHANGE));
    PutExchange.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraExchangeConstant.SPRING_PUT_EXCHANGE));
    String[] methodAnnotations = ArrayUtil.toStringArray(InfraExchangeConstant.SPRING_EXCHANGE_METHOD_ANNOTATIONS);
    InfraExchangeClient.META.register(registrar, PsiJavaPatterns.psiClass().nonAnnotationType().isInterface().withMethod(false, PsiJavaPatterns.psiMethod().withAnnotations(methodAnnotations)));
  }
}
