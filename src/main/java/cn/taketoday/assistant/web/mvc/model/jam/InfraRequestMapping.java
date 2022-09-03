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

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamConverter;
import com.intellij.jam.JamEnumAttributeElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.microservices.jvm.url.UrlPathReferenceJamConverter;
import com.intellij.microservices.mime.MimeTypeReference;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiTarget;
import com.intellij.semantic.SemKey;
import com.intellij.semantic.SemRegistrar;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UPolyadicExpression;
import org.jetbrains.uast.UastContextKt;

import java.util.List;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.assistant.web.mvc.presentation.WebMvcPresentationConstant;
import cn.taketoday.lang.Nullable;

import static com.intellij.jam.reflect.JamAttributeMeta.collectionString;

@Presentation(typeName = WebMvcPresentationConstant.REQUEST_MAPPING, icon = "cn.taketoday.assistant.Icons.RequestMapping")
public abstract class InfraRequestMapping<T extends PsiMember> extends JamBaseElement<T> implements RequestMapping<T> {

  private static final JamStringAttributeMeta.Collection<String> VALUE_META = collectionString(
          VALUE_ATTRIBUTE, new UrlPathReferenceJamConverter(InfraMvcUrlPathSpecification.INSTANCE));

  private static final JamEnumAttributeMeta.Collection<RequestMethod> METHOD_META = new JamEnumAttributeMeta.Collection<>(METHOD_ATTRIBUTE, RequestMethod.class);

  private static final JamStringAttributeMeta.Collection<String> PATH_META = collectionString(PATH_ATTRIBUTE, new UrlPathReferenceJamConverter(InfraMvcUrlPathSpecification.INSTANCE));
  private static final JamStringAttributeMeta.Collection<String> CONSUMES_META = collectionString(CONSUMES_ATTRIBUTE, new RequestMappingMimetypeConverter());
  private static final JamStringAttributeMeta.Collection<String> PRODUCES_META = collectionString(PRODUCES_ATTRIBUTE, new RequestMappingMimetypeConverter());
  private static final JamStringAttributeMeta.Collection<String> PARAMS_META = collectionString(PARAMS_ATTRIBUTE);
  private static final JamStringAttributeMeta.Collection<String> HEADERS_META = collectionString(HEADERS_ATTRIBUTE);

  static final JamAnnotationMeta REQUEST_MAPPING_ANNO_META = new JamAnnotationMeta(InfraMvcConstant.REQUEST_MAPPING)
          .addAttribute(VALUE_META)
          .addAttribute(PATH_META)
          .addAttribute(METHOD_META)
          .addAttribute(CONSUMES_META)
          .addAttribute(PRODUCES_META)
          .addAttribute(PARAMS_META)
          .addAttribute(HEADERS_META);

  private final Function<JamStringAttributeElement<String>, PsiTarget> myMapping;

  public InfraRequestMapping(PsiElementRef<?> ref) {
    super(ref);
    this.myMapping = url -> {
      if (!(url.getPsiElement() instanceof PsiLiteral)) {
        return null;
      }
      return new JamPomTarget(this, url);
    };
  }

  private List<String> mapToStringList(JamStringAttributeMeta.Collection<String> attributeMeta) {
    return mapToStringList(REQUEST_MAPPING_ANNO_META.getAttribute(getPsiElement(), attributeMeta));
  }

  static List<String> mapToStringList(List<? extends JamStringAttributeElement<String>> stringAttributeElements) {
    return ContainerUtil.mapNotNull(stringAttributeElements, JamStringAttributeElement::getStringValue);
  }

  @Override
  public List<JamStringAttributeElement<String>> getMappingUrls() {
    PsiElementRef<T> psiElementRef = PsiElementRef.real(getPsiElement());
    List<JamStringAttributeElement<String>> valueElements = REQUEST_MAPPING_ANNO_META.getAttribute(psiElementRef, VALUE_META);
    if (!valueElements.isEmpty()) {
      return valueElements;
    }
    return REQUEST_MAPPING_ANNO_META.getAttribute(psiElementRef, PATH_META);
  }

  @Override
  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return REQUEST_MAPPING_ANNO_META.getAnnotation(getPsiElement());
  }

  @Override
  public RequestMethod[] getMethods() {
    List<RequestMethod> methods = ContainerUtil.mapNotNull(REQUEST_MAPPING_ANNO_META.getAttribute(getPsiElement(), METHOD_META), JamEnumAttributeElement::getValue);
    return methods.toArray(RequestMethod.EMPTY_ARRAY);
  }

  @Override
  public List<String> getUrls() {
    return mapToStringList(getMappingUrls());
  }

  @Override
  public List<String> getConsumes() {
    return mapToStringList(CONSUMES_META);
  }

  @Override
  public List<String> getProduces() {
    return mapToStringList(PRODUCES_META);
  }

  @Override
  public List<String> getParams() {
    return mapToStringList(PARAMS_META);
  }

  @Override
  public List<String> getHeaders() {
    return mapToStringList(HEADERS_META);
  }

  protected List<PsiTarget> getPomTargets() {
    return ContainerUtil.mapNotNull(getMappingUrls(), this.myMapping);
  }

  public static void register(SemRegistrar registrar) {
    ClassMapping.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(InfraMvcConstant.REQUEST_MAPPING));
    MethodMapping.META.register(registrar, PsiJavaPatterns.psiMethod().withAnnotation(InfraMvcConstant.REQUEST_MAPPING));
  }

  public static class ClassMapping extends InfraRequestMapping<PsiClass> {

    private static final SemKey<ClassMapping> JAM_KEY = CLASS_JAM_KEY.subKey("ClassMapping");
    public static final JamClassMeta<ClassMapping> META = new JamClassMeta<>(null, ClassMapping.class, JAM_KEY)
            .addAnnotation(REQUEST_MAPPING_ANNO_META)
            .addChildrenQuery(MODEL_ATTRIBUTE_METHODS_QUERY)
            .addPomTargetProducer((classMapping, pomTargetConsumer) -> {
              for (PsiTarget target : classMapping.getPomTargets()) {
                pomTargetConsumer.consume(target);
              }
            });

    public ClassMapping(PsiElementRef<?> ref) {
      super(ref);
    }

    @Override
    public List<WebMVCModelAttribute> getModelAttributes() {
      return MODEL_ATTRIBUTE_METHODS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }
  }

  public static class MethodMapping extends InfraRequestMapping<PsiMethod> implements Method {
    private static final SemKey<MethodMapping> JAM_KEY = METHOD_JAM_KEY.subKey("MethodMapping");
    public static final JamMethodMeta<MethodMapping> META = new JamMethodMeta<>(null, MethodMapping.class, JAM_KEY)
            .addAnnotation(REQUEST_MAPPING_ANNO_META)
            .addChildrenQuery(MODEL_ATTRIBUTE_PARAMETERS_QUERY)
            .addChildrenQuery(PATH_VARIABLE_PARAMETERS_QUERY)
            .addPomTargetProducer((methodMapping, pomTargetConsumer) -> {
              for (PsiTarget target : methodMapping.getPomTargets()) {
                pomTargetConsumer.consume(target);
              }
            });

    public MethodMapping(PsiElementRef<?> ref) {
      super(ref);
    }

    @Override
    public List<WebMVCModelAttribute> getModelAttributes() {
      return MODEL_ATTRIBUTE_PARAMETERS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }

    @Override
    public List<MVCPathVariable> getPathVariables() {
      return PATH_VARIABLE_PARAMETERS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }

    @Override
    public RequestMethod[] getMethods() {
      return RequestMappingUtil.getAllRequestMethods(this);
    }

    @Override
    public RequestMethod[] getLocalMethods() {
      return super.getMethods();
    }
  }

  private static class RequestMappingMimetypeConverter extends JamConverter<String> {

    @Nullable
    public String fromString(@Nullable String s, JamStringAttributeElement<String> context) {
      return s;
    }

    public PsiReference[] createReferences(JamStringAttributeElement<String> context, PsiLanguageInjectionHost injectionHost) {
      UElement element = UastContextKt.toUElement(injectionHost);
      if (element == null || !(element.getUastParent() instanceof UPolyadicExpression)) {
        return MimeTypeReference.forElement(injectionHost);
      }
      return PsiReference.EMPTY_ARRAY;
    }
  }
}
