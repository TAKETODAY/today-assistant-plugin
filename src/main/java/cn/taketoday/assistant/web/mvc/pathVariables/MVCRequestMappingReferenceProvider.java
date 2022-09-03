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

import com.intellij.microservices.url.parameters.PathVariableDeclaringReference;
import com.intellij.microservices.url.parameters.PathVariableDefinitionsSearcher;
import com.intellij.microservices.url.parameters.PathVariablePsiElement;
import com.intellij.microservices.url.parameters.PathVariableUsageReference;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.uast.UExpressionPattern;
import com.intellij.patterns.uast.UastPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.UastInjectionHostReferenceProvider;
import com.intellij.psi.UastReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;

import org.jetbrains.uast.UExpression;

import cn.taketoday.assistant.web.mvc.InfraMvcConstant;
import cn.taketoday.assistant.web.mvc.model.jam.RequestMapping;

public class MVCRequestMappingReferenceProvider extends UastInjectionHostReferenceProvider {
  static final UExpressionPattern<UExpression, ?> PATTERN = UastPatterns.uExpression()
          .annotationParams(InfraMvcConstant.PATH_VARIABLE, StandardPatterns.string().oneOf(RequestMapping.VALUE_ATTRIBUTE, "name"));

  public static void register(PsiReferenceRegistrar registrar) {
    UastReferenceRegistrar.registerUastReferenceProvider(registrar, PATTERN, new MVCRequestMappingReferenceProvider(), 100.0d);
  }

  @Override
  public PsiReference[] getReferencesForInjectionHost(UExpression uExpression, PsiLanguageInjectionHost host, ProcessingContext context) {
    return new PsiReference[] { new PathVariableUsageReference(host, new MyPathVariableDefinitionsSearcher()) };
  }

  public static class MyPathVariableDefinitionsSearcher implements PathVariableDefinitionsSearcher {

    @Override
    public boolean processDefinitions(PsiElement context, Processor<? super PathVariablePsiElement> processor) {
      return WebMvcPathVariableDeclarationSearcher.collectDeclarations(context)
              .map(PathVariableDeclaringReference::resolve)
              .processWith(processor);
    }
  }
}
