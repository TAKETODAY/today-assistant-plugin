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

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.completion.YamlKeyCompletionInsertHandler;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.lang.Nullable;

final class InfraApplicationYamlDeprecationFixFactory {

  static LocalQuickFix[] getDeprecationFixes(PsiElement keyElement, MetaConfigKey configKey, boolean isOnTheFly) {
    String replacement = configKey.getDeprecation().getReplacement();
    if (replacement == null) {
      return LocalQuickFix.EMPTY_ARRAY;
    }
    return replacementKeyExists(keyElement, replacement) ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] { new UseReplacementKeyFix(keyElement, replacement, configKey, isOnTheFly) };
  }

  private static boolean replacementKeyExists(PsiElement keyElement, String replacement) {
    ConfigYamlAccessor accessor = new ConfigYamlAccessor(keyElement, InfraApplicationMetaConfigKeyManager.of());
    YAMLKeyValue existingReplacement = accessor.findExistingKey(replacement);
    return existingReplacement != null;
  }

  private static final class UseReplacementKeyFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String myReplacement;
    private final MetaConfigKey myConfigKey;
    private final boolean myIsOnTheFly;

    private UseReplacementKeyFix(PsiElement keyElement, String replacement, MetaConfigKey configKey, boolean isOnTheFly) {
      super(keyElement);
      this.myReplacement = replacement;
      this.myConfigKey = configKey;
      this.myIsOnTheFly = isOnTheFly;
    }

    public String getText() {
      String message = InfraAppBundle.message("application.config.replacement.quick.fix", this.myReplacement);
      return message;
    }

    public void invoke(Project project, PsiFile file, @Nullable Editor editor, PsiElement startElement, PsiElement endElement) {
      YAMLKeyValue existingKey;
      if (replacementKeyExists(startElement, this.myReplacement)
              || (existingKey = PsiTreeUtil.getParentOfType(startElement, YAMLKeyValue.class)) == null) {
        return;
      }
      String existingKeyName = ConfigYamlUtils.getQualifiedConfigKeyName(existingKey);
      Module module = ModuleUtilCore.findModuleForPsiElement(startElement);
      InfraApplicationMetaConfigKeyManager metaConfigKeyManager = InfraApplicationMetaConfigKeyManager.of();
      MetaConfigKeyManager.ConfigKeyNameBinder binder = metaConfigKeyManager.getConfigKeyNameBinder(module);
      if (!binder.bindsTo(this.myConfigKey, existingKeyName)) {
        return;
      }
      ConfigYamlAccessor accessor = new ConfigYamlAccessor(startElement, metaConfigKeyManager);
      YAMLValue existingValue = existingKey.getValue();
      PsiElement existingValueCopy = existingValue == null ? null : existingValue.copy();
      if (existingValueCopy instanceof YAMLSequence) {
        YamlKeyCompletionInsertHandler.trimSequenceItemIndents((YAMLSequence) existingValueCopy);
      }
      YAMLMapping mapping = existingKey.getParentMapping();
      deleteKeyValuesRecursively(mapping, existingKey);
      YAMLKeyValue replacementKeyValue = accessor.create(this.myReplacement);
      if (existingValueCopy != null) {
        MetaConfigKey replacementMetaConfigKey = metaConfigKeyManager.findApplicationMetaConfigKey(module, this.myReplacement);
        if (replacementMetaConfigKey != null && replacementMetaConfigKey.getType() != null && this.myConfigKey.getType() != null && !this.myConfigKey.getType()
                .isAssignableFrom(replacementMetaConfigKey.getType())) {
          if (editor == null) {
            return;
          }
          ASTNode[] colons = replacementKeyValue.getNode().getChildren(TokenSet.create(YAMLTokenTypes.COLON));
          ASTNode colon = colons[0];
          editor.getCaretModel().moveToOffset(colon.getTextRange().getEndOffset());
          PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
          EditorModificationUtil.insertStringAtCaret(editor, " ");
          AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
          return;
        }
        replacementKeyValue.setValue((YAMLValue) existingValueCopy);
      }
      if (this.myIsOnTheFly) {
        replacementKeyValue.navigate(true);
      }
    }

    public String getFamilyName() {
      return InfraAppBundle.message("application.config.replacement.quick.fix.family.name");
    }

    private static void deleteKeyValuesRecursively(YAMLMapping mapping, YAMLKeyValue keyValue) {
      mapping.deleteKeyValue(keyValue);
      for (YAMLKeyValue value : mapping.getKeyValues()) {
        if (value.getValue() != null) {
          return;
        }
      }
      YAMLKeyValue parentKeyValue = PsiTreeUtil.getParentOfType(mapping, YAMLKeyValue.class);
      YAMLMapping parentMapping = PsiTreeUtil.getParentOfType(parentKeyValue, YAMLMapping.class);
      mapping.delete();
      if (parentMapping != null) {
        deleteKeyValuesRecursively(parentMapping, parentKeyValue);
      }
    }
  }
}
