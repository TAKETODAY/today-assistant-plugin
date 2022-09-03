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

import com.intellij.codeInsight.documentation.QuickDocUtil;
import com.intellij.codeInsight.javadoc.JavaDocInfoGeneratorFactory;
import com.intellij.codeInsight.javadoc.JavaDocUtil;
import com.intellij.ide.TypePresentationService;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamService;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.microservices.url.references.UrlPathReference;
import com.intellij.microservices.url.references.UrlPathReferenceUnifiedPomTarget;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.ObjectUtils;

import org.jetbrains.annotations.Nls;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastUtils;

import java.util.Iterator;
import java.util.List;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlTargetInfo;
import cn.taketoday.assistant.web.mvc.model.jam.MVCPathVariable;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.assistant.web.mvc.model.jam.WebMVCModelAttribute;
import cn.taketoday.lang.Nullable;

public class WebMvcRequestMappingDocumentationProvider extends AbstractDocumentationProvider {

  @Nls
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if (!InfraLibraryUtil.hasWebMvcLibrary(element.getProject())) {
      return null;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module != null && !InfraLibraryUtil.hasWebMvcLibrary(module)) {
      return null;
    }
    PsiElement refPsi = ObjectUtils.doIfNotNull(UrlPathReference.getFromPomTargetPsiElement(element), PsiReferenceBase::getElement);
    if (refPsi != null) {
      element = refPsi;
    }
    UDeclaration containing = UastUtils.findContaining(element, UDeclaration.class);
    if (containing == null) {
      return null;
    }
    RequestMapping<?> requestMapping = null;
    if (containing instanceof UMethod) {
      requestMapping = ObjectUtils.doIfNotNull(containing.getJavaPsi(), javaPsi -> {
        return JamService.getJamService(javaPsi.getProject())
                .getJamElement(RequestMapping.METHOD_JAM_KEY, javaPsi);
      });
    }
    else if (containing instanceof UClass) {
      requestMapping = ObjectUtils.doIfNotNull(containing.getJavaPsi(), javaPsi2 -> {
        return JamService.getJamService(javaPsi2.getProject())
                .getJamElement(RequestMapping.CLASS_JAM_KEY, javaPsi2);
      });
    }
    if (requestMapping != null) {
      return QuickDocUtil.inferLinkFromFullDocumentation(this, element, originalElement, getQuickNavigateInfoForRequestMapping(requestMapping));
    }
    return null;
  }

  @Nullable
  private static String getQuickNavigateInfoInner(PsiElement element) {
    RequestMapping<?> requestMapping = getRequestMapping(element);
    if (requestMapping == null) {
      return null;
    }
    return getQuickNavigateInfoForRequestMapping(requestMapping);
  }

  private static String getQuickNavigateInfoForRequestMapping(RequestMapping<?> requestMapping) {
    StringBuilder sb = new StringBuilder("<html>");
    appendElementTypeURLs(sb, requestMapping);
    RequestMethod[] requestMethods = requestMapping.getMethods();
    if (requestMethods.length != 0) {
      sb.append(" ");
      sb.append(RequestMethod.getDisplay(requestMethods));
    }
    PsiClass containingClass = requestMapping.getPsiElement().getContainingClass();
    if (containingClass != null) {
      sb.append("<br/>");
      sb.append(containingClass.getQualifiedName());
    }
    sb.append("</html>");
    return sb.toString();
  }

  @Nls
  @Nullable
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    RequestMapping<?> requestMapping;
    if (!InfraLibraryUtil.hasWebMvcLibrary(element.getProject())) {
      return null;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if ((module != null && !InfraLibraryUtil.hasWebMvcLibrary(module)) || (requestMapping = getRequestMapping(element)) == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder("<div class='definition'><pre>");
    appendElementTypeURLs(sb, requestMapping);
    addMethodLink(sb, requestMapping);
    sb.append("</pre></div>");
    sb.append("<table class='sections'>");
    addHeadersParams(sb, requestMapping);
    addConsumesProduces(sb, requestMapping);
    addRequestMethod(sb, requestMapping);
    addModelAttributes(sb, requestMapping);
    addPathVariables(sb, requestMapping);
    sb.append("</table>");
    return sb.toString();
  }

  @Nullable
  private static RequestMapping<?> getRequestMapping(PsiElement element) {
    UrlPathReferenceUnifiedPomTarget urlPomTarget = UrlPathReferenceUnifiedPomTarget.getFromPomTargetPsiElement(element);
    if (urlPomTarget != null) {
      for (UrlTargetInfo target : urlPomTarget.getResolvedTargets()) {
        if (target instanceof WebMvcUrlTargetInfo) {
          PomTarget pomTarget = ((WebMvcUrlTargetInfo) target).getUrlMapping().getPomTarget();
          if (pomTarget instanceof JamPomTarget jamPomTarget) {
            JamElement jamElement = jamPomTarget.getJamElement();
            if (jamElement instanceof RequestMapping) {
              return (RequestMapping) jamElement;
            }
          }
        }
      }
    }
    Object modelObject = JamCommonUtil.getModelObject(element);
    if (!(modelObject instanceof RequestMapping)) {
      return null;
    }
    return (RequestMapping) modelObject;
  }

  private static void appendElementTypeURLs(StringBuilder sb, RequestMapping<?> requestMapping) {
    sb.append(TypePresentationService.getService().getTypeName(requestMapping));
    sb.append(" <b>");
    StringUtil.join(requestMapping.getUrls(), ", ", sb);
    sb.append("</b>");
  }

  private static void appendSection(StringBuilder sb, @Nls String sectionName, String sectionContent) {
    sb.append("<tr><td valign='top' class='section'><p>").append(sectionName).append(":").append("</td><td valign='top'>");
    sb.append(sectionContent);
    sb.append("</td>");
  }

  private static void addHeadersParams(StringBuilder sb, RequestMapping<?> mapping) {
    List<String> headers = mapping.getHeaders();
    if (!headers.isEmpty()) {
      appendSection(sb, InfraAppBundle.message("documentation.provider.headers"), StringUtil.join(headers, ", "));
    }
    List<String> params = mapping.getParams();
    if (!params.isEmpty()) {
      appendSection(sb, InfraAppBundle.message("documentation.provider.params"), StringUtil.join(params, ", "));
    }
  }

  private static void addConsumesProduces(StringBuilder sb, RequestMapping<?> mapping) {
    List<String> consumes = mapping.getConsumes();
    if (!consumes.isEmpty()) {
      StringBuilder consumesSb = new StringBuilder();
      Iterator<String> it = consumes.iterator();
      while (it.hasNext()) {
        String consume = it.next();
        if (StringUtil.startsWithChar(consume, '!')) {
          consumesSb.append("<strike>").append(consume).append("</strike>");
        }
        else {
          consumesSb.append(consume);
        }
        if (it.hasNext()) {
          consumesSb.append(", ");
        }
      }
      appendSection(sb, InfraAppBundle.message("documentation.provider.consumes"), consumesSb.toString());
    }
    List<String> produces = mapping.getProduces();
    if (!produces.isEmpty()) {
      appendSection(sb, InfraAppBundle.message("documentation.provider.produces"), StringUtil.join(produces, ", "));
    }
  }

  private static void addRequestMethod(StringBuilder sb, RequestMapping<?> requestMapping) {
    RequestMethod[] requestMethods = requestMapping.getMethods();
    if (requestMethods.length != 0) {
      appendSection(sb, InfraAppBundle.message("documentation.provider.methods"), RequestMethod.getDisplay(requestMethods));
    }
  }

  private static void addModelAttributes(StringBuilder sb, RequestMapping<?> requestMapping) {
    List<WebMVCModelAttribute> modelAttributes = requestMapping.getModelAttributes();
    if (!modelAttributes.isEmpty()) {
      StringBuilder modelSb = new StringBuilder();
      Iterator<WebMVCModelAttribute> it = modelAttributes.iterator();
      while (it.hasNext()) {
        WebMVCModelAttribute attribute = it.next();
        modelSb.append(attribute.getName());
        modelSb.append(":");
        PsiMember psiElement = requestMapping.getPsiElement();
        JavaDocInfoGeneratorFactory.create(psiElement.getProject(), null).generateType(modelSb, attribute.getType(), psiElement, true);
        if (it.hasNext()) {
          modelSb.append(", ");
        }
      }
      appendSection(sb, InfraAppBundle.message("documentation.provider.model.attributes"), modelSb.toString());
    }
  }

  private static void addPathVariables(StringBuilder sb, RequestMapping<?> mapping) {
    if (!(mapping instanceof RequestMapping.Method methodMapping)) {
      return;
    }
    List<MVCPathVariable> pathVariables = methodMapping.getPathVariables();
    if (!pathVariables.isEmpty()) {
      StringBuilder varSb = new StringBuilder();
      Iterator<MVCPathVariable> it = pathVariables.iterator();
      while (it.hasNext()) {
        MVCPathVariable pathVariable = it.next();
        varSb.append(pathVariable.getName());
        varSb.append(":");
        PsiMethod element = methodMapping.getPsiElement();
        JavaDocInfoGeneratorFactory.create(element.getProject(), null).generateType(varSb, pathVariable.getType(), element, true);
        if (it.hasNext()) {
          varSb.append(", ");
        }
      }
      appendSection(sb, InfraAppBundle.message("documentation.provider.path.variables"), varSb.toString());
    }
  }

  private static void addMethodLink(StringBuilder sb, RequestMapping<?> requestMapping) {
    if (requestMapping instanceof RequestMapping.Method) {
      PsiMember requestMappingPsiElement = requestMapping.getPsiElement();
      String methodName = PsiFormatUtil.formatMethod((PsiMethod) requestMappingPsiElement, PsiSubstitutor.EMPTY, 4103, 2);
      sb.append("<br><a href=\"psi_element://");
      sb.append(JavaDocUtil.getReferenceText(requestMappingPsiElement.getProject(), requestMappingPsiElement));
      sb.append("\">");
      sb.append(methodName);
      sb.append("</a>");
    }
  }
}
