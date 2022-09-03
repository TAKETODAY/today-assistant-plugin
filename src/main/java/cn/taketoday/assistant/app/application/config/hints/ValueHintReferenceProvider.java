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

package cn.taketoday.assistant.app.application.config.hints;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.hints.HintReferenceProviderBase;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.app.application.metadata.InfraValueHintPsiElement;
import cn.taketoday.lang.Nullable;

class ValueHintReferenceProvider extends HintReferenceProviderBase {
  private final List<? extends MetaConfigKey.ValueHint> myValueHints;
  private final MetaConfigKey myConfigKey;
  private final boolean myAllowOtherValues;

  ValueHintReferenceProvider(MetaConfigKey configKey, List<? extends MetaConfigKey.ValueHint> valueHints, boolean allowOtherValues) {
    this.myConfigKey = configKey;
    this.myValueHints = valueHints;
    this.myAllowOtherValues = allowOtherValues;
  }

  protected PsiReference createReference(PsiElement element, TextRange textRange, ProcessingContext context) {
    return new HintReferenceBase(element, textRange) {

      public TextAttributesKey getTextAttributesKey() {
        return DefaultLanguageHighlighterColors.INSTANCE_METHOD;
      }

      public boolean isSoft() {
        return ValueHintReferenceProvider.this.myAllowOtherValues;
      }

      @Nullable
      protected PsiElement doResolve() {
        String value = getValue();
        MetaConfigKey.ValueHint valueHint = ContainerUtil.find(ValueHintReferenceProvider.this.myValueHints, hint -> {
          return hint.getValue().equals(value);
        });
        if (valueHint != null) {
          return new InfraValueHintPsiElement(getElement(), valueHint);
        }
        return null;
      }

      public Object[] getVariants() {
        return ContainerUtil.map2Array(ValueHintReferenceProvider.this.myValueHints, LookupElementBuilder.class, hint -> {
          LookupElementBuilder builder = LookupElementBuilder.create(new InfraValueHintPsiElement(getElement(), hint))
                  .withBoldness(hint.getValue().equals(ValueHintReferenceProvider.this.myConfigKey.getDefaultValue()));
          String shortDescriptionText = hint.getDescriptionText().getShortText();
          if (StringUtil.isNotEmpty(shortDescriptionText)) {
            builder = builder.appendTailText(" (" + shortDescriptionText + ")", true);
          }
          return builder.withIcon(Icons.Today);
        });
      }

      public String getUnresolvedMessagePattern() {
        String[] valueHintNames = ContainerUtil.map2Array(ValueHintReferenceProvider.this.myValueHints, String.class, hint -> "''" + hint.getValue() + "''");
        return "Invalid value ''{0}'', must be one of " + StringUtil.join(valueHintNames, "|");
      }
    };
  }
}
