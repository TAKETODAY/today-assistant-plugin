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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.properties.PropertiesHighlighter;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.ui.SimpleTextAttributes;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.config.InfraConfigFileAnnotatorBase;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraApplicationPropertiesAnnotator extends InfraConfigFileAnnotatorBase {

  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (!(element instanceof PropertyValueImpl) && !(element instanceof PropertyKeyImpl)) {
      return;
    }
    PsiFile file = holder.getCurrentAnnotationSession().getFile();
    if (!(file instanceof PropertiesFile)
            || !InfraUtils.hasFacets(file.getProject())
            || !InfraConfigurationFileService.of().isApplicationConfigurationFile(file)) {
      return;
    }
    if (element instanceof PropertyValueImpl) {
      annotateValue(element, holder);
    }
    if (element instanceof PropertyKeyImpl) {
      doAnnotateKey((PropertyKeyImpl) element, holder);
    }
  }

  @Override
  protected TextAttributesKey getPlaceholderTextAttributesKey() {
    return PropertiesHighlighter.PropertiesComponent.PROPERTY_KEY.getTextAttributesKey();
  }

  private void doAnnotateKey(PropertyKeyImpl element, AnnotationHolder holder) {
    MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(element);
    if (configKey == null) {
      return;
    }
    String keyText = element.getText();
    int elementStartOffset = element.getNode().getStartOffset();
    annotateIndexAccessExpressions(holder, keyText, configKey, elementStartOffset);
    if (configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      return;
    }
    annotateKey(element, holder);
    if (configKey.isAccessType(MetaConfigKey.AccessType.MAP) && configKey.getKeyItemHint() != MetaConfigKey.ItemHint.NONE) {
      int configKeyNameLength = -1;
      PsiReference[] references = element.getReferences();
      int length = references.length;
      int i = 0;
      while (true) {
        if (i >= length) {
          break;
        }
        PsiReference reference = references[i];
        if (!(reference instanceof MetaConfigKeyReference)) {
          i++;
        }
        else {
          configKeyNameLength = reference.getRangeInElement().getEndOffset();
          break;
        }
      }
      int endOffset = (keyText.length() - configKeyNameLength) - (StringUtil.endsWithChar(keyText, ']') ? 2 : 1);
      if (endOffset < 0) {
        return;
      }
      TextRange genericKeyRange = TextRange.from(configKeyNameLength + 1, endOffset).shiftRight(elementStartOffset);
      doAnnotateEnforced(holder, genericKeyRange, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES");
    }
  }

  private static void annotateIndexAccessExpressions(AnnotationHolder holder, String text, MetaConfigKey configKey, int elementStartOffset) {
    new IndexAccessTextProcessor(text, configKey) {
      protected void onMissingClosingBracket(int startIdx) {
      }

      protected void onMissingIndexValue(int startIdx) {
      }

      protected void onBracket(int startIdx) {
        doAnnotate(holder, TextRange.from(startIdx, 1).shiftRight(elementStartOffset), DefaultLanguageHighlighterColors.BRACKETS);
      }

      protected void onIndexValue(TextRange indexValueRange) {
        doAnnotate(holder, indexValueRange.shiftRight(elementStartOffset), DefaultLanguageHighlighterColors.NUMBER);
      }

      protected void onIndexValueNotInteger(TextRange indexValueRange) {
      }
    }.process();
  }
}
