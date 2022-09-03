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

package cn.taketoday.assistant.web.mvc.client.exchange;

import com.intellij.jam.JamService;
import com.intellij.microservices.url.FrameworkUrlPathSpecification;
import com.intellij.microservices.url.UrlConstants;
import com.intellij.microservices.url.UrlConversionConstants;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlResolveRequest;
import com.intellij.microservices.url.UrlSpecialSegmentMarker;
import com.intellij.microservices.url.references.UrlExtractors;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.microservices.url.references.UrlPksParser;
import com.intellij.microservices.utils.PlaceholderSplitEscaper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.SplitEscaper;
import com.intellij.util.SmartList;

import java.util.List;

import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;

public final class InfraExchangeUrlPathSpecification extends FrameworkUrlPathSpecification {

  public static final InfraExchangeUrlPathSpecification INSTANCE = new InfraExchangeUrlPathSpecification();
  private static final List<UrlSpecialSegmentMarker> PATH_PLACEHOLDER_BRACES = List.of(
          UrlConversionConstants.SPRING_LIKE_PATH_VARIABLE_BRACES,
          UrlConversionConstants.SPRING_LIKE_PLACEHOLDER_BRACES
  );

  private static final UrlPksParser parser = new UrlPksParser(
          new Function2<>() {
            @Override
            public final SplitEscaper invoke(CharSequence input, String pattern) {
              return new PlaceholderSplitEscaper(PATH_PLACEHOLDER_BRACES, input, pattern);
            }
          },
          segmentStr -> {
            Intrinsics.checkNotNullParameter(segmentStr, "segmentStr");
            UrlPath.PathSegment extractPlaceholderAsUndefined = UrlExtractors.extractPlaceholderAsUndefined(segmentStr, UrlConversionConstants.SPRING_LIKE_PLACEHOLDER_BRACES);
            if (extractPlaceholderAsUndefined == null) {
              extractPlaceholderAsUndefined = UrlExtractors.extractPathVariable(segmentStr, UrlConversionConstants.SPRING_LIKE_PATH_VARIABLE_BRACES);
            }
            return extractPlaceholderAsUndefined != null ? extractPlaceholderAsUndefined : new UrlPath.PathSegment.Exact(segmentStr);
          }, false);

  public UrlPathContext getUrlPathContext(PsiElement declaration) {
    JamService jamService = JamService.getJamService(declaration.getProject());
    HttpExchange.ClassMapping httpExchange = jamService.getJamElement(HttpExchange.CLASS_JAM_KEY, declaration);
    if (httpExchange != null) {
      return getBaseContext(declaration).withDeclarationFlag(false);
    }
    InfraExchangeMapping exchangeMethod = jamService.getJamElement(InfraExchangeMapping.MAPPING_JAM_KEY, declaration);
    if (exchangeMethod != null) {
      return getBaseContext(declaration).subContext(parsePath(exchangeMethod.getResourceValue())).withMethod(exchangeMethod.getHttpMethod()).withDeclarationFlag(false);
    }
    return UrlPathContext.Companion.emptyRoot();
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
    HttpExchange.ClassMapping jamElement = JamService.getJamService(psiElement.getProject()).getJamElement(HttpExchange.CLASS_JAM_KEY, psiElement);
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
}
