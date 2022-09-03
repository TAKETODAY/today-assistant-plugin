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

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.highlighting.HighlightedReference;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.microservices.url.MultiPathBestMatcher;
import com.intellij.microservices.url.UrlResolveRequest;
import com.intellij.microservices.url.UrlResolverManager;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.TextRangeUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.mapping.UrlMappingElement;
import cn.taketoday.assistant.web.mvc.model.WebMvcUrlTargetInfo;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.services.WebMvcUtils;
import cn.taketoday.assistant.web.mvc.views.ViewMultiResolverReference;
import cn.taketoday.lang.Nullable;
import kotlin.sequences.SequencesKt;
import one.util.streamex.StreamEx;

public class WebMVCReference extends PsiReferenceBase.Poly<PsiElement> implements EmptyResolveMessageProvider, HighlightedReference, ViewMultiResolverReference {
  private final String myText;
  private final NotNullLazyValue<List<String>> myAllValues;
  private static final ResolveCache.PolyVariantResolver<WebMVCReference> MY_RESOLVER = WebMVCReference::innerResolve;

  public WebMVCReference(PsiElement element, TextRange range, String fullText, boolean soft) {
    super(element, range, soft);
    this.myText = fullText;
    this.myAllValues = NotNullLazyValue.lazy(() -> {
      return firRanges(this.myText, "/", true).flatMap(slashRange -> {
                return firRanges(slashRange.subSequence(this.myText), ".", false).map(textRange -> {
                  return textRange.shiftRight(slashRange.getStartOffset());
                });
              })
              .map(it -> it.substring(this.myText))
              .toList();
    });
  }

  private static StreamEx<TextRange> firRanges(CharSequence text, String delimiter, boolean forward) {
    List<TextRange> split = SequencesKt.toList(TextRangeUtils.splitToTextRanges(text, delimiter, false));
    if (split.size() == 0) {
      return StreamEx.empty();
    }
    TextRange anchor = forward ? split.get(split.size() - 1) : split.get(0);
    return StreamEx.of(split).map(range -> {
      if (forward) {
        return TextRange.create(range.getStartOffset(), anchor.getEndOffset());
      }
      return TextRange.create(anchor.getStartOffset(), range.getEndOffset());
    });
  }

  private StreamEx<ServletMappingInfo> getServletMappingInfos() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) {
      return StreamEx.empty();
    }
    return StreamEx.of(WebMvcUtils.getServletMappingInfos(module));
  }

  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    ServletMappingInfo myMappingInfo = selectServletMapping().nonNull().findAny().orElse(null);
    if (myMappingInfo == null) {
      return super.handleElementRename(newElementName);
    }
    String s = myMappingInfo.stripMapping(newElementName);
    return super.handleElementRename(s);
  }

  private StreamEx<ServletMappingInfo> selectServletMapping() {
    return getServletMappingInfos()
            .filter(info -> ContainerUtil.or(this.myAllValues.getValue(), info::matches));
  }

  @Override
  public boolean hasResolvers() {
    return selectServletMapping().findAny().isPresent();
  }

  public ResolveResult[] multiResolve(boolean incompleteCode) {
    return ResolveCache.getInstance(this.myElement.getProject())
            .resolveWithCaching(this, MY_RESOLVER, false, incompleteCode);
  }

  public ResolveResult[] innerResolve(boolean incompleteCode) {
    Set<UrlTargetInfo> allResolvedTargets = StreamEx.of(this.myAllValues.getValue())
            .map(value -> {
              return InfraMvcUrlPathSpecification.INSTANCE.getParser().parseUrlPath(new PartiallyKnownString(value)).getUrlPath();
            })
            .foldLeft(new MultiPathBestMatcher(), (matcher, urlPath) -> {
              Iterable<UrlTargetInfo> resolved = UrlResolverManager.getInstance(getElement().getProject())
                      .resolve(new UrlResolveRequest(null, null, urlPath, "GET"));
              return matcher.addBestMatching(resolved, urlPath);
            })
            .getResult();
    Set<PsiElement> allResolvedPsiElements = StreamEx.of(allResolvedTargets).map(this::extractPsiElement).nonNull().toImmutableSet();
    return PsiElementResolveResult.createResults(allResolvedPsiElements);
  }

  @Nullable
  private PsiElement extractPsiElement(UrlTargetInfo it) {
    if (it instanceof WebMvcUrlTargetInfo) {
      UrlMappingElement urlMapping = ((WebMvcUrlTargetInfo) it).getUrlMapping();
      PomNamedTarget pomTarget = urlMapping.getPomTarget();
      if (pomTarget != null) {
        return PomService.convertToPsi(getElement().getProject(), pomTarget);
      }
      return null;
    }
    return it.resolveToPsiElement();
  }

  public Object[] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) {
      return EMPTY_ARRAY;
    }
    Set<ServletMappingInfo> mappingInfos = WebMvcUtils.getServletMappingInfos(module);
    Object[] map2Array = ContainerUtil.map2Array(WebMvcUtils.getUrlMappings(module), LookupElement.class, variant -> {
      LookupElementBuilder builder;
      String lookupString = "/" + StreamEx.of(variant.getUrlPath().getSegments()).map(e -> {
        return e.getValueIfExact();
      }).takeWhileInclusive((v0) -> {
        return Objects.nonNull(v0);
      }).map(StringUtil::notNullize).joining("/");
      Iterator it = mappingInfos.iterator();
      while (true) {
        if (!it.hasNext()) {
          break;
        }
        ServletMappingInfo mappingInfo = (ServletMappingInfo) it.next();
        if (!mappingInfo.matches(lookupString)) {
          lookupString = mappingInfo.addMapping(lookupString);
          break;
        }
      }
      PsiElement resolveElement = variant.getNavigationTarget();
      if (resolveElement != null) {
        builder = LookupElementBuilder.create(resolveElement, lookupString).withTypeText(FileUtilRt.getNameWithoutExtension(resolveElement.getContainingFile().getName()));
      }
      else {
        builder = LookupElementBuilder.create(lookupString);
      }
      LookupElementBuilder builder2 = builder.withIcon(Icons.RequestMapping);
      if (variant.getMethod().length > 0) {
        builder2 = builder2.withTailText(" " + RequestMethod.getDisplay(variant.getMethod()), true);
      }
      return builder2.withPresentableText("/" + variant.getPresentation());
    });
    return map2Array;
  }

  public String getUnresolvedMessagePattern() {
    return InfraAppBundle.message("cannot.resolve.controller.url");
  }

  public String toString() {
    return "WebMVCReference{myElement=" + this.myElement.getText() + "range=" + getRangeInElement() + "}";
  }
}
