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
import com.intellij.microservices.url.Authority;
import com.intellij.microservices.url.FrameworkUrlPathSpecification;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlResolveRequest;
import com.intellij.microservices.url.references.UrlExtractors;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.microservices.url.references.UrlPksParser;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.web.mvc.InfraControllerUtils;
import cn.taketoday.assistant.web.mvc.jam.InfraRequestBaseUrlElement;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.model.WebUrlPathSpecificationProvider;
import cn.taketoday.assistant.web.mvc.utils.WebMvcUrlUtils;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public final class InfraMvcUrlPathSpecification extends FrameworkUrlPathSpecification {

  public static final UrlPksParser parser;

  public static final InfraMvcUrlPathSpecification INSTANCE = new InfraMvcUrlPathSpecification();

  static {
    UrlPksParser urlPksParser = new UrlPksParser(UrlExtractors::springLikePropertySplitEscaper, new Function1<String, UrlPath.PathSegment>() {
      @Override
      public UrlPath.PathSegment invoke(String s) {
        return new UrlPath.PathSegment.Exact(s);
      }
    }, false);
    urlPksParser.setSplitEscaper(UrlExtractors::springLikePropertySplitEscaper);
    urlPksParser.setShouldHaveScheme(false);
    urlPksParser.setParseQueryParameters(false);
    parser = urlPksParser;
  }

  public UrlPathContext getUrlPathContext(PsiElement declaration) {
    List<String> topLevelUrls;
    PsiClass it = PsiTreeUtil.getParentOfType(declaration, PsiClass.class, false);
    boolean isController = it != null && InfraControllerUtils.isController(it);
    JamService jamService = JamService.getJamService(declaration.getProject());
    Module module = ModuleUtil.findModuleForPsiElement(declaration);
    List<String> appPaths = WebMvcUrlUtils.getApplicationPaths(module);
    RequestMapping.Method methodJam = jamService.getJamElement(RequestMapping.METHOD_JAM_KEY, declaration);
    if (methodJam == null) {
      RequestMapping classJam = jamService.getJamElement(RequestMapping.CLASS_JAM_KEY, declaration);
      if (classJam == null) {
        if (appPaths.isEmpty()) {
          return UrlPathContext.Companion.emptyRoot();
        }
        UrlPathContext baseContext = getBaseContext(declaration);
        ArrayList<UrlPath> pathContext = new ArrayList<>(Math.max(appPaths.size(), 10));
        for (String p1 : appPaths) {
          pathContext.add(parsePath(p1));
        }
        return baseContext.subContexts(pathContext);
      }
      UrlPathContext applyOnResolve = getBaseContext(declaration).withDeclarationFlag(isController)
              .applyOnResolve(new Function1<UrlPathContext, UrlPathContext>() {
                @Override
                public UrlPathContext invoke(UrlPathContext pathContext) {
                  Set authorities = INSTANCE.getAuthorities(declaration);
                  return INSTANCE.withAuthoritiesIfDeclaration(pathContext, authorities);
                }
              });

      ArrayList<UrlPath> urlPaths = new ArrayList<>(Math.max(appPaths.size(), 10));
      for (String p12 : appPaths) {
        urlPaths.add(parsePath(p12));
      }
      UrlPathContext subContexts = applyOnResolve.subContexts(urlPaths);
      List<String> urls = classJam.getUrls();
      ArrayList<UrlPath> arrayList = new ArrayList<>(Math.max(urls.size(), 10));
      for (String url : urls) {
        arrayList.add(parsePath(url));
      }
      return subContexts.subContexts(arrayList);
    }
    RequestMapping<PsiClass> classLevelMapping = methodJam.getClassLevelMapping();
    if (classLevelMapping != null) {
      topLevelUrls = classLevelMapping.getUrls();
    }
    else
      topLevelUrls = CollectionsKt.listOf("");
    UrlPathContext applyOnResolve2 = getBaseContext(declaration)
            .withDeclarationFlag(isController)
            .applyOnResolve(new Function1<UrlPathContext, UrlPathContext>() {
              @Override
              public UrlPathContext invoke(UrlPathContext pathContext) {
                Set authorities = INSTANCE.getAuthorities(declaration);
                return INSTANCE.withAuthoritiesIfDeclaration(pathContext, authorities);
              }
            });
    RequestMethod[] methods = methodJam.getMethods();

    ArrayList<String> collection = new ArrayList<>(methods.length);
    for (RequestMethod requestMethod : methods) {
      collection.add(requestMethod.name());
    }
    UrlPathContext withMethods = applyOnResolve2.withMethods(collection);
    ArrayList<UrlPath> objects = new ArrayList<>(Math.max(appPaths.size(), 10));
    for (String path : appPaths) {
      objects.add(parsePath(path));
    }
    UrlPathContext subContexts2 = withMethods.subContexts(objects);
    ArrayList<UrlPath> arrayList = new ArrayList<>(Math.max(topLevelUrls.size(), 10));
    for (String p15 : topLevelUrls) {
      arrayList.add(parsePath(p15));
    }
    UrlPathContext subContexts3 = subContexts2.subContexts(arrayList);
    List<String> urls2 = methodJam.getUrls();
    Collection destination$iv$iv7 = new ArrayList<>(Math.max(urls2.size(), 10));
    for (Object item$iv$iv6 : urls2) {
      String p16 = (String) item$iv$iv6;
      destination$iv$iv7.add(parsePath(p16));
    }
    return subContexts3.subContexts((List) destination$iv$iv7);
  }

  private UrlPathContext getBaseContext(PsiElement declaration) {
    List it = getBaseUrls(declaration);
    return it != null ? new UrlPathContext(it) : UrlPathContext.Companion.supportingSchemes(UrlConstants.HTTP_SCHEMES, null);
  }

  private List<UrlResolveRequest> getBaseUrls(PsiElement declaration) {
    PsiElement psiElement;
    PartiallyKnownString pks;
    String str;
    String str2;
    String it;
    if (declaration == null || (psiElement = PsiTreeUtil.getParentOfType(declaration, PsiClass.class, false)) == null) {
      return null;
    }
    InfraRequestBaseUrlElement jamElement = JamService.getJamService(psiElement.getProject()).getJamElement(InfraRequestBaseUrlElement.Companion.getJAM_ELEMENT_KEY(), psiElement);
    if (jamElement == null || (pks = jamElement.getUrl()) == null) {
      return null;
    }
    UrlPksParser.ParsedPksUrl parseFullUrl = parser.parseFullUrl(pks);
    PartiallyKnownString authority = parseFullUrl.getAuthority();
    String authorityHint = authority != null ? authority.getValueIfKnown() : null;
    PartiallyKnownString scheme = parseFullUrl.getScheme();
    if (scheme == null || (it = scheme.getValueIfKnown()) == null) {
      str = null;
    }
    else {
      str = it.length() > 0 ? it : null;
    }
    String schemeHint = str;
    String str3 = !Intrinsics.areEqual(schemeHint, authorityHint) ? schemeHint : null;
    if (authorityHint != null) {
      str3 = str3;
      str2 = authorityHint.length() > 0 ? authorityHint : null;
    }
    else {
      str2 = null;
    }
    UrlResolveRequest request = new UrlResolveRequest(str3, str2, parseFullUrl.getUrlPath(), null);
    return new SmartList<>(request);
  }

  public UrlPksParser getParser() {
    return parser;
  }

  public static final class Provider implements WebUrlPathSpecificationProvider {
    @Override
    public FrameworkUrlPathSpecification getFrameworkUrlPathSpecification() {
      return INSTANCE;
    }
  }

  public Set<String> getAuthorities(PsiElement element) {
    Module module = ModuleUtil.findModuleForPsiElement(element);
    if (module != null) {
      List<Authority> authoritiesByModule = WebMvcUrlUtils.getAuthoritiesByModule(module);
      ArrayList<Authority.Exact> arrayList = new ArrayList();
      for (Authority auth : authoritiesByModule) {
        if (auth instanceof Authority.Exact authority) {
          arrayList.add(authority);
        }
      }
      LinkedHashSet<String> list = new LinkedHashSet<>();
      for (Authority.Exact it : arrayList) {
        list.add(it.getText());
      }
      return list;
    }
    return SetsKt.emptySet();
  }

  public UrlPathContext withAuthoritiesIfDeclaration(UrlPathContext pathContext, Set<String> set) {
    return pathContext.isDeclaration()
           ? pathContext.withAuthorities(set) : pathContext;
  }
}


