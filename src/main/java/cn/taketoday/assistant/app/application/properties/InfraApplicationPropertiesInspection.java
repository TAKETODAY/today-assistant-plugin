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

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ui.ListEditForm;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.microservices.jvm.config.ConfigKeyParts;
import com.intellij.microservices.jvm.config.ConfigKeyPathReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils;
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.FormBuilder;

import org.gradle.internal.impldep.org.jetbrains.annotations.Nls;
import org.jdom.Element;

import java.util.List;

import javax.swing.JComponent;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.config.InfraConfigFileHighlightingUtil;
import cn.taketoday.assistant.app.application.config.InfraReplacementTokenStorage;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraApplicationPropertiesInspection extends LocalInspectionTool {

  public String replacementTokens = "@";
  final InfraReplacementTokenStorage myReplacementTokenStorage = new InfraReplacementTokenStorage();

  public InfraApplicationPropertiesInspection() {
    this.myReplacementTokenStorage.deserialize(this.replacementTokens);
  }

  public List<Couple<String>> getReplacementTokens() {
    return this.myReplacementTokenStorage.getReplacementTokens();
  }

  public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
    PropertyImpl property;
    PropertyKeyImpl propertyKey;
    PsiReference[] references;
    ConfigKeyParts keyParts;
    if (!(file instanceof PropertiesFile propertiesFile)) {
      return null;
    }
    if (!InfraUtils.hasFacets(manager.getProject()) || !InfraConfigurationFileService.of().isApplicationConfigurationFile(file)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
    InfraConfigFileHighlightingUtil configFileHighlightingUtil = new InfraConfigFileHighlightingUtil(holder);
    for (IProperty iproperty : propertiesFile.getProperties()) {
      ProgressManager.checkCanceled();
      if ((iproperty instanceof PropertyImpl propertyImpl)
              && (propertyKey = InfraApplicationPropertiesUtil.getPropertyKey((property = propertyImpl))) != null) {
        MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(propertyKey);
        if (configKey == null) {
          String keyName = property.getName();
          configFileHighlightingUtil.highlightUnresolvedConfigKey(propertyKey, property, keyName, isOnTheFly);
        }
        else {
          if (configKey.getDeprecation() != MetaConfigKey.Deprecation.NOT_DEPRECATED) {
            configFileHighlightingUtil.highlightDeprecatedConfigKey(propertyKey, configKey, getDeprecationFix(property, configKey));
          }
          if (configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)
                  && ((keyParts = ConfigKeyParts.splitToParts(configKey, propertyKey.getText(), false)) == null || keyParts.getKeyIndex() == null)) {
            holder.registerProblem(propertyKey, InfraAppBundle.message("application.config.missing.map.key"), ProblemHighlightType.ERROR);
          }
          else {
            highlightIndexAccessExpressions(holder, propertyKey, configKey);
            if (MetaConfigKey.MAP_OR_INDEXED_WITHOUT_KEY_HINTS_CONDITION.value(configKey)) {
              for (PsiReference reference : propertyKey.getReferences()) {
                if ((reference instanceof ConfigKeyPathReference) && !reference.isSoft() && reference.resolve() == null) {
                  holder.registerProblem(reference, ProblemsHolder.unresolvedReferenceMessage(reference), ProblemHighlightType.ERROR);
                }
              }
            }
            PropertyValueImpl valueElement = InfraApplicationPropertiesUtil.getPropertyValue(property);
            if (valueElement != null) {
              MicroservicesConfigUtils.highlightValueReferences(valueElement, holder);
            }
          }
        }
      }
    }
    return holder.getResultsArray();
  }

  private static void highlightIndexAccessExpressions(ProblemsHolder holder, PropertyKeyImpl propertyKey, MetaConfigKey configKey) {
    new IndexAccessTextProcessor(propertyKey.getText(), configKey) {
      protected void onMissingClosingBracket(int startIdx) {
        holder.registerProblem(propertyKey, InfraAppBundle.message("application.config.missing.closing.bracket"), ProblemHighlightType.ERROR, TextRange.from(startIdx, 1)
        );
      }

      protected void onMissingIndexValue(int startIdx) {
        holder.registerProblem(propertyKey, InfraAppBundle.message("application.config.missing.index.value"), ProblemHighlightType.ERROR, TextRange.from(startIdx, 2)
        );
      }

      protected void onBracket(int startIdx) {
      }

      protected void onIndexValue(TextRange indexValueRange) {
      }

      protected void onIndexValueNotInteger(TextRange indexValueRange) {
        holder.registerProblem(propertyKey, InfraAppBundle.message("application.config.non.integer.index"), ProblemHighlightType.ERROR, indexValueRange);
      }
    }.process();
  }

  private static LocalQuickFix[] getDeprecationFix(PropertyImpl property, MetaConfigKey configKey) {
    String replacement = configKey.getDeprecation().getReplacement();
    return replacement == null ? LocalQuickFix.EMPTY_ARRAY : new LocalQuickFix[] { new LocalQuickFixAndIntentionActionOnPsiElement(property) {

      public String getText() {
        String message = InfraAppBundle.message("application.config.replacement.quick.fix", replacement);
        return message;
      }

      public void invoke(Project project, PsiFile file, @Nullable Editor editor, PsiElement startElement, PsiElement endElement) {
        PropertyValueImpl value;
        PropertyImpl property2 = (PropertyImpl) startElement;
        if (configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
          PropertyKeyImpl propertyKey = InfraApplicationPropertiesUtil.getPropertyKey(property2);
          MetaConfigKeyReference<?> reference = (MetaConfigKeyReference) ContainerUtil.findInstance(propertyKey.getReferences(), MetaConfigKeyReference.class);
          String originalKeyText = property2.getKey();
          String originalMapKey = originalKeyText.substring(reference.getRangeInElement().getEndOffset());
          property2.setName(replacement + originalMapKey);
        }
        else {
          property2.setName(replacement);
        }
        Module module = ModuleUtilCore.findModuleForPsiElement(property2);
        MetaConfigKey replacementMetaConfigKey = InfraApplicationMetaConfigKeyManager.getInstance().findApplicationMetaConfigKey(module, property2.getKey());
        if (replacementMetaConfigKey == null || replacementMetaConfigKey.getType() == null || configKey.getType() == null || configKey.getType()
                .isAssignableFrom(replacementMetaConfigKey.getType()) || (value = InfraApplicationPropertiesUtil.getPropertyValue(
                property2)) == null) {
          return;
        }
        if (editor == null) {
          property2.setValue("");
          return;
        }
        editor.getCaretModel().moveToOffset(value.getTextOffset());
        property2.setValue("");
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      }

      @Nls

      public String getFamilyName() {
        String message = InfraAppBundle.message("application.config.replacement.quick.fix.family.name");
        return message;
      }
    } };
  }

  public void readSettings(Element element) throws InvalidDataException {
    super.readSettings(element);
    this.myReplacementTokenStorage.deserialize(this.replacementTokens);
  }

  public void writeSettings(Element element) throws WriteExternalException {
    this.replacementTokens = this.myReplacementTokenStorage.serialize();
    super.writeSettings(element);
  }

  @Nullable
  public JComponent createOptionsPanel() {
    ListEditForm form = new ListEditForm(InfraAppBundle.message("infra.replacement.token.column.name"),
            InfraAppBundle.message("infra.replacement.token.label"), this.myReplacementTokenStorage.getTokens());
    return FormBuilder.createFormBuilder().addComponentFillVertically(form.getContentPanel(), 0).getPanel();
  }
}
