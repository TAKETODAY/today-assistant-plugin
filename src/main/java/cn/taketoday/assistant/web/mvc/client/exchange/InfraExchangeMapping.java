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

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationArchetype;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.url.AnnotationValueHintInfo;
import com.intellij.microservices.jvm.url.AnnotationValueUrlPathInlayProvider;
import com.intellij.microservices.jvm.url.UrlPathReferenceJamConverter;
import com.intellij.microservices.url.inlay.UrlPathInlayHintsProviderSemElement;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PartiallyKnownString;
import com.intellij.semantic.SemKey;

import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.expressions.UInjectionHost;
import org.jetbrains.uast.expressions.UStringConcatenationsFacade;

import java.util.Collection;
import java.util.List;

import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import cn.taketoday.lang.Nullable;

public abstract class InfraExchangeMapping<T extends PsiMember> extends JamBaseElement<T> implements AnnotationValueUrlPathInlayProvider {
  protected static final JamStringAttributeMeta.Single<String> VALUE_META = JamAttributeMeta.singleString(RequestMapping.VALUE_ATTRIBUTE, new UrlPathReferenceJamConverter(
          InfraExchangeUrlPathSpecification.INSTANCE, true));
  protected static final JamStringAttributeMeta.Single<String> URL_META = JamAttributeMeta.singleString("url", new UrlPathReferenceJamConverter(InfraExchangeUrlPathSpecification.INSTANCE, true));
  private static final JamStringAttributeMeta.Single<String> CONTENT_TYPE_META = JamAttributeMeta.singleString("contentType", new ContentTypeJamConverter());
  private static final JamStringAttributeMeta.Collection<String> ACCEPT_META = JamAttributeMeta.collectionString("accept", new ContentTypeJamConverter());
  protected static final JamAnnotationArchetype STANDARD_METHOD_ARCHETYPE = new JamAnnotationArchetype().addAttribute(VALUE_META).addAttribute(URL_META).addAttribute(CONTENT_TYPE_META)
          .addAttribute(ACCEPT_META);
  public static final SemKey<InfraExchangeMapping<?>> MAPPING_JAM_KEY = JamService.JAM_ELEMENT_KEY.subKey("InfraExchangeMapping",
          new SemKey[] { UrlPathInlayHintsProviderSemElement.INLAY_HINT_SEM_KEY });

  @Nullable
  public abstract String getHttpMethod();

  protected abstract JamAnnotationMeta getAnnotationMeta();

  protected InfraExchangeMapping(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public String getResourceValue() {
    String value = (String) ((JamStringAttributeElement) getAnnotationMeta().getAttribute(getPsiElementRef(), VALUE_META)).getValue();
    if (value != null) {
      return value;
    }
    return (String) ((JamStringAttributeElement) getAnnotationMeta().getAttribute(getPsiElementRef(), URL_META)).getValue();
  }

  public List<JamStringAttributeElement<String>> getUrls() {
    return List.of(getAnnotationMeta().getAttribute(getPsiElementRef(), VALUE_META),
            getAnnotationMeta().getAttribute(getPsiElementRef(), URL_META));
  }

  public Collection<AnnotationValueHintInfo> getAnnotationValueHintInfos() {
    UrlPathContext urlPathContext = InfraExchangeUrlPathSpecification.INSTANCE.getUrlPathContext(getPsiElement());
    JamAnnotationMeta annotationMeta = getAnnotationMeta();
    return List.of(
            new AnnotationValueHintInfo(annotationMeta.getAttribute(getPsiElementRef(), VALUE_META), urlPathContext),
            new AnnotationValueHintInfo(annotationMeta.getAttribute(getPsiElementRef(), URL_META), urlPathContext)
    );
  }

  @Nullable
  public PartiallyKnownString getUrl() {
    PsiAnnotationMemberValue valueAttr = getAnnotationMeta().getAttribute(getPsiElementRef(), VALUE_META).getPsiElement();
    PsiAnnotationMemberValue urlAttr = getAnnotationMeta().getAttribute(getPsiElementRef(), URL_META).getPsiElement();
    PsiAnnotationMemberValue targetAttr = valueAttr == null ? urlAttr : valueAttr;
    return asPartiallyKnownString(targetAttr);
  }

  @Nullable
  protected static PartiallyKnownString asPartiallyKnownString(@Nullable PsiAnnotationMemberValue attributeValue) {
    UStringConcatenationsFacade facade;
    UInjectionHost uInjectionHost = UastContextKt.toUElement(attributeValue, UInjectionHost.class);
    if (uInjectionHost == null || (facade = UStringConcatenationsFacade.createFromUExpression(uInjectionHost)) == null) {
      return null;
    }
    return facade.asPartiallyKnownString();
  }
}
