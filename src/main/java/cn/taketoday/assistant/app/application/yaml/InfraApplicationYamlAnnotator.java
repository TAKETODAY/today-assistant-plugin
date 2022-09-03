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

package cn.taketoday.assistant.app.application.yaml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.microservices.jvm.config.ConfigKeyPathArbitraryEntryKeyReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.yaml.YAMLHighlighter;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.config.InfraConfigFileAnnotatorBase;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.util.InfraUtils;

public class InfraApplicationYamlAnnotator extends InfraConfigFileAnnotatorBase {

  @Override
  public void annotate(PsiElement element, AnnotationHolder holder) {
    YAMLKeyValue yamlKeyValue;
    PsiElement yamlKeyElement;
    if (!(element instanceof YAMLKeyValue)) {
      return;
    }
    PsiFile file = holder.getCurrentAnnotationSession().getFile();
    if (!(file instanceof YAMLFile)
            || !InfraUtils.hasFacets(file.getProject())
            || !InfraConfigurationFileService.of().isApplicationConfigurationFile(file)
            || (yamlKeyElement = (yamlKeyValue = (YAMLKeyValue) element).getKey()) == null) {
      return;
    }
    doAnnotateKey(yamlKeyValue, yamlKeyElement, holder);
    YAMLValue value = yamlKeyValue.getValue();
    if (value instanceof YAMLScalar) {
      annotateValue(value, holder);
    }
    else if (value instanceof YAMLSequence sequence) {
      for (YAMLSequenceItem item : sequence.getItems()) {
        YAMLValue value2 = item.getValue();
        if (value2 instanceof YAMLScalar) {
          annotateValue(value2, holder);
        }
      }
    }
  }

  @Override
  protected TextAttributesKey getPlaceholderTextAttributesKey() {
    return YAMLHighlighter.SCALAR_KEY;
  }

  private void doAnnotateKey(YAMLKeyValue keyValue, PsiElement yamlKeyElement, AnnotationHolder holder) {
    String keyName = ConfigYamlUtils.getQualifiedConfigKeyName(keyValue);
    PsiReference[] references = keyValue.getReferences();
    MetaConfigKeyReference<?> keyReference = ContainerUtil.findInstance(references, MetaConfigKeyReference.class);
    MetaConfigKey configKey = keyReference != null ? keyReference.getResolvedKey() : null;
    if (InfraMetadataConstant.INFRA_PROFILES_KEY.equals(keyName) && configKey == null) {
      holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .range(yamlKeyElement)
              .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
              .create();
    }
    annotateKey(keyValue, holder);
    int elementStartOffset = keyValue.getNode().getStartOffset();
    ConfigKeyPathArbitraryEntryKeyReference entryKeyReference = ContainerUtil.findInstance(references, ConfigKeyPathArbitraryEntryKeyReference.class);
    if (entryKeyReference != null && entryKeyReference.getRangeInElement().isEmpty()) {
      TextRange textRange = ConfigYamlUtils.getKeyReferenceRange(keyValue);
      doAnnotateEnforced(holder,
              ConfigYamlUtils.trimIndexedAccess(textRange, keyValue)
                      .shiftRight(elementStartOffset),
              SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES, "REGULAR_ITALIC_ATTRIBUTES"
      );
    }
    PsiElement keyElement = keyValue.getKey();
    if (keyElement != null) {
      annotateIndexAccessExpressions(holder, keyElement.getText(), configKey, elementStartOffset);
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
      }

      protected void onIndexValueNotInteger(TextRange indexValueRange) {
      }
    }.process();
  }
}
