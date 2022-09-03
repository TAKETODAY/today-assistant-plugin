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

package cn.taketoday.assistant.model.values.converters;

import com.intellij.lang.properties.ResourceBundleManager;
import com.intellij.lang.properties.ResourceBundleReference;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import com.intellij.util.xml.CustomReferenceConverter;
import com.intellij.util.xml.GenericDomValue;

import cn.taketoday.assistant.model.converters.PatternFileReferenceSet;
import cn.taketoday.lang.Nullable;

public class BundleNameConverter extends Converter<PropertiesFile> implements CustomReferenceConverter {
  private static final String[] prefixes = { "classpath:", "classpath*:" };

  public static final String RESOURCE_BUNDLE_MESSAGE_SOURCE = "cn.taketoday.context.support.ResourceBundleMessageSource";

  public static final String RELOADABLE_RESOURCE_BUNDLE_MESSAGE_SOURCE = "cn.taketoday.context.support.ReloadableResourceBundleMessageSource";

  @Nullable
  public PropertiesFile fromString(@Nullable String s, ConvertContext context) {
    return null;
  }

  @Nullable
  public String toString(@Nullable PropertiesFile file, ConvertContext context) {
    return null;
  }

  public PsiReference[] createReferences(GenericDomValue value, PsiElement element, ConvertContext context) {
    int offset;
    int endOffset;
    String text = element.getText();
    if (StringUtil.isNotEmpty(text)) {
      try {
        TextRange range = ElementManipulators.getValueTextRange(element);
        String textValue = range.substring(text);
        for (String prefix : prefixes) {
          int lastIndexOf = textValue.lastIndexOf(prefix);
          if (lastIndexOf >= 0 && (offset = range.getStartOffset() + lastIndexOf + prefix.length()) < (endOffset = range.getEndOffset())) {
            TextRange textRange = TextRange.create(offset, endOffset);
            if (textValue.contains("/")) {
              return getPathReferences(element, textRange, textRange.substring(text));
            }
            return new PsiReference[] { new ResourceBundleReference(element, textRange, true) };
          }
        }
        if (textValue.contains("/")) {
          return getPathReferences(element, range, textValue);
        }
      }
      catch (Exception ignored) {
      }
    }
    return new PsiReference[] { new ResourceBundleReference(element, true) };
  }

  public PsiReference[] getPathReferences(PsiElement element, TextRange range, String textValue) {
    FileReferenceSet set = new PatternFileReferenceSet(textValue, element, range.getStartOffset(), true) {
      @Override
      public FileReference createFileReference(TextRange range2, int index, String text) {
        return new PatternFileReference(this, range2, index, text, getReferenceCompletionFilter()) {

          public ResolveResult[] multiResolve(boolean incompleteCode) {
            ResolveResult[] results = super.multiResolve(incompleteCode);
            if (results.length == 0) {
              String s = getText();
              for (PsiFileSystemItem psiDirectory : getContexts()) {
                if (psiDirectory instanceof PsiDirectory directory) {
                  for (PsiFile file : directory.getFiles()) {
                    if ((file instanceof PropertiesFile) && s.equals(ResourceBundleManager.getInstance(getElement().getProject()).getBaseName(file))) {
                      return new ResolveResult[] { new PsiElementResolveResult(file) };
                    }
                  }
                }
              }
            }
            return results;
          }
        };
      }
    };
    if (textValue.startsWith("/")) {
      set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION, FileReferenceSet.ABSOLUTE_TOP_LEVEL);
    }
    return set.getAllReferences();
  }

  public static class BundleNameConverterCondition implements Condition<Pair<PsiType, GenericDomValue>> {
    private static final Condition<GenericDomValue> CONDITION = InfraValueConditionFactory.createBeanPropertyCondition(RESOURCE_BUNDLE_MESSAGE_SOURCE, "basename", "basenames");
    private static final Condition<GenericDomValue> RELOADABLE_MESSAGE_SOURCE_CONDITION = InfraValueConditionFactory.createBeanPropertyCondition(
            RELOADABLE_RESOURCE_BUNDLE_MESSAGE_SOURCE, "basename", "basenames");

    public boolean value(Pair<PsiType, GenericDomValue> pair) {
      PsiType psiType = pair.getFirst();
      if (psiType instanceof PsiArrayType arrayType) {
        psiType = arrayType.getComponentType();
      }
      if (!(psiType instanceof PsiClassType)) {
        return false;
      }
      String psiTypeText = psiType.getCanonicalText();
      return "java.lang.String".equals(psiTypeText) && (CONDITION.value(pair.second) || RELOADABLE_MESSAGE_SOURCE_CONDITION.value(pair.second));
    }
  }
}
