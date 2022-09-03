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

package cn.taketoday.assistant.model.jam.javaConfig;

import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamService;
import com.intellij.jam.JamSimpleReferenceConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamMethodMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.semantic.SemKey;
import com.intellij.util.ArrayUtilRt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.model.jam.JamPsiMemberInfraBean;
import cn.taketoday.lang.Nullable;

public class ContextJavaBean extends InfraJavaBean {
  public static final SemKey<JamAnnotationMeta> BEAN_ANNOTATION_KEY = JamService.ANNO_META_KEY.subKey("ContextJavaBean");
  public static final SemKey<ContextJavaBean> BEAN_JAM_KEY =
          JamPsiMemberInfraBean.PSI_MEMBERINFRA_BEAN_JAM_KEY.subKey("ContextJavaBean");
  public static final JamMethodMeta<ContextJavaBean> METHOD_META = new JamMethodMeta<>(null, ContextJavaBean.class, BEAN_JAM_KEY);

  // @Bean annotation meta
  private static final JamStringAttributeMeta.Collection<String> NAME_ATTRIBUTE_META = JamAttributeMeta.collectionString("name");
  private static final JamStringAttributeMeta.Collection<String> VALUE_ATTRIBUTE_META = JamAttributeMeta.collectionString("value");

  private static final JamStringAttributeMeta.Single<PsiMethod> INIT_METHOD_META =
          JamAttributeMeta.singleString("initMethod", new PsiMethodReferenceConverter());
  private static final JamStringAttributeMeta.Single<PsiMethod> DESTROY_METHOD_META =
          JamAttributeMeta.singleString("destroyMethod", new PsiMethodReferenceConverter());

//  private static final JamBooleanAttributeMeta autowireCandidate = new JamBooleanAttributeMeta("autowireCandidate", true);

  public static final JamAnnotationMeta ANNOTATION_META =
          new JamAnnotationMeta(AnnotationConstant.COMPONENT)
                  .addAttribute(NAME_ATTRIBUTE_META)
                  .addAttribute(VALUE_ATTRIBUTE_META)
                  .addAttribute(INIT_METHOD_META)
                  .addAttribute(DESTROY_METHOD_META);

  static {
    METHOD_META.addAnnotation(ANNOTATION_META);
    METHOD_META.addPomTargetProducer((contextJavaBean, consumer) -> {
      PsiTarget psiTarget = contextJavaBean.getPsiTarget();
      if (psiTarget != null) {
        consumer.consume(psiTarget);
      }
    });
  }

  @SuppressWarnings("unused")
  public ContextJavaBean(PsiMethod psiMethod) {
    super(PsiElementRef.real(psiMethod));
  }

  @Override
  public PsiAnnotation getPsiAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }

  @Override
  public String getBeanName() {
    List<JamStringAttributeElement<String>> elements = getBeanNameAttributeValue();
    if (elements.size() > 0) {
      return elements.get(0).getValue();
    }

    return super.getBeanName();
  }

  public JamStringAttributeElement<PsiMethod> getInitMethodAttributeElement() {
    return ANNOTATION_META.getAttribute(getPsiElement(), INIT_METHOD_META);
  }

  public JamStringAttributeElement<PsiMethod> getDestroyMethodAttributeElement() {
    return ANNOTATION_META.getAttribute(getPsiElement(), DESTROY_METHOD_META);
  }

  @Override
  public String[] getAliases() {
    // @Bean "name" attribute: The name of this bean, or if plural, aliases for this bean.
    List<JamStringAttributeElement<String>> elements = getBeanNameAttributeValue();

    if (elements.size() < 2)
      return ArrayUtilRt.EMPTY_STRING_ARRAY;

    List<String> aliases = getStringNames(elements);

    return ArrayUtilRt.toStringArray(aliases);
  }

  public List<PomNamedTarget> getPomTargets() {
    List<PomNamedTarget> pomTargets = new LinkedList<>();

    List<JamStringAttributeElement<String>> elements = getBeanNameAttributeValue();
    if (!elements.isEmpty()) {
      for (JamStringAttributeElement<String> attributeElement : elements) {
        pomTargets.add(new JamPomTarget(this, attributeElement));
      }
    }
    else {
      pomTargets.add(getPsiElement());
    }

    return pomTargets;
  }

  protected List<JamStringAttributeElement<String>> getBeanNameAttributeValue() {
    List<JamStringAttributeElement<String>> nameAttributes = getNameAttributeValue();

    return nameAttributes.isEmpty() ? getValueAttributeValue() : nameAttributes;
  }

  private List<JamStringAttributeElement<String>> getNameAttributeValue() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_ATTRIBUTE_META);
  }

  private List<JamStringAttributeElement<String>> getValueAttributeValue() {
    return ANNOTATION_META.getAttribute(getPsiElement(), VALUE_ATTRIBUTE_META);
  }

  @Override
  public PsiNamedElement getIdentifyingPsiElement() {
    PsiTarget psiTarget = getPsiTarget();
    if (psiTarget != null) {
      PsiElement psiElement = PomService.convertToPsi(getPsiManager().getProject(), psiTarget);
      if (psiElement instanceof PsiNamedElement)
        return (PsiNamedElement) psiElement;
    }
    return super.getIdentifyingPsiElement();
  }

  @Nullable
  public PsiTarget getPsiTarget() {
    List<JamStringAttributeElement<String>> beanNameAttributeValue = getBeanNameAttributeValue();
    if (beanNameAttributeValue == null || beanNameAttributeValue.size() == 0) {
      return null;
    }

    return new JamPomTarget(this, beanNameAttributeValue.get(0));
  }

  private static class PsiMethodReferenceConverter extends JamSimpleReferenceConverter<PsiMethod> {
    @Override
    public PsiMethod fromString(@Nullable String s, JamStringAttributeElement<PsiMethod> context) {
      for (PsiMethod psiMethod : getAppropriateMethods(context)) {
        if (psiMethod.getName().equals(s)) {
          return psiMethod;
        }
      }
      return null;
    }

    private static List<PsiMethod> getAppropriateMethods(JamStringAttributeElement<PsiMethod> context) {
      List<PsiMethod> methods = new ArrayList<>();
      PsiMethod method = PsiTreeUtil.getParentOfType(context.getPsiElement(), PsiMethod.class);

      if (method != null) {
        PsiType type = method.getReturnType();
        if (type instanceof PsiClassType) {
          PsiClass psiClass = ((PsiClassType) type).resolve();
          if (psiClass != null) {
            for (PsiMethod psiMethod : psiClass.getAllMethods()) {
              if (!psiMethod.isConstructor() && psiMethod.getParameterList().getParametersCount() == 0) {
                methods.add(psiMethod);
              }
            }
          }
        }
      }
      return methods;
    }

    @Override
    public Collection<PsiMethod> getVariants(JamStringAttributeElement<PsiMethod> context) {
      List<PsiMethod> methods = new ArrayList<>();
      for (PsiMethod method : getAppropriateMethods(context)) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
          if (!CommonClassNames.JAVA_LANG_OBJECT.equals(containingClass.getQualifiedName())) {
            methods.add(method);
          }
        }
      }
      return methods;
    }
  }
}
