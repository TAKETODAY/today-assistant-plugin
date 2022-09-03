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

package cn.taketoday.assistant.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.StringPattern;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.patterns.uast.UElementPattern;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiType;
import com.intellij.psi.UastReferenceRegistrar;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.UastUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.JavaeeConstant;
import cn.taketoday.assistant.ReferencePatternConditions;
import cn.taketoday.assistant.model.jam.SemContributorUtil;
import cn.taketoday.assistant.model.values.PlaceholderInfo;
import cn.taketoday.assistant.model.values.PlaceholderPropertyReference;
import cn.taketoday.assistant.model.values.PlaceholderUtils;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

public final class InfraUastReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registerPropertyReferences(registrar);
    registerQualifierNameReferenceProviders(registrar);
  }

  private void registerPropertyReferences(PsiReferenceRegistrar registrar) {
    UExpressionPattern elementPattern = UastPatterns.injectionHostUExpression(false);
    PatternCondition<PsiElement> patternCondition = ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION;
    UExpressionPattern injectionHostWithCondition = (UExpressionPattern) elementPattern.withSourcePsiCondition(patternCondition);
    ElementPattern inheritorOf = PsiJavaPatterns.psiClass().inheritorOf(false, InfraConstant.PROPERTY_RESOLVER_CLASS);
    UCallExpressionPattern callExpression = UastPatterns.callExpression();

    ElementPattern oneOf = StandardPatterns.string().oneOf("getProperty", "containsProperty", "getRequiredProperty");
    UCallExpressionPattern withMethodName = callExpression.withMethodName(oneOf);

    UastReferenceRegistrar.registerUastReferenceProvider(registrar,
            injectionHostWithCondition.callParameter(0, withMethodName.withReceiver(inheritorOf)),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    (uExpression, host) -> {
                      TextRange textRange = ElementManipulators.getValueTextRange(host);
                      String text = ElementManipulators.getValueText(host);
                      PlaceholderInfo info = new PlaceholderInfo(text, Pair.create("", ""), text, textRange);
                      return new PsiReference[] {
                              PlaceholderPropertyReference.create(host, textRange, info)
                      };
                    }), 100.0d);

    UCallExpressionPattern callExpression2 = UastPatterns.callExpression();
    StringPattern string = StandardPatterns.string();
    String[] strArr = InfraBeanNamesReferenceProvider.METHODS;
    ElementPattern oneOf2 = string.oneOf(Arrays.copyOf(strArr, strArr.length));
    UCallExpressionPattern withMethodName2 = callExpression2.withMethodName(oneOf2);
    ElementPattern inheritorOf2 = PsiJavaPatterns.psiClass().inheritorOf(false, InfraConstant.BEAN_FACTORY_CLASS);

    UastReferenceRegistrar.registerUastReferenceProvider(registrar, injectionHostWithCondition.inCall(withMethodName2.withReceiver(inheritorOf2)),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    (expression, host) -> {
                      return new PsiReference[] {
                              new InfraBeanReference(host, ElementManipulators.getValueTextRange(host),
                                      InfraBeanNamesReferenceProvider.determineRequiredClass(host), false)
                      };
                    }), 0.0d);
    ElementPattern oneOf3 = StandardPatterns.string().oneOf(JavaeeConstant.JAVAX_RESOURCE, JavaeeConstant.JAKARTA_RESOURCE);

    UastReferenceRegistrar.registerUastReferenceProvider(registrar, injectionHostWithCondition.annotationParam(oneOf3, "name"), UastReferenceRegistrar.uastInjectionHostReferenceProvider(
            (uLiteral, psi) -> {

              String it = UastUtils.evaluateString(uLiteral);
              String str = !StringUtil.isEmptyOrSpaces(it) ? it : null;
              if (str == null) {
                return PsiReference.EMPTY_ARRAY;
              }
              String beanName = str;
              boolean isFactoryBeanRef = StringsKt.startsWith(beanName, "&", false);
              if (isFactoryBeanRef) {
                beanName = beanName.substring(1);
              }

              String text = psi.getText();
              TextRange range = TextRange.from(StringsKt.indexOf(text, beanName, 0, false), beanName.length());

              UVariable parentOfType = UastUtils.getParentOfType(uLiteral, UVariable.class, true);
              PsiType type = parentOfType != null ? parentOfType.getType() : null;
              if (!(type instanceof PsiClassType)) {
                type = null;
              }
              PsiClassType requiredClassType = (PsiClassType) type;
              return new PsiReference[] {
                      new InfraBeanResourceReference(psi, range, requiredClassType != null ? requiredClassType.resolve() : null, isFactoryBeanRef)
              };
            }), 100.0d);
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, injectionHostWithCondition.annotationParam(AnnotationConstant.SCOPE, "value"),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    new Function2<>() {
                      @Override
                      public PsiReference[] invoke(UExpression uLiteral, PsiLanguageInjectionHost host) {
                        return new PsiReference[] {
                                new InfraBeanScopeReference(uLiteral, host)
                        };
                      }
                    }), 100.0d);
    Map<String, List<String>> annotations = PlaceholderReferencesPlaces.PLACEHOLDER_ANNOTATIONS;
    ArrayList<UExpressionPattern> expressionPatterns = new ArrayList<>(annotations.size());
    for (Map.Entry<String, List<String>> item$iv$iv : annotations.entrySet()) {
      ElementPattern oneOf4 = StandardPatterns.string().oneOf(item$iv$iv.getValue());
      expressionPatterns.add(injectionHostWithCondition.annotationParams(item$iv$iv.getKey(), oneOf4));
    }
    UExpressionPattern[] elementPatternArr = expressionPatterns.toArray(new UExpressionPattern[0]);
    ElementPattern or = PsiJavaPatterns.or(Arrays.copyOf(elementPatternArr, elementPatternArr.length));

    UastReferenceRegistrar.registerUastReferenceProvider(registrar, or,
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    new Function2<>() {
                      @Override
                      public PsiReference[] invoke(UExpression expression, PsiLanguageInjectionHost host) {
                        return PlaceholderUtils.getInstance().createPlaceholderPropertiesReferences(host);
                      }
                    }),
            100.0d);
  }

  private void registerQualifierNameReferenceProviders(PsiReferenceRegistrar registrar) {
    UExpressionPattern elementPattern = UastPatterns.injectionHostUExpression(false);
    PatternCondition<PsiElement> patternCondition = ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION;

    UExpressionPattern uElementPattern = (UExpressionPattern) elementPattern.withSourcePsiCondition(patternCondition);
    UastReferenceRegistrar.registerUastReferenceProvider(registrar,
            uElementPattern.annotationParam("value", qualifierAnnotation()),
            new InfraUastQualifierNameReferenceProvider(), 0.0d);
  }

  private UElementPattern.Capture<UAnnotation> qualifierAnnotation() {
    return UastPatterns.capture(UAnnotation.class)
            .filter(uAnnotation -> {
              Module module;
              String qualifiedName = uAnnotation.getQualifiedName();
              if (qualifiedName != null) {
                PsiElement sourcePsi = uAnnotation.getSourcePsi();
                if (sourcePsi == null || (module = ModuleUtilCore.findModuleForPsiElement(sourcePsi)) == null) {
                  return false;
                }
                boolean isQualifierAnnotation = isQualifierAnnotation(qualifiedName, AnnotationConstant.QUALIFIER, module);
                if (!isQualifierAnnotation) {
                  return isQualifierAnnotation(qualifiedName, JavaeeConstant.JAVAX_INJECT_QUALIFIER, module);
                }
                return true;
              }
              return false;
            });
  }

  public boolean isQualifierAnnotation(String anno, String qualifierAnno, Module module) {
    return Intrinsics.areEqual(anno, qualifierAnno) || SemContributorUtil.getCustomMetaAnnotations(qualifierAnno).fun(module).contains(anno);
  }
}
