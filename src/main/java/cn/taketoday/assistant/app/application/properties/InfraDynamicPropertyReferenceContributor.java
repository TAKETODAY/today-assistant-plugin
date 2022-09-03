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

package cn.taketoday.assistant.app.application.properties;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.uast.UCallExpressionPattern;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastReferenceRegistrar;

import org.jetbrains.uast.UExpression;

import cn.taketoday.assistant.ReferencePatternConditions;
import kotlin.jvm.functions.Function2;

public final class InfraDynamicPropertyReferenceContributor extends PsiReferenceContributor {
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    UExpressionPattern uExpressionPattern = UastPatterns.injectionHostUExpression(false);
    PatternCondition patternCondition = ReferencePatternConditions.PROJECT_HAS_FACETS_CONDITION;
    UExpressionPattern injectionHostWithCondition = (UExpressionPattern) uExpressionPattern.withSourcePsiCondition(patternCondition);
    UCallExpressionPattern withMethodName = UastPatterns.callExpression().withMethodName("add");
    ElementPattern withQualifiedName = PsiJavaPatterns.psiClass().withQualifiedName("cn.taketoday.test.context.DynamicPropertyRegistry");
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, injectionHostWithCondition.callParameter(0, withMethodName.withReceiver(withQualifiedName)),
            UastReferenceRegistrar.uastInjectionHostReferenceProvider(
                    new Function2<UExpression, PsiLanguageInjectionHost, PsiReference[]>() {
                      @Override
                      public PsiReference[] invoke(UExpression $noName_0, PsiLanguageInjectionHost host) {
                        String valueText = ElementManipulators.getValueText(host);
                        return new PsiReference[] { new DynamicPropertyRegistryReference(host, valueText) };
                      }
                    }), 0.0d);
  }
}
