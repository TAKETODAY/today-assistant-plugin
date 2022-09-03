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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlAccessor;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.app.application.config.InfraPlaceholderReference;
import cn.taketoday.assistant.model.values.InfraPlaceholderReferenceResolver;
import cn.taketoday.lang.Nullable;

public class InfraApplicationYamlPlaceholderReferenceResolver implements InfraPlaceholderReferenceResolver {

  public Pair<List<PsiElement>, List<VirtualFile>> resolve(PsiReference reference) {
    List<VirtualFile> configFiles = InfraPlaceholderReference.findConfigFiles(reference, YAMLFileType.YML);
    if (configFiles.isEmpty()) {
      return Pair.create(Collections.emptyList(), Collections.emptyList());
    }
    PsiManager psiManager = reference.getElement().getManager();
    String key = reference.getCanonicalText();
    SmartList<PsiElement> smartList = new SmartList<>();
    for (VirtualFile configFile : configFiles) {
      YAMLFile yamlFile = ObjectUtils.tryCast(psiManager.findFile(configFile), YAMLFile.class);
      if (yamlFile != null) {
        List<YAMLDocument> yamlDocuments = yamlFile.getDocuments();
        for (YAMLDocument yamlDocument : yamlDocuments) {
          YAMLPsiElement findKey = findKey(key, yamlDocument);
          if (findKey instanceof YAMLKeyValue yamlKeyValue) {
            if (yamlKeyValue.getValue() instanceof YAMLScalar) {
              PsiElement resolvedKey = InfraPlaceholderReference.resolveKeyReference(yamlKeyValue);
              smartList.add(Objects.requireNonNullElse(resolvedKey, yamlKeyValue));
            }
          }
          else if (findKey instanceof YAMLSequenceItem yamlSequenceItem) {
            if (yamlSequenceItem.getValue() instanceof YAMLScalar) {
              PsiElement parent = yamlSequenceItem.getParent();
              PsiElement yamlKeyValue2 = parent == null ? null : parent.getParent();
              PsiElement resolvedKey2 = yamlKeyValue2 == null ? null : InfraPlaceholderReference.resolveKeyReference(yamlKeyValue2);
              smartList.add(Objects.requireNonNullElse(resolvedKey2, yamlSequenceItem));
            }
          }
        }
      }
    }
    return Pair.create(smartList, configFiles);
  }

  public List<LookupElement> getVariants(PsiReference reference) {
    List<VirtualFile> configFiles = InfraPlaceholderReference.findConfigFiles(reference, YAMLFileType.YML);
    if (configFiles.isEmpty()) {
      return Collections.emptyList();
    }
    PsiManager psiManager = reference.getElement().getManager();
    SmartList<LookupElement> smartList = new SmartList<>();
    for (VirtualFile configFile : configFiles) {
      YAMLFile yamlFile = ObjectUtils.tryCast(psiManager.findFile(configFile), YAMLFile.class);
      if (yamlFile != null) {
        List<YAMLDocument> yamlDocuments = yamlFile.getDocuments();
        for (YAMLDocument yamlDocument : yamlDocuments) {
          Processor<YAMLKeyValue> processor = value -> {
            if (value.getValue() instanceof YAMLScalar) {
              String qualifiedKey = ConfigYamlUtils.getQualifiedConfigKeyName(value);
              smartList.add(LookupElementBuilder.create(value, qualifiedKey).withRenderer(ConfigYamlUtils.getYamlPlaceholderLookupRenderer()));
              return true;
            }
            else if (value.getValue() instanceof YAMLSequence yamlSequence) {
              String qualifiedKey2 = ConfigYamlUtils.getQualifiedConfigKeyName(value);
              List<YAMLSequenceItem> items = yamlSequence.getItems();
              for (YAMLSequenceItem item : items) {
                if (item.getValue() instanceof YAMLScalar) {
                  smartList.add(LookupElementBuilder.create(item, qualifiedKey2 + "[" + item.getItemIndex() + "]").withRenderer(ConfigYamlUtils.getYamlPlaceholderLookupRenderer()));
                }
              }
              return true;
            }
            else {
              return true;
            }
          };
          ConfigYamlAccessor.processAllKeys(yamlDocument, processor, false);
        }
      }
    }
    return smartList;
  }

  public boolean isProperty(PsiElement psiElement) {
    return (psiElement instanceof YAMLKeyValue) || (psiElement instanceof YAMLSequenceItem);
  }

  @Nullable
  public String getPropertyValue(PsiReference reference, PsiElement psiElement) {
    if (psiElement instanceof YAMLKeyValue yamlKeyValue) {
      YAMLValue value = yamlKeyValue.getValue();
      if (value instanceof YAMLScalar yamlScalar) {
        return yamlScalar.getTextValue();
      }
      return null;
    }
    else if (psiElement instanceof YAMLSequenceItem yamlSequenceItem) {
      YAMLValue value2 = yamlSequenceItem.getValue();
      if (!(value2 instanceof YAMLScalar yamlScalar)) {
        return null;
      }
      return yamlScalar.getTextValue();
    }
    else {
      return null;
    }
  }

  @Nullable
  private static YAMLPsiElement findKey(String qualifiedKey, YAMLDocument yamlDocument) {
    YAMLValue value;
    int indexStart;
    if (StringUtil.isEmptyOrSpaces(qualifiedKey)) {
      return null;
    }
    YAMLValue searchElement = yamlDocument.getTopLevelValue();
    List<String> key = StringUtil.split(qualifiedKey, ".");
    for (int i = 0; i < key.size(); i++) {
      if (!(searchElement instanceof YAMLMapping yamlMapping)) {
        return null;
      }
      String subKey = key.get(i);
      int index = -1;
      if (subKey.endsWith("]") && (indexStart = subKey.indexOf(91)) >= 0) {
        try {
          index = Integer.parseInt(subKey.substring(indexStart + 1, subKey.length() - 1));
          subKey = subKey.substring(0, indexStart);
        }
        catch (NumberFormatException e) {
        }
      }
      YAMLKeyValue child = yamlMapping.getKeyValueByKey(subKey);
      if (child == null) {
        return null;
      }
      if (index >= 0) {
        YAMLValue value2 = child.getValue();
        if (!(value2 instanceof YAMLSequence yamlSequence)) {
          return null;
        }
        List<YAMLSequenceItem> items = yamlSequence.getItems();
        if (items.size() <= index) {
          return null;
        }
        YAMLSequenceItem item = items.get(index);
        if (i + 1 == key.size()) {
          return item;
        }
        value = item.getValue();
      }
      else if (i + 1 == key.size()) {
        return child;
      }
      else {
        value = child.getValue();
      }
      searchElement = value;
    }
    throw new IllegalStateException("Should have returned from the loop '" + qualifiedKey + "'");
  }
}
