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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.PropertiesHighlighter;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.microservices.jvm.config.ConfigKeyParts;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformIcons;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cn.taketoday.assistant.CommonInfraModel;
import cn.taketoday.assistant.app.application.config.InfraPlaceholderReference;
import cn.taketoday.assistant.app.application.metadata.InfraConfigKeyDeclarationPsiElement;
import cn.taketoday.assistant.model.config.ConfigurationValueResult;
import cn.taketoday.assistant.model.config.InfraConfigValueSearcher;
import cn.taketoday.assistant.model.utils.InfraModelService;
import cn.taketoday.assistant.model.values.InfraPlaceholderReferenceResolver;
import cn.taketoday.lang.Nullable;

public class InfraApplicationPropertiesPlaceholderReferenceResolver implements InfraPlaceholderReferenceResolver {

  public static final LookupElementRenderer<LookupElement> LOOKUP_ELEMENT_RENDERER = new LookupElementRenderer<LookupElement>() {
    public void renderElement(LookupElement element, LookupElementPresentation presentation) {
      IProperty property = (IProperty) element.getObject();
      presentation.setIcon(PlatformIcons.PROPERTY_ICON);
      String key = StringUtil.notNullize(property.getUnescapedKey());
      presentation.setItemText(key);
      TextAttributes attrs = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(PropertiesHighlighter.PropertiesComponent.PROPERTY_VALUE.getTextAttributesKey());
      presentation.setTailText("=" + property.getValue(), attrs.getForegroundColor());
      PsiFile psiFile = ((PsiElement) element.getObject()).getContainingFile();
      if (psiFile != null) {
        presentation.setTypeText(psiFile.getOriginalFile().getName(), psiFile.getOriginalFile().getIcon(0));
      }
    }
  };

  public Pair<List<PsiElement>, List<VirtualFile>> resolve(PsiReference reference) {
    List<VirtualFile> configFiles = InfraPlaceholderReference.findConfigFiles(reference, PropertiesFileType.INSTANCE);
    if (configFiles.isEmpty()) {
      return Pair.create(Collections.emptyList(), Collections.emptyList());
    }
    PsiManager psiManager = reference.getElement().getManager();
    String key = reference.getCanonicalText();
    SmartList smartList = new SmartList();
    for (VirtualFile configFile : configFiles) {
      PropertiesFile propertiesFile = ObjectUtils.tryCast(psiManager.findFile(configFile), PropertiesFile.class);
      if (propertiesFile != null) {
        List<IProperty> properties = propertiesFile.findPropertiesByKey(key);
        for (IProperty iProperty : properties) {
          if (iProperty instanceof PropertyImpl) {
            PropertyKeyImpl propertyKey = InfraApplicationPropertiesUtil.getPropertyKey((PropertyImpl) iProperty);
            PsiElement resolvedKey = propertyKey == null ? null : InfraPlaceholderReference.resolveKeyReference(propertyKey);
            smartList.add(Objects.requireNonNullElseGet(resolvedKey, () -> (PropertyImpl) iProperty));
          }
        }
      }
    }
    return Pair.create(smartList, configFiles);
  }

  public List<LookupElement> getVariants(PsiReference reference) {
    List<VirtualFile> configFiles = InfraPlaceholderReference.findConfigFiles(reference, PropertiesFileType.INSTANCE);
    if (configFiles.isEmpty()) {
      return Collections.emptyList();
    }
    PsiManager psiManager = reference.getElement().getManager();
    SmartList smartList = new SmartList();
    for (VirtualFile configFile : configFiles) {
      PropertiesFile propertiesFile = ObjectUtils.tryCast(psiManager.findFile(configFile), PropertiesFile.class);
      if (propertiesFile != null) {
        List<IProperty> properties = propertiesFile.getProperties();
        for (IProperty property : properties) {
          String key = property.getKey();
          if (key != null) {
            LookupElementBuilder builder = LookupElementBuilder.create(property, key).withRenderer(LOOKUP_ELEMENT_RENDERER);
            smartList.add(builder);
          }
        }
      }
    }
    return smartList;
  }

  public boolean isProperty(PsiElement psiElement) {
    return psiElement instanceof InfraConfigKeyDeclarationPsiElement;
  }

  @Nullable
  public String getPropertyValue(PsiReference reference, PsiElement psiElement) {
    Module module;
    if (psiElement instanceof IProperty) {
      return ((IProperty) psiElement).getValue();
    }
    if (!(psiElement instanceof InfraConfigKeyDeclarationPsiElement) || (module = ModuleUtilCore.findModuleForPsiElement(reference.getElement())) == null) {
      return null;
    }
    String configKey = ((InfraConfigKeyDeclarationPsiElement) psiElement).getName();
    String referenceText = reference.getCanonicalText();
    ConfigKeyParts parts = ConfigKeyParts.splitToParts(configKey, referenceText, false);
    String keyIndex = parts == null ? null : parts.getKeyIndex();
    String keyProperty = parts == null ? null : parts.getKeyProperty();
    CommonInfraModel model = InfraModelService.of().getModel(reference.getElement());
    InfraConfigValueSearcher searcher = InfraConfigValueSearcher.productionForProfiles(module, configKey, model.getActiveProfiles(), keyIndex, keyProperty);
    Ref<String> valueText = Ref.create();
    Processor<ConfigurationValueResult> findValueTextProcessor = result -> {
      String text = result.getValueText();
      if (text == null) {
        return true;
      }
      String keyText = result.getMetaConfigKeyReference().getKeyText();
      String keyIndexText = result.getKeyIndexText();
      if (referenceText.equals(keyText) || (keyIndexText != null && referenceText.equals(keyText + "[" + result.getKeyIndexText() + "]"))) {
        valueText.set(text);
        return false;
      }
      return true;
    };
    searcher.process(findValueTextProcessor);
    return valueText.get();
  }
}
