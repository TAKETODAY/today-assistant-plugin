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

package cn.taketoday.assistant.web.mvc.navigation.requests.rest;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.httpClient.actions.generation.HttpRequestGenerationManager;
import com.intellij.httpClient.actions.generation.HttpRequestUrlsGenerationRequest;
import com.intellij.httpClient.actions.generation.PartialResultKt;
import com.intellij.httpClient.http.request.microservices.HttpRequestMicroservicesUtil;
import com.intellij.httpClient.http.request.microservices.OpenInHttpClientGotoRelatedItem;
import com.intellij.jam.JamService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UElementKt;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeMapping;
import cn.taketoday.assistant.web.mvc.client.exchange.InfraExchangeUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.request.InfraMergingMvcRequestMappingLineMarkerProvider;
import kotlin.jvm.internal.Intrinsics;

import static cn.taketoday.assistant.gutter.GutterIconBuilder.CustomNavigationHandlerBuilder.createBuilder;

public final class WebWebOpenInHttpClientLineMarkerProvider implements InfraMergingMvcRequestMappingLineMarkerProvider {

  @Override
  public boolean collectLineMarkers(PsiElement element, Collection<RelatedItemLineMarkerInfo<PsiElement>> collection) {
    UMethod uMethod;
    PsiClass containingClass;
    UElement uParentForIdentifier = UastUtils.getUParentForIdentifier(element);

    if (!(uParentForIdentifier instanceof UMethod)) {
      uParentForIdentifier = null;
    }

    if (uParentForIdentifier instanceof UMethod uMethod2) {
      uMethod = !Intrinsics.areEqual(UElementKt.getSourcePsiElement(uMethod2.getUastAnchor()), element) ? null : uMethod2;
    }
    else {
      uMethod = null;
    }
    if (uMethod == null) {
      return false;
    }
    PsiMethod psiMethod = uMethod.getJavaPsi();
    if (psiMethod.isConstructor() || (containingClass = psiMethod.getContainingClass()) == null) {
      return false;
    }
    return collectControllerMarkers(element, containingClass, psiMethod, collection)
            || collectExchangeClientMarkers(element, containingClass, psiMethod, collection);
  }

  private boolean collectControllerMarkers(PsiElement element, PsiClass containingClass,
          PsiMethod psiMethod, Collection<RelatedItemLineMarkerInfo<PsiElement>> collection) {
    RequestMapping.Method requestMethodInfo;
    if (InfraUtils.isBeanCandidateClass(containingClass) && (requestMethodInfo = JamService.getJamService(element.getProject())
            .getJamElement(RequestMapping.METHOD_JAM_KEY, psiMethod)) != null && InfraControllerUtils.isRequestHandler(psiMethod)) {
      Project project = element.getProject();
      InfraMvcUrlPathSpecification infraMvcUrlPathSpecification = InfraMvcUrlPathSpecification.INSTANCE;
      PsiMethod psiElement = requestMethodInfo.getPsiElement();
      List it = PartialResultKt.unwrapSuccess(HttpRequestMicroservicesUtil.generationRequestsFromUrlPathContext(project, infraMvcUrlPathSpecification.getUrlPathContext(psiElement)));
      List list = !it.isEmpty() ? it : null;
      if (list != null) {
        List generationRequests = list;
        return buildLineMarker(generationRequests, collection, element);
      }
      return false;
    }
    return false;
  }

  private boolean collectExchangeClientMarkers(PsiElement element, PsiClass containingClass,
          PsiMethod psiMethod, Collection<RelatedItemLineMarkerInfo<PsiElement>> collection) {
    InfraExchangeMapping exchangeMethod;
    if (containingClass.isInterface() && (exchangeMethod = JamService.getJamService(element.getProject()).getJamElement(InfraExchangeMapping.MAPPING_JAM_KEY, psiMethod)) != null) {
      Project project = element.getProject();
      InfraExchangeUrlPathSpecification infraExchangeUrlPathSpecification = InfraExchangeUrlPathSpecification.INSTANCE;
      PsiMember psiElement = (PsiMember) exchangeMethod.getPsiElement();
      List it = PartialResultKt.unwrapSuccess(
              HttpRequestMicroservicesUtil.generationRequestsFromUrlPathContext(project, infraExchangeUrlPathSpecification.getUrlPathContext(psiElement)));
      List generationRequests = !it.isEmpty() ? it : null;
      if (generationRequests != null) {
        return buildLineMarker(generationRequests, collection, element);
      }
    }
    return false;
  }

  private boolean buildLineMarker(List<HttpRequestUrlsGenerationRequest> list, Collection<RelatedItemLineMarkerInfo<PsiElement>> collection, PsiElement element) {
    var builder = createBuilder(Icons.Gutter.RequestMapping, InfraAppBundle.message("request.mapping.gutter.open.in.http.name"),
            new GutterIconNavigationHandler() {
              public void navigate(MouseEvent e, PsiElement elt) {
                Project project = elt.getProject();
                new HttpRequestGenerationManager(project).generateRequestsInHttpEditor(list);
              }
            }, null);

    Project project = element.getProject();
    var markerInfo = createBuilder(Icons.Gutter.RequestMapping, InfraAppBundle.message("request.mapping.gutter.open.in.http.name"),
            new GutterIconNavigationHandler() {
              public void navigate(MouseEvent e, PsiElement elt) {
                Project project = elt.getProject();
                new HttpRequestGenerationManager(project).generateRequestsInHttpEditor(list);
              }
            }, null).withElementPresentation(InfraAppBundle.message("request.mapping.gutter.open.in.http.name"))
            .withAdditionalGotoRelatedItems(new OpenInHttpClientGotoRelatedItem(project, list))
            .createRelatedMergeableLineMarkerInfo(element);
    collection.add(markerInfo);
    return true;
  }
}
