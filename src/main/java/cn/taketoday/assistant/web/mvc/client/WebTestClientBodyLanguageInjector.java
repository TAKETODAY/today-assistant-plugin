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

package cn.taketoday.assistant.web.mvc.client;

import com.intellij.jam.JavaLibraryUtils;
import com.intellij.lang.Language;
import com.intellij.lang.injection.general.Injection;
import com.intellij.lang.injection.general.LanguageInjectionContributor;
import com.intellij.lang.injection.general.SimpleInjection;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.expressions.UInjectionHost;

import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.WebMvcFunctionalRoutingConstant;
import cn.taketoday.lang.Nullable;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class WebTestClientBodyLanguageInjector implements LanguageInjectionContributor {

  @Nullable
  public Injection getInjection(PsiElement host) {
    UInjectionHost uInjectionHost;
    Project project = host.getProject();
    if (!project.isDefault() && InfraUtils.hasFacets(project)
            && JavaLibraryUtils.hasLibraryClass(project, WebMvcFunctionalRoutingConstant.WEB_TEST_CLIENT_REQUEST_BODY_SPEC)
            && (uInjectionHost = UastContextKt.toUElement(host, UInjectionHost.class)) != null) {
      UExpression roomExpression = uInjectionHost.getStringRoomExpression();
      UCallExpression uCallExpression = UastUtils.getUCallExpression(roomExpression.getUastParent(), 1);
      if (!isTargetMethod(uCallExpression)) {
        return null;
      }
      UElement uastParent = uCallExpression != null ? uCallExpression.getUastParent() : null;
      if (!(uastParent instanceof UExpression)) {
        uastParent = null;
      }
      UExpression chainExpression = (UExpression) uastParent;
      if (chainExpression == null) {
        return null;
      }

      UCallExpression contentTypeCall = getCallExpression(chainExpression);
      if (contentTypeCall == null) {
        return null;
      }
      UExpression uReferenceExpression = CollectionsKt.firstOrNull(contentTypeCall.getValueArguments());
      if (uReferenceExpression instanceof UReferenceExpression expression) {
        String bodyType = expression.getResolvedName();
        if (Intrinsics.areEqual(bodyType, "APPLICATION_JSON")) {
          Language findLanguageByID = Language.findLanguageByID("JSON");
          return new SimpleInjection(findLanguageByID, "", "", "java");
        }
        else if (!Intrinsics.areEqual(bodyType, "APPLICATION_XML") && !Intrinsics.areEqual(bodyType, "TEXT_XML")) {
          return null;
        }
        else {
          return new SimpleInjection(XMLLanguage.INSTANCE, "", "", "java");
        }
      }
      else if (!(uReferenceExpression instanceof UExpression)) {
        return null;
      }
      else {
        String contentType = UastUtils.evaluateString(uReferenceExpression);
        if (Intrinsics.areEqual(contentType, "application/json")) {
          Language findLanguageByID2 = Language.findLanguageByID("JSON");
          return new SimpleInjection(findLanguageByID2, "", "", "java");
        }
        else if (!Intrinsics.areEqual(contentType, "text/xml") && !Intrinsics.areEqual(contentType, "application/xml")) {
          return null;
        }
        else {
          return new SimpleInjection(XMLLanguage.INSTANCE, "", "", "java");
        }
      }
    }
    return null;
  }

  private static UCallExpression getCallExpression(UExpression chainExpression) {
    for (UExpression expression : UastUtils.getQualifiedChain(chainExpression)) {
      if (expression instanceof UCallExpression callExpression
              && Intrinsics.areEqual(callExpression.getMethodName(), "contentType")) {
        return callExpression;
      }
    }
    return null;
  }

  private boolean isTargetMethod(UCallExpression uCallExpression) {
    String str;
    String str2;
    String methodName = uCallExpression != null ? uCallExpression.getMethodName() : null;
    if (Intrinsics.areEqual(methodName, "bodyValue")) {
      PsiMethod resolve = uCallExpression.resolve();
      if (resolve != null) {
        PsiClass containingClass = resolve.getContainingClass();
        if (containingClass != null) {
          str2 = containingClass.getQualifiedName();
          return Intrinsics.areEqual(str2, WebMvcFunctionalRoutingConstant.WEB_TEST_CLIENT_REQUEST_BODY_SPEC);
        }
      }
      str2 = null;
      return Intrinsics.areEqual(str2, WebMvcFunctionalRoutingConstant.WEB_TEST_CLIENT_REQUEST_BODY_SPEC);
    }
    else if (Intrinsics.areEqual(methodName, "content")) {
      PsiMethod resolve2 = uCallExpression.resolve();
      if (resolve2 != null) {
        PsiClass containingClass2 = resolve2.getContainingClass();
        if (containingClass2 != null) {
          str = containingClass2.getQualifiedName();
          return Intrinsics.areEqual(str, InfraMvcConstant.MOCK_HTTP_SERVLET_REQUEST_BUILDER);
        }
      }
      str = null;
      return Intrinsics.areEqual(str, InfraMvcConstant.MOCK_HTTP_SERVLET_REQUEST_BUILDER);
    }
    else {
      return false;
    }
  }
}
