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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ui.ListEditForm;
import com.intellij.microservices.jvm.config.ConfigKeyPathReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.MicroservicesConfigBundle;
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.properties.IndexAccessTextProcessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.microservices.jvm.config.yaml.ShowDuplicateKeysQuickFix;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.FormBuilder;

import org.jdom.Element;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.app.application.config.InfraConfigFileHighlightingUtil;
import cn.taketoday.assistant.app.application.config.InfraReplacementTokenStorage;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public final class InfraApplicationYamlInspection extends LocalInspectionTool {

  public String replacementTokens = "@";
  final InfraReplacementTokenStorage myReplacementTokenStorage = new InfraReplacementTokenStorage();

  public InfraApplicationYamlInspection() {
    this.myReplacementTokenStorage.deserialize(this.replacementTokens);
  }

  List<Couple<String>> getReplacementTokens() {
    return this.myReplacementTokenStorage.getReplacementTokens();
  }

  public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof YAMLFile yamlFile)) {
      return null;
    }
    if (!InfraUtils.hasFacets(manager.getProject())
            || !InfraConfigurationFileService.of().isApplicationConfigurationFile(file)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
    InfraConfigFileHighlightingUtil configFileHighlightingUtil = new InfraConfigFileHighlightingUtil(holder);
    List<YAMLDocument> yamlDocuments = yamlFile.getDocuments();
    boolean hasMultipleDocuments = yamlDocuments.size() > 1;
    for (YAMLDocument document : yamlDocuments) {
      MultiMap<String, YAMLKeyValue> duplicates = new MultiMap<>();
      document.acceptChildren(new PsiRecursiveElementVisitor() {

        public void visitElement(PsiElement element) {
          YAMLKeyValue psiElement;
          PsiElement keyElement;
          super.visitElement(element);
          if (!(element instanceof YAMLKeyValue yamlKeyValue) || (keyElement = (psiElement = yamlKeyValue).getKey()) == null) {
            return;
          }
          PsiReference[] psiReferences = psiElement.getReferences();
          MetaConfigKeyReference<?> metaConfigKeyReference = (MetaConfigKeyReference) ContainerUtil.findInstance(psiReferences, MetaConfigKeyReference.class);
          boolean isConfigKeyPath = ConfigYamlUtils.isConfigKeyPath(metaConfigKeyReference);
          if (isConfigKeyPath) {
            MetaConfigKey configKey = metaConfigKeyReference.getResolvedKey();
            if (configKey != null) {
              highlightIndexAccessExpressions(holder, keyElement, configKey);
            }
            for (PsiReference reference : psiReferences) {
              if (((reference instanceof ConfigKeyPathReference) || (reference instanceof HintReferenceBase)) && !reference.isSoft() && reference.resolve() == null) {
                holder.registerProblem(reference, ProblemsHolder.unresolvedReferenceMessage(reference), ProblemHighlightType.ERROR);
              }
            }
          }
          YAMLValue value = yamlKeyValue.getValue();
          if (value instanceof YAMLScalar) {
            MicroservicesConfigUtils.highlightValueReferences(value, holder);
          }
          else if (value instanceof YAMLSequence sequence) {
            for (YAMLSequenceItem item : sequence.getItems()) {
              YAMLValue itemValue = item.getValue();
              if (itemValue instanceof YAMLScalar) {
                MicroservicesConfigUtils.highlightValueReferences(itemValue, holder);
              }
            }
          }
          if (isConfigKeyPath) {
            return;
          }
          MetaConfigKey configKey2 = metaConfigKeyReference != null ? metaConfigKeyReference.getResolvedKey() : null;
          if (configKey2 == null) {
            if (noValueOrScalar(value)) {
              String qualifiedConfigKeyName = ConfigYamlUtils.getQualifiedConfigKeyName(psiElement);
              if (hasMultipleDocuments && InfraMetadataConstant.INFRA_PROFILES_KEY.equals(qualifiedConfigKeyName)) {
                return;
              }
              configFileHighlightingUtil.highlightUnresolvedConfigKey(keyElement, psiElement, qualifiedConfigKeyName, isOnTheFly);
              return;
            }
            return;
          }
          if (configKey2.getDeprecation() != MetaConfigKey.Deprecation.NOT_DEPRECATED && (configKey2.isAccessType(MetaConfigKey.AccessType.MAP_GROUP) || !(value instanceof YAMLMapping))) {
            LocalQuickFix[] fixes = InfraApplicationYamlDeprecationFixFactory.getDeprecationFixes(keyElement, configKey2, isOnTheFly);
            configFileHighlightingUtil.highlightDeprecatedConfigKey(keyElement, configKey2, fixes);
          }
          if (!configKey2.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
            duplicates.putValue(configKey2.getName(), psiElement);
          }
          else if (noValueOrScalar(value)) {
            holder.registerProblem(keyElement, InfraAppBundle.message("application.config.missing.map.key"), ProblemHighlightType.ERROR);
          }
        }
      });
      for (Map.Entry<String, Collection<YAMLKeyValue>> entry : duplicates.entrySet()) {
        Collection<YAMLKeyValue> values = entry.getValue();
        if (values.size() != 1) {
          String configKey = entry.getKey();
          LocalQuickFix showDuplicatesFix = new ShowDuplicateKeysQuickFix(configKey, values);
          for (YAMLKeyValue keyValue : values) {
            holder.registerProblem(keyValue, MicroservicesConfigBundle.message("config.duplicate.key", configKey), showDuplicatesFix);
          }
        }
      }
    }
    return holder.getResultsArray();
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

  private static boolean noValueOrScalar(PsiElement valueElement) {
    return valueElement == null || (valueElement instanceof YAMLScalar) || (valueElement instanceof YAMLSequence);
  }

  private static void highlightIndexAccessExpressions(ProblemsHolder holder, PsiElement keyElement, MetaConfigKey configKey) {
    new IndexAccessTextProcessor(keyElement.getText(), configKey) {
      protected void onMissingClosingBracket(int startIdx) {
        holder.registerProblem(keyElement, InfraAppBundle.message("application.config.missing.closing.bracket"), ProblemHighlightType.ERROR, TextRange.from(startIdx, 1)
        );
      }

      protected void onMissingIndexValue(int startIdx) {
        holder.registerProblem(keyElement, InfraAppBundle.message("application.config.missing.index.value"), ProblemHighlightType.ERROR, TextRange.from(startIdx, 2)
        );
      }

      protected void onBracket(int startIdx) {
      }

      protected void onIndexValue(TextRange indexValueRange) {
      }

      protected void onIndexValueNotInteger(TextRange indexValueRange) {
      }
    }.process();
  }
}
