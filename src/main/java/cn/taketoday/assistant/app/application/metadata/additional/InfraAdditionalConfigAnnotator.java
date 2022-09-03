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

package cn.taketoday.assistant.app.application.metadata.additional;

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.json.JsonUtil;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.TextWithIcon;

import java.util.Collections;
import java.util.List;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.gutter.GutterIconBuilder;
import cn.taketoday.lang.Nullable;

public class InfraAdditionalConfigAnnotator implements Annotator {
  private static final boolean DEBUG_MODE = ApplicationManager.getApplication().isUnitTestMode();

  private static PsiElementListCellRenderer<PsiElement> getGotoHintGutterCellRenderer() {
    return new PsiElementListCellRenderer<PsiElement>() {

      public String getElementText(PsiElement element) {
        JsonValue value = ((JsonProperty) element).getValue();
        return ((JsonStringLiteral) value).getValue();
      }

      @Nullable
      protected String getContainerText(PsiElement element, String name) {
        return null;
      }

      @Nullable
      protected TextWithIcon getItemLocation(Object value) {
        return null;
      }
    };
  }

  public void annotate(PsiElement element, AnnotationHolder holder) {
    if (element instanceof JsonProperty jsonProperty) {
      PsiFile file = holder.getCurrentAnnotationSession().getFile();
      if (InfraAdditionalConfigUtils.isAdditionalMetadataFile(file)) {
        String name = jsonProperty.getName();
        switch (name) {
          case InfraMetadataConstant.TYPE, InfraMetadataConstant.SOURCE_TYPE, InfraMetadataConstant.TARGET -> highlight(holder, jsonProperty, DefaultLanguageHighlighterColors.CLASS_REFERENCE);
          case InfraMetadataConstant.DESCRIPTION, InfraMetadataConstant.REASON -> highlight(holder, jsonProperty, DefaultLanguageHighlighterColors.DOC_COMMENT);
          case InfraMetadataConstant.DEFAULT_VALUE -> highlight(holder, jsonProperty, DefaultLanguageHighlighterColors.METADATA);
          case "value" -> highlight(holder, jsonProperty, DefaultLanguageHighlighterColors.PARAMETER);
          case InfraMetadataConstant.NAME -> installHintsGutterIcon(holder, (JsonFile) file, jsonProperty);
        }
      }
    }
  }

  private static void highlight(AnnotationHolder holder, JsonProperty jsonProperty, TextAttributesKey key) {
    JsonValue value = jsonProperty.getValue();
    if (value != null) {
      String message = DEBUG_MODE ? key.getExternalName() : null;
      (message == null ? holder.newSilentAnnotation(HighlightSeverity.INFORMATION) : holder.newAnnotation(HighlightSeverity.INFORMATION, message)).range(value).textAttributes(key).create();
    }
  }

  private static void installHintsGutterIcon(AnnotationHolder holder, JsonFile file, JsonProperty nameProperty) {
    List<JsonProperty> hintProperties = findHints(file, nameProperty);
    if (hintProperties.isEmpty()) {
      return;
    }
    GutterIconBuilder.create(Icons.Gutter.Today)
            .setTooltipText(InfraAppBundle.message("goto.hint.tooltip", hintProperties.size()))
            .setCellRenderer(InfraAdditionalConfigAnnotator::getGotoHintGutterCellRenderer)
            .setTargets(hintProperties)
            .createGutterIcon(holder, nameProperty);
  }

  private static List<JsonProperty> findHints(JsonFile file, JsonProperty nameProperty) {
    JsonProperty hintNameProperty;
    JsonStringLiteral nameValue;
    JsonProperty parentProperty = PsiTreeUtil.getParentOfType(nameProperty, JsonProperty.class);
    if (parentProperty == null) {
      return Collections.emptyList();
    }
    else if (!parentProperty.getName().equals(InfraMetadataConstant.PROPERTIES)) {
      return Collections.emptyList();
    }
    else {
      JsonStringLiteral namePropertyValue = ObjectUtils.tryCast(nameProperty.getValue(), JsonStringLiteral.class);
      if (namePropertyValue == null) {
        return Collections.emptyList();
      }
      String propertyName = namePropertyValue.getValue();
      JsonObject topValue = ObjectUtils.tryCast(file.getTopLevelValue(), JsonObject.class);
      if (topValue == null) {
        return Collections.emptyList();
      }
      JsonArray hintsArray = JsonUtil.getPropertyValueOfType(topValue, InfraMetadataConstant.HINTS, JsonArray.class);
      if (hintsArray == null) {
        return Collections.emptyList();
      }
      SmartList<JsonProperty> smartList = new SmartList<>();
      for (JsonValue hint : hintsArray.getValueList()) {
        JsonObject hintObject = ObjectUtils.tryCast(hint, JsonObject.class);
        if (hintObject != null && (hintNameProperty = hintObject.findProperty(InfraMetadataConstant.NAME)) != null && (nameValue = ObjectUtils.tryCast(
                hintNameProperty.getValue(), JsonStringLiteral.class)) != null) {
          String name = nameValue.getValue();
          if (StringUtil.startsWith(name, propertyName)
                  && (name.equals(propertyName)
                  || name.equals(propertyName + ".keys")
                  || name.equals(propertyName + ".values"))) {
            smartList.add(hintNameProperty);
          }
        }
      }
      return smartList;
    }
  }
}
