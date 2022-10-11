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
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.icons.AllIcons;
import com.intellij.microservices.jvm.config.ConfigKeyPathArbitraryEntryKeyReference;
import com.intellij.microservices.jvm.config.ConfigKeyPathBeanPropertyReference;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.microservices.jvm.config.hints.HintReferenceBase;
import com.intellij.microservices.jvm.config.yaml.ConfigKeyPathYamlContext;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtilEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;

import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.completion.YamlKeyCompletionInsertHandler;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.config.InfraHintReferencesProvider;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;
import cn.taketoday.lang.Nullable;

class InfraApplicationYamlKeyCompletionProvider extends CompletionProvider<CompletionParameters> {
  private static final Key<String> CONFIG_KEY = Key.create("ymlConfigKey");
  private static final Key<YAMLKeyValue> NEW_CONFIG_KEY = Key.create("ymlNewConfigKey");
  private static final InsertHandler<LookupElementDecorator<LookupElement>> INSERT_HANDLER = new YamlKeyCompletionInsertHandler<>() {

    public YAMLKeyValue createNewEntry(YAMLDocument document, LookupElementDecorator<LookupElement> item, @Nullable YAMLKeyValue parent) {
      String qualifiedKey = item.getCopyableUserData(CONFIG_KEY);
      ConfigYamlAccessor accessor = new ConfigYamlAccessor(document, InfraApplicationMetaConfigKeyManager.of());
      YAMLKeyValue keyValue = accessor.findExistingKey(qualifiedKey);
      if (keyValue != null) {
        item.putUserData(NEW_CONFIG_KEY, keyValue);
        return keyValue;
      }
      YAMLKeyValue keyValue2 = accessor.create(qualifiedKey);
      item.putUserData(NEW_CONFIG_KEY, keyValue2);
      return keyValue2;
    }

    public void handleInsert(InsertionContext context, LookupElementDecorator<LookupElement> item) {
      super.handleInsert(context, item);
      Editor editor = context.getEditor();
      String qualifiedKey = item.getCopyableUserData(CONFIG_KEY);
      if (StringUtil.endsWithChar(qualifiedKey, '.')) {
        YAMLKeyValue keyValue = item.getUserData(NEW_CONFIG_KEY);
        assert keyValue != null;
        PsiElement keyElement = keyValue.getKey();
        assert keyElement != null;
        int offset = keyElement.getNode().getTreeNext().getStartOffset() + 1;
        Document document = context.getDocument();
        CharSequence text = document.getCharsSequence();
        if (text.charAt(offset) == ' ') {
          document.deleteString(offset, offset + 1);
        }
        CaretModel caretModel = editor.getCaretModel();
        caretModel.moveToOffset(offset);
        int keyIndent = YAMLUtil.getIndentToThisElement(keyValue) + 2;
        String insertion = "\n" + StringUtil.repeatSymbol(' ', keyIndent);
        EditorModificationUtilEx.insertStringAtCaret(editor, insertion);
      }
      AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(editor);
    }
  };

  protected void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
    MetaConfigKey configKey;
    YAMLFile yamlFile = (YAMLFile) parameters.getOriginalFile();
    Module module = ModuleUtilCore.findModuleForPsiElement(yamlFile);
    if (module == null) {
      return;
    }
    InfraApplicationMetaConfigKeyManager keyManager = InfraApplicationMetaConfigKeyManager.of();
    List<? extends MetaConfigKey> configKeys = keyManager.getAllMetaConfigKeys(module);
    MetaConfigKeyManager.ConfigKeyNameBinder binder = keyManager.getConfigKeyNameBinder(module);
    PsiElement element = parameters.getPosition();
    PsiElement originalElement = CompletionUtil.getOriginalElement(element);
    PsiElement originalDocumentAnchor = originalElement != null ? originalElement : element.getContainingFile().getOriginalFile().findElementAt(parameters.getOffset());
    ConfigYamlAccessor accessor = new ConfigYamlAccessor(ObjectUtils.chooseNotNull(originalDocumentAnchor, element), keyManager);
    YAMLKeyValue parentYamlKeyValue = ConfigYamlUtils.getParentKeyValue(element, originalElement);
    ConfigYamlUtils.addCompletionAddIfNeeded(parameters, result);
    String fullParentConfigName = ConfigYamlUtils.getQualifiedConfigKeyName(parentYamlKeyValue);
    List<LookupElement> currentLineComponents = ConfigYamlUtils.getCurrentLineKeyComponents(ObjectUtils.chooseNotNull(originalElement, element), binder, fullParentConfigName, configKeys);
    if (!currentLineComponents.isEmpty()) {
      result.addAllElements(currentLineComponents);
    }
    String parentConfigKeyName = (parentYamlKeyValue == null || parameters.getInvocationCount() > 1) ? "" : fullParentConfigName;
    List<LookupElement> keyLookupElements = getConfigKeyElements(accessor, binder, parentConfigKeyName, configKeys);
    result.addAllElements(keyLookupElements);

    addSpringProfiles(yamlFile, accessor, result);

    if (parentYamlKeyValue != null
            && (configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(parentYamlKeyValue)) != null
            && !configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
      if (isInBrackets(element)) {
        return;
      }
      String currentLineKey = ConfigYamlUtils.getElementKeyText(ObjectUtils.chooseNotNull(originalElement, element));
      YAMLMapping parentMapping = PsiTreeUtil.getParentOfType(ObjectUtils.chooseNotNull(originalElement, element), YAMLMapping.class);
      YAMLMapping value = parentMapping != null ? parentMapping : (YAMLMapping) parentYamlKeyValue.getValue();
      Condition<String> newKeyCondition = key -> isNewKey(accessor, binder, currentLineKey, value, key);
      List<PsiReference> pathReferences = getConfigKeyPathReferences(parentYamlKeyValue, fullParentConfigName, configKey, element, context);
      int offset = parameters.getOffset() - element.getTextOffset();
      String completionPrefix = element.getText().substring(0, offset);
      result.addAllElements(getConfigKeyPathElements(parentYamlKeyValue, fullParentConfigName, configKey, pathReferences, offset, completionPrefix, newKeyCondition));
    }
    result.stopHere();
  }

  private static List<LookupElement> getConfigKeyElements(ConfigYamlAccessor accessor,
          MetaConfigKeyManager.ConfigKeyNameBinder binder, String parentConfigKeyName, List<? extends MetaConfigKey> configKeys) {
    List<LookupElement> keyLookupElements = new ArrayList<>();
    for (MetaConfigKey configKey : configKeys) {
      if (parentConfigKeyName.isEmpty() || binder.matchesPrefix(configKey, parentConfigKeyName)) {
        if (accessor.findExistingKey(configKey.getName()) == null) {
          LookupElementBuilder builder = configKey.getPresentation().getLookupElement();
          String name = configKey.getName();
          if (configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
            name = name + ".";
          }
          builder.putCopyableUserData(CONFIG_KEY, name);
          LookupElementDecorator<LookupElement> insertHandler = LookupElementDecorator.withInsertHandler(builder, INSERT_HANDLER);
          LookupElement lookupElement = configKey.getPresentation().tuneLookupElement(insertHandler);
          keyLookupElements.add(lookupElement);
        }
      }
    }
    return keyLookupElements;
  }

  private static void addSpringProfiles(YAMLFile yamlFile, ConfigYamlAccessor accessor, CompletionResultSet result) {
    if (yamlFile.getDocuments().size() > 1 && accessor.findExistingKey(InfraMetadataConstant.INFRA_PROFILES_KEY) == null) {
      LookupElementBuilder builder = LookupElementBuilder.create(InfraMetadataConstant.INFRA_PROFILES_KEY).withIcon(Icons.SpringProfile)
              .withTailText(InfraAppBundle.message("application.config.profiles.completion.tail"), true);
      builder.putCopyableUserData(CONFIG_KEY, builder.getLookupString());
      LookupElementDecorator<LookupElement> decorator = LookupElementDecorator.withInsertHandler(builder, INSERT_HANDLER);
      result.addElement(decorator);
    }
  }

  private static List<PsiReference> getConfigKeyPathReferences(YAMLKeyValue parentYamlKeyValue, String fullParentConfigName, MetaConfigKey configKey, PsiElement element, ProcessingContext context) {
    context.put(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY, configKey);
    context.put(InfraHintReferencesProvider.HINT_REFERENCES_CONFIG_KEY_TEXT, fullParentConfigName + ".");
    return InfraApplicationYamlKeyReferenceProvider.getReferences(parentYamlKeyValue, parentYamlKeyValue, element, context);
  }

  public static boolean isNewKey(ConfigYamlAccessor accessor, MetaConfigKeyManager.ConfigKeyNameBinder binder, String currentLineKey, YAMLValue searchElement, String key) {
    return (currentLineKey != null && binder.matchesPart(key, currentLineKey)) || accessor.findExistingKey(key, searchElement) == null;
  }

  private static List<LookupElement> getConfigKeyPathElements(
          YAMLKeyValue parentYamlKeyValue, String fullParentConfigName, MetaConfigKey configKey,
          List<PsiReference> references, int offset,
          String completionPrefix, Condition<String> newKeyCondition) {
    List<LookupElement> result = new ArrayList<>();
    for (PsiReference psiReference : references) {
      if (!(psiReference instanceof MetaConfigKeyReference) && psiReference.getRangeInElement().contains(offset)) {
        if (psiReference instanceof HintReferenceBase configKeyPathBeanPropertyReference) {
          ConfigKeyPathArbitraryEntryKeyReference entryKeyReference = ContainerUtil.findInstance(references, ConfigKeyPathArbitraryEntryKeyReference.class);
          String prefix = entryKeyReference != null ? ((ConfigKeyPathYamlContext) entryKeyReference.getContext()).getPrefix() : null;
          String prefix2 = prefix == null ? "" : prefix + ".";
          Set<String> currentLineHints = new HashSet<>();
          for (Object variant : configKeyPathBeanPropertyReference.getVariants()) {
            if (variant instanceof LookupElement lookupElement) {
              String lookupString = lookupElement.getLookupString();
              if (StringUtil.startsWithIgnoreCase(lookupString, prefix2)) {
                String key = lookupString.substring(prefix2.length());
                if (newKeyCondition.value(key)) {
                  lookupElement.putCopyableUserData(CONFIG_KEY, fullParentConfigName + "." + key);
                  LookupElementDecorator<LookupElement> withInsertHandler = LookupElementDecorator.withInsertHandler(lookupElement, INSERT_HANDLER);
                  result.add(withInsertHandler);
                }
                int index = key.indexOf(46);
                if (index >= 0) {
                  String currentLineComponent = key.substring(0, index);
                  if (!currentLineHints.contains(currentLineComponent) && newKeyCondition.value(currentLineComponent)) {
                    currentLineHints.add(currentLineComponent);
                  }
                }
              }
            }
          }
          for (String currentLineHint : currentLineHints) {
            result.add(LookupElementBuilder.create(currentLineHint).withIcon(AllIcons.Nodes.Property).withInsertHandler(ConfigYamlUtils.INSERT_COLON_AND_NEW_LINE_INSERT_HANDLER));
          }
        }
        else if (!(psiReference instanceof ConfigKeyPathBeanPropertyReference) || !((ConfigKeyPathBeanPropertyReference) psiReference).getContext().isFirst() || !configKey.isAccessType(
                MetaConfigKey.AccessType.INDEXED) || (parentYamlKeyValue.getValue() instanceof YAMLSequence)) {
          for (Object variant2 : psiReference.getVariants()) {
            if (variant2 instanceof LookupElement lookupElement) {
              String key = lookupElement.getLookupString();
              if (newKeyCondition.value(key)) {
                if (lookupElement instanceof LookupElementBuilder) {
                  int startOffset = psiReference.getRangeInElement().getStartOffset();
                  if (startOffset > 0 && startOffset <= completionPrefix.length()) {
                    String variantCompletionPrefix = completionPrefix.substring(0, startOffset);
                    lookupElement = ((LookupElementBuilder) lookupElement).withLookupString(variantCompletionPrefix + key).withInsertHandler(new InsertHandler<LookupElement>() {
                      public void handleInsert(InsertionContext context, LookupElement item) {

                        Editor editor = context.getEditor();
                        editor.getCaretModel().moveCaretRelatively(-1 * item.getLookupString().length(), 0, false, false, false);
                        EditorModificationUtilEx.insertStringAtCaret(editor, variantCompletionPrefix);
                        editor.getCaretModel().moveCaretRelatively(item.getLookupString().length(), 0, false, false, false);
                      }
                    });
                  }
                }

                result.add(lookupElement);
              }
            }
          }
        }
      }
    }
    return result;
  }

  private static boolean isInBrackets(PsiElement element) {
    if (element.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
      return false;
    }
    String text = element.getText();
    return (StringUtil.startsWith(text, "\"[") && StringUtil.endsWith(text, "]\"")) || (StringUtil.startsWith(text, "'[") && StringUtil.endsWith(text, "]'"));
  }
}
