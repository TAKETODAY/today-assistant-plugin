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

package cn.taketoday.assistant.web.mvc.pathVariables;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.microservices.jvm.pathvars.PathVariableReferenceProvider;
import com.intellij.microservices.jvm.pathvars.usages.PathVariableSemParametersUsageSearcher;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.StringPattern;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;

import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UExpression;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.jam.InfraMvcUrlPathSpecification;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;

public final class WebMvcPathVariableDeclaration {

  public static final UExpressionPattern<?, ?> PATH_VARIABLE_DECLARATION_PATTERN;
  public static final PathVariableReferenceProvider PROVIDER;

  static {
    UExpressionPattern.Capture<UExpression> uExpression = UastPatterns.uExpression();
    PatternCondition<PsiElement> patternCondition = InfraLibraryUtil.IS_WEB_MVC_PROJECT;
    StringPattern oneOf = StandardPatterns.string().oneOf(RequestMapping.VALUE_ATTRIBUTE, "path");
    PATH_VARIABLE_DECLARATION_PATTERN = uExpression.withSourcePsiCondition(patternCondition)
            .annotationParams(UastPatterns.capture(UAnnotation.class)
                    .filter(annotation -> {
                      if (Intrinsics.areEqual(InfraMvcConstant.REQUEST_MAPPING, annotation.getQualifiedName())) {
                        return true;
                      }
                      PsiModifierListOwner resolve = annotation.resolve();
                      if (resolve == null) {
                        return false;
                      }
                      return MetaAnnotationUtil.isMetaAnnotated(resolve, CollectionsKt.listOf(InfraMvcConstant.REQUEST_MAPPING));
                    }), oneOf);
    PROVIDER = new PathVariableReferenceProvider(InfraMvcUrlPathSpecification.INSTANCE, PathVariableSemParametersUsageSearcher.INSTANCE);
  }
}
