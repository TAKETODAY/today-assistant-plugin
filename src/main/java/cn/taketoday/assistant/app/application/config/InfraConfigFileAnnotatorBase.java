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
package cn.taketoday.assistant.app.application.config;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.microservices.jvm.config.ConfigKeyPathReference;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.beanProperties.BeanPropertyElement;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.assistant.core.StrategiesClassesListJamConverter;
import cn.taketoday.lang.Nullable;

public abstract class InfraConfigFileAnnotatorBase implements Annotator {

  /**
   * @see InfraPlaceholderReference
   */
  @Nullable
  protected abstract TextAttributesKey getPlaceholderTextAttributesKey();

  /**
   * @see InfraReplacementTokenResolver
   */
  @Nullable
  protected TextAttributesKey getReplacementTokenTextAttributesKey() {
    return DefaultLanguageHighlighterColors.METADATA;
  }

  /**
   * Highlights references in value using language default colors.
   *
   * @param element Value element.
   * @param holder Holder.
   */
  protected void annotateValue(PsiElement element, AnnotationHolder holder) {
    int elementOffset = element.getNode().getStartOffset();
    PsiReference[] references = element.getReferences();

    boolean highlightOnlyPlaceholders =
            ContainerUtil.findInstance(references, InfraPlaceholderReference.class) != null;

    Set<Integer> annotatedOffsets = new HashSet<>();

    for (PsiReference reference : references) {
      TextAttributesKey key = null;
      if (highlightOnlyPlaceholders) {
        if (reference instanceof InfraPlaceholderReference) {
          key = getPlaceholderTextAttributesKey();
        }
      }
      else {
        if (reference instanceof JavaClassReference ||
                reference instanceof StrategiesClassesListJamConverter.ClassReference ||
                reference instanceof PsiPackageReference) {
          if (reference.resolve() != null) {   // FQN references are injected by default in .properties
            key = DefaultLanguageHighlighterColors.CLASS_REFERENCE;
          }
        }
        else if (reference instanceof InfraReplacementTokenResolver.ReplacementTokenReference) {
          key = getReplacementTokenTextAttributesKey();
        }
        else if (reference instanceof HintReferenceBase) {
          key = ((HintReferenceBase) reference).getTextAttributesKey();
        }
      }

      if (key != null) {
        TextRange highlightTextRange = reference.getRangeInElement().shiftRight(elementOffset);
        if (!annotatedOffsets.add(highlightTextRange.getStartOffset()))
          continue;

        doAnnotate(holder, highlightTextRange, key);
      }
    }
  }

  /**
   * Highlights references in key using language default colors.
   *
   * @param element Key element.
   * @param holder Holder.
   */
  protected void annotateKey(PsiElement element, AnnotationHolder holder) {
    int elementStartOffset = element.getNode().getStartOffset();
    for (PsiReference psiReference : element.getReferences()) {
      if (!(psiReference instanceof ConfigKeyPathReference configKeyPathReference)) {
        continue;
      }

      TextRange referenceRange = configKeyPathReference.getRangeInElement().shiftRight(elementStartOffset);

      switch (configKeyPathReference.getPathType()) {
        case ENUM -> doAnnotate(holder, referenceRange, DefaultLanguageHighlighterColors.CONSTANT);
        case BEAN_PROPERTY -> {
          doAnnotate(holder, referenceRange, DefaultLanguageHighlighterColors.INSTANCE_METHOD);
          PsiElement resolve = configKeyPathReference.resolve();
          if (resolve instanceof BeanPropertyElement) {
            if (((BeanPropertyElement) resolve).getMethod().isDeprecated()) {
              doAnnotate(holder, referenceRange, CodeInsightColors.DEPRECATED_ATTRIBUTES);
            }
          }
        }
        case ARBITRARY_ENTRY_KEY -> doAnnotateEnforced(holder, referenceRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES");
      }
    }
  }

  private static final boolean DEBUG_MODE = ApplicationManager.getApplication().isUnitTestMode();

  protected static void doAnnotate(AnnotationHolder holder, TextRange range, TextAttributesKey key) {
    if (range.isEmpty())
      return;

    (DEBUG_MODE
     ? holder.newAnnotation(HighlightSeverity.INFORMATION, key.getExternalName())
     : holder.newSilentAnnotation(HighlightSeverity.INFORMATION)).range(range).textAttributes(key).create();
  }

  protected static void doAnnotateEnforced(AnnotationHolder holder, TextRange range, SimpleTextAttributes key, String debugMessage) {
    if (range.isEmpty())
      return;

    //noinspection HardCodedStringLiteral
    String message = DEBUG_MODE ? debugMessage : null;
    (message != null ? holder.newAnnotation(HighlightSeverity.INFORMATION, message) : holder.newSilentAnnotation(HighlightSeverity.INFORMATION))
            .range(range).enforcedTextAttributes(key.toTextAttributes()).create();
  }
}

