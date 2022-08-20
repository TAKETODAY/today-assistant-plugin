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


package cn.taketoday.assistant.spel;

import com.intellij.openapi.util.text.DelimitedListProcessor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/20 01:19
 */
final class CommentReferenceContributor extends PsiReferenceContributor {

  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
            Holder.EL_VAR_COMMENT,
            new PsiReferenceProvider() {
              public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                Set<PsiReference> refs = new HashSet<>();
                String text = element.getText();
                CommentVariablesExtension.ElVarsProcessor processor = (vars, startOffset, endOffset) -> {
                  (new DelimitedListProcessor(",") {
                    protected void processToken(int start, int end, boolean delimitersOnly) {
                      int startInElement = startOffset + start;
                      int endInElement = startOffset + end;
                      String varDeclaration = text.substring(startInElement, endInElement);
                      int indexOf = varDeclaration.indexOf(":");
                      if (indexOf > 0 && varDeclaration.length() > indexOf + 1) {
                        String type = varDeclaration.substring(indexOf + 1);
                        PsiReference[] references = (new JavaClassReferenceProvider()).getReferencesByString(type, element, startInElement + indexOf + 1);
                        ContainerUtil.addAll(refs, references);
                      }

                    }
                  }).processText(vars);
                };
                CommentVariablesExtension.processVariableDeclarations(element.getText(), processor);
                return refs.toArray(PsiReference.EMPTY_ARRAY);
              }
            });
  }

  private static class Holder {
    private static final PsiElementPattern.Capture<PsiComment> EL_VAR_COMMENT
            = PlatformPatterns.psiElement(PsiComment.class).withText(StandardPatterns.string().contains("@el"));

  }
}
