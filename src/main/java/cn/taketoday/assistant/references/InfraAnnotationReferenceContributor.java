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

import com.intellij.patterns.PsiJavaElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;

import cn.taketoday.assistant.AnnotationConstant;
import cn.taketoday.assistant.InfraConstant;
import cn.taketoday.assistant.model.utils.resources.InfraResourcesBuilder;
import cn.taketoday.assistant.model.utils.resources.ResourcesUtil;

final class InfraAnnotationReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registerPsiClassReferences(registrar);
    registerAnnoReferenceProviders(registrar);
  }

  private static void registerAnnoReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PsiJavaPatterns.literalExpression().annotationParam(AnnotationConstant.ASYNC),
            new PsiReferenceProvider() {
              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                if (!(element instanceof PsiLiteralExpression)) {
                  return PsiReference.EMPTY_ARRAY;
                }
                PsiClass executorClass = JavaPsiFacade.getInstance(element.getProject())
                        .findClass("java.util.concurrent.Executor", element.getResolveScope());
                return new PsiReference[] {
                        new InfraBeanReference(element, ElementManipulators.getValueTextRange(element), executorClass, false)
                };
              }
            }, 100.0d);
  }

  private static void registerPsiClassReferences(PsiReferenceRegistrar registrar) {
    PsiJavaElementPattern.Capture<PsiNewExpression> newExpressionCapture = PsiJavaPatterns.psiNewExpression(
            InfraConstant.CLASS_PATH_XML_APP_CONTEXT,
            InfraConstant.CLASS_PATH_RESOURCE
    );
    var capture = PsiJavaPatterns.literalExpression().andOr(
            PsiJavaPatterns.psiExpression()
                    .withSuperParent(2, newExpressionCapture),
            PsiJavaPatterns.psiExpression().withSuperParent(4, newExpressionCapture)
    );
    registrar.registerReferenceProvider(capture, new ClassPathResourceReferenceProvider());
    registrar.registerReferenceProvider(
            PsiJavaPatterns.literalExpression().annotationParam(PsiJavaPatterns.psiAnnotation().qName(StandardPatterns.string().oneOf(AnnotationConstant.VALUE))),
            new ClassPathResourceReferenceProvider(), 100.0d);
  }

  public static class ClassPathResourceReferenceProvider extends PsiReferenceProvider {

    public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
      String s = ElementManipulators.getValueText(element);
      return ResourcesUtil.of().getClassPathReferences(InfraResourcesBuilder.create(element, s).fromRoot(s.startsWith("/")).soft(true));
    }
  }
}
