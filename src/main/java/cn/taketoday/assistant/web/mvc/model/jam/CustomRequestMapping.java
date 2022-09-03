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
import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamEnumAttributeElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamEnumAttributeMeta;
import com.intellij.jam.reflect.JamMemberMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import cn.taketoday.assistant.AliasForUtils;
import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAliasFor;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.jam.RequestMethod;
import cn.taketoday.lang.Nullable;

@Presentation(provider = CustomRequestMapping.CustomRequestPresentationProvider.class)
public abstract class CustomRequestMapping<T extends PsiMember> extends JamBaseElement<T> implements RequestMapping<T> {
  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;
  private final JamAnnotationMeta myAnnotationMeta;
  private final NullableLazyValue<InfraRequestMapping.ClassMapping> myDefiningMetaAnnotation;
  private static final Map<String, JamAnnotationMeta> annotationMetaMap = new HashMap();
  private final Function<JamStringAttributeElement<String>, PsiTarget> myPsiTargetMapping;

  CustomRequestMapping(String anno, T psiElement) {
    super(PsiElementRef.real(psiElement));
    this.myDefiningMetaAnnotation = new NullableLazyValue<InfraRequestMapping.ClassMapping>() {
      @Nullable
      public InfraRequestMapping.ClassMapping compute() {
        PsiClass annotationType;
        PsiAnnotation definingMetaAnnotation = AliasForUtils.findDefiningMetaAnnotation(
                getPsiElement(), myAnnotationMeta.getAnnoName(),
                InfraMvcConstant.REQUEST_MAPPING);
        if (definingMetaAnnotation == null || (annotationType = PsiTreeUtil.getParentOfType(definingMetaAnnotation, PsiClass.class, true)) == null) {
          return null;
        }
        return InfraRequestMapping.ClassMapping.META.getJamElement(annotationType);
      }
    };
    this.myPsiTargetMapping = url -> {
      if (!(url.getPsiElement() instanceof PsiLiteral)) {
        return null;
      }
      return new JamPomTarget(this, url);
    };
    this.myAnnotationMeta = getMeta(anno);
    this.myPsiAnnotation = this.myAnnotationMeta.getAnnotationRef(getPsiElement());
  }

  private static synchronized JamAnnotationMeta getMeta(String anno) {
    JamAnnotationMeta meta = annotationMetaMap.get(anno);
    if (meta == null) {
      meta = new JamAnnotationMeta(anno);
      annotationMetaMap.put(anno, meta);
    }
    return meta;
  }

  @Override
  public List<JamStringAttributeElement<String>> getMappingUrls() {
    InfraAliasFor valueAliasFor = findAliasFor(RequestMapping.VALUE_ATTRIBUTE);
    InfraAliasFor pathAliasFor = findAliasFor("path");
    if (valueAliasFor == null && pathAliasFor == null) {
      InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
      return definingAnnotation != null ? definingAnnotation.getMappingUrls() : Collections.emptyList();
    }
    List<JamStringAttributeElement<String>> valueElements = getCollectionStringAttributeElements(valueAliasFor);
    List<JamStringAttributeElement<String>> pathElements = getCollectionStringAttributeElements(pathAliasFor);
    return ContainerUtil.concat(valueElements, pathElements);
  }

  private List<JamStringAttributeElement<String>> getCollectionStringAttributeElements(InfraAliasFor aliasFor) {
    return aliasFor == null ? Collections.emptyList() : JamAttributeMeta.collectionString(aliasFor.getMethodName()).getJam(this.myPsiAnnotation);
  }

  @Override
  public List<String> getUrls() {
    List<JamStringAttributeElement<String>> urlElements = getMappingUrls();
    List<String> urls = InfraRequestMapping.mapToStringList(urlElements);
    if (!urls.isEmpty()) {
      return urls;
    }
    InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
    return definingAnnotation != null ? definingAnnotation.getUrls() : Collections.emptyList();
  }

  @Override
  public RequestMethod[] getMethods() {
    InfraAliasFor aliasFor = findAliasFor("method");
    if (aliasFor == null) {
      InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
      return definingAnnotation != null ? definingAnnotation.getMethods() : RequestMethod.EMPTY_ARRAY;
    }
    var attributeMeta = new JamEnumAttributeMeta.Collection<>(aliasFor.getMethodName(), RequestMethod.class);
    List<RequestMethod> methods = ContainerUtil.mapNotNull(attributeMeta.getJam(this.myPsiAnnotation), JamEnumAttributeElement::getValue);
    return methods.toArray(RequestMethod.EMPTY_ARRAY);
  }

  @Override
  public List<String> getConsumes() {
    List<String> values = getStringValues(RequestMapping.CONSUMES_ATTRIBUTE);
    if (values != null) {
      return values;
    }
    InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
    return definingAnnotation != null ? definingAnnotation.getConsumes() : Collections.emptyList();
  }

  @Override
  public List<String> getProduces() {
    List<String> values = getStringValues(RequestMapping.PRODUCES_ATTRIBUTE);
    if (values != null) {
      return values;
    }
    InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
    return definingAnnotation != null ? definingAnnotation.getProduces() : Collections.emptyList();
  }

  @Override
  public List<String> getParams() {
    List<String> values = getStringValues(RequestMapping.PARAMS_ATTRIBUTE);
    if (values != null) {
      return values;
    }
    InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
    return definingAnnotation != null ? definingAnnotation.getParams() : Collections.emptyList();
  }

  @Override
  public List<String> getHeaders() {
    List<String> values = getStringValues(RequestMapping.HEADERS_ATTRIBUTE);
    if (values != null) {
      return values;
    }
    InfraRequestMapping.ClassMapping definingAnnotation = getDefiningAnnotation();
    return definingAnnotation != null ? definingAnnotation.getHeaders() : Collections.emptyList();
  }

  @Override
  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return this.myPsiAnnotation.getPsiElement();
  }

  @Nullable
  protected List<String> getStringValues(String attributeName) {
    InfraAliasFor aliasFor = findAliasFor(attributeName);
    if (aliasFor == null) {
      return null;
    }
    JamStringAttributeMeta.Collection<String> stringAttributeMeta = JamAttributeMeta.collectionString(aliasFor.getMethodName());
    return InfraRequestMapping.mapToStringList(stringAttributeMeta.getJam(this.myPsiAnnotation));
  }

  @Nullable
  private InfraAliasFor findAliasFor(String attrName) {
    return AliasForUtils.findAliasFor(getPsiElement(), this.myAnnotationMeta.getAnnoName(), InfraMvcConstant.REQUEST_MAPPING, attrName);
  }

  protected List<PsiTarget> getPomTargets() {
    return ContainerUtil.mapNotNull(getMappingUrls(), this.myPsiTargetMapping);
  }

  @Nullable
  protected InfraRequestMapping.ClassMapping getDefiningAnnotation() {
    return this.myDefiningMetaAnnotation.getValue();
  }

  public static class ClassMapping extends CustomRequestMapping<PsiClass> {
    static final SemKey<ClassMapping> JAM_KEY = RequestMapping.CLASS_JAM_KEY.subKey("CustomClassMapping");
    private static final JamClassMeta<ClassMapping> META = new JamClassMeta<>(null, ClassMapping.class, JAM_KEY);
    static final SemKey<JamMemberMeta<PsiClass, ClassMapping>> META_KEY = META.getMetaKey().subKey("CustomClassMappingMeta");

    public ClassMapping(String anno, PsiClass psiElement) {
      super(anno, psiElement);
    }

    @Override
    public List<WebMVCModelAttribute> getModelAttributes() {
      return MODEL_ATTRIBUTE_METHODS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }

    static Consumer<JamMemberMeta<PsiClass, ClassMapping>> createMetaConsumer() {
      return meta -> {
        meta.addChildrenQuery(MODEL_ATTRIBUTE_METHODS_QUERY).addPomTargetProducer((clazz, consumer) -> {
          for (PsiTarget target : clazz.getPomTargets()) {
            consumer.consume(target);
          }
        });
      };
    }
  }

  public static class MethodMapping extends CustomRequestMapping<PsiMethod> implements Method {
    static final SemKey<MethodMapping> JAM_KEY = RequestMapping.METHOD_JAM_KEY.subKey("CustomMethodMapping");
    public static final JamMethodMeta<MethodMapping> META = new JamMethodMeta<>(null, MethodMapping.class, JAM_KEY);
    static final SemKey<JamMemberMeta<PsiMethod, MethodMapping>> META_KEY = META.getMetaKey().subKey("CustomMethodMappingMeta");

    public MethodMapping(String anno, PsiMethod psiElement) {
      super(anno, psiElement);
    }

    @Override
    public RequestMethod[] getMethods() {
      return RequestMappingUtil.getAllRequestMethods(this);
    }

    @Override
    public List<WebMVCModelAttribute> getModelAttributes() {
      return MODEL_ATTRIBUTE_PARAMETERS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }

    @Override
    public RequestMethod[] getLocalMethods() {
      return super.getMethods();
    }

    @Override
    public List<MVCPathVariable> getPathVariables() {
      return PATH_VARIABLE_PARAMETERS_QUERY.findChildren(PsiElementRef.real(getPsiElement()));
    }

    static Consumer<JamMemberMeta<PsiMethod, MethodMapping>> createMetaConsumer() {
      return meta -> {
        meta.addChildrenQuery(MODEL_ATTRIBUTE_PARAMETERS_QUERY)
                .addChildrenQuery(PATH_VARIABLE_PARAMETERS_QUERY)
                .addPomTargetProducer((method, consumer) -> {
                  for (PsiTarget target : method.getPomTargets()) {
                    consumer.consume(target);
                  }
                });
      };
    }
  }

  public static class CustomRequestPresentationProvider extends PresentationProvider<CustomRequestMapping<?>> {
    @Nullable
    public String getTypeName(CustomRequestMapping<?> mapping) {
      String annotationName = mapping.myAnnotationMeta.getAnnoName();
      return "@" + StringUtil.getShortName(annotationName);
    }

    @Nullable
    public Icon getIcon(CustomRequestMapping<?> mapping) {
      return Icons.RequestMapping;
    }
  }
}
