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

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.json.JsonUtil;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.InfraAppBundle;
import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.assistant.app.application.metadata.InfraMetadataConstant;

class InfraAdditionalConfigMetaConfigKeyReference extends MetaConfigKeyReference<PsiElement> implements EmptyResolveMessageProvider {
  private final Mode myMode;

  enum Mode {
    HINTS("Hint"),
    REPLACEMENT("Replacement");

    private final String myDisplayName;

    Mode(String displayName) {
      this.myDisplayName = displayName;
    }
  }

  InfraAdditionalConfigMetaConfigKeyReference(PsiElement element, Mode mode) {
    super(InfraApplicationMetaConfigKeyManager.of(), element, ElementManipulators.getValueText(element));
    this.myMode = mode;
  }

  protected TextRange calculateDefaultRangeInElement() {
    MetaConfigKey key = getResolvedKey();
    if (key != null && key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
      String keyName = key.getName();
      return TextRange.allOf(keyName).shiftRight(ElementManipulators.getOffsetInElement(getElement()));
    }
    return super.calculateDefaultRangeInElement();
  }

  protected List<MetaConfigKey> getAllKeys(String keyText) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) {
      List<MetaConfigKey> emptyList = Collections.emptyList();
      return emptyList;
    }
    List<? extends MetaConfigKey> allKeys = InfraApplicationMetaConfigKeyManager.of().getAllMetaConfigKeys(module);
    MetaConfigKeyManager.ConfigKeyNameBinder binder = InfraApplicationMetaConfigKeyManager.of().getConfigKeyNameBinder(module);
    List<MetaConfigKey> filter = ContainerUtil.filter(allKeys, configKey -> {
      String keyName = configKey.getName();
      if (configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
        if (this.myMode != Mode.HINTS) {
          return binder.bindsTo(configKey, keyText);
        }
        return keyText.equals(keyName + ".keys") || keyText.equals(keyName + ".values");
      }
      return keyText.equals(keyName);
    });
    return filter;
  }

  public String getReferenceDisplayText() {
    String str = this.myMode.myDisplayName + " for " + ElementManipulators.getValueText(getElement());
    return str;
  }

  public Object[] getVariants() {
    Set<String> existingKeys = this.myMode == Mode.HINTS ? collectExistingKeys() : Collections.emptySet();
    List<? extends MetaConfigKey> allKeys = InfraApplicationMetaConfigKeyManager.of().getAllMetaConfigKeys(getElement());
    List<LookupElement> allVariants = new ArrayList<>(allKeys.size());
    for (MetaConfigKey key : allKeys) {
      MetaConfigKey.MetaConfigKeyPresentation presentation = key.getPresentation();
      if (this.myMode == Mode.REPLACEMENT) {
        allVariants.add(presentation.tuneLookupElement(presentation.getLookupElement()));
      }
      else if (key.isAccessType(MetaConfigKey.AccessType.MAP_GROUP)) {
        String dotKeysName = key.getName() + ".keys";
        if (!existingKeys.contains(dotKeysName)) {
          allVariants.add(presentation.tuneLookupElement(presentation.getLookupElement(dotKeysName)));
        }
        String dotValuesName = key.getName() + ".values";
        if (!existingKeys.contains(dotValuesName)) {
          allVariants.add(presentation.tuneLookupElement(presentation.getLookupElement(dotValuesName)));
        }
      }
      else if (!existingKeys.contains(key.getName())) {
        allVariants.add(presentation.tuneLookupElement(presentation.getLookupElement()));
      }
    }
    return allVariants.toArray(LookupElement.EMPTY_ARRAY);
  }

  public String getUnresolvedMessagePattern() {
    return InfraAppBundle.message("additional.config.unresolved.config.key.reference", getValue());
  }

  private Set<String> collectExistingKeys() {
    JsonObject rootObject;
    JsonArray hintsArray;
    JsonStringLiteral literal;
    PsiFile containingFile = getElement().getContainingFile();
    if ((containingFile instanceof JsonFile jsonFile)
            && (rootObject = ObjectUtils.tryCast(jsonFile.getTopLevelValue(), JsonObject.class)) != null
            && (hintsArray = JsonUtil.getPropertyValueOfType(rootObject, InfraMetadataConstant.HINTS, JsonArray.class)) != null) {
      Set<String> result = new HashSet<>();
      for (JsonValue value : hintsArray.getValueList()) {
        JsonObject entry = ObjectUtils.tryCast(value, JsonObject.class);
        if (entry != null && (literal = JsonUtil.getPropertyValueOfType(entry, InfraMetadataConstant.NAME, JsonStringLiteral.class)) != null) {
          ContainerUtil.addIfNotNull(result, literal.getValue());
        }
      }
      return result;
    }
    return Collections.emptySet();
  }
}
