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
import com.intellij.codeInsight.CharTailType;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.microservices.jvm.config.ConfigKeyParts;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;

class InfraApplicationPropertiesKeyReferenceProvider extends PsiReferenceProvider {

  InfraApplicationPropertiesKeyReferenceProvider() {
  }

  public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
    PropertyImpl property = PsiTreeUtil.getParentOfType(element, PropertyImpl.class);
    if (property == null) {
      return PsiReference.EMPTY_ARRAY;
    }
    return new PsiReference[] { new PropertyKeyMetaConfigKeyReference(element, property) };
  }

  private static final class PropertyKeyMetaConfigKeyReference extends MetaConfigKeyReference<PsiElement> {
    private static final TailType MAP_DOT_TAIL = new CharTailType('.') {
      public int processTail(Editor editor, int tailOffset) {
        int offset = super.processTail(editor, tailOffset);
        AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
        return offset;
      }
    };
    private final PropertyImpl myProperty;

    private PropertyKeyMetaConfigKeyReference(PsiElement element, PropertyImpl property) {
      super(InfraApplicationMetaConfigKeyManager.of(), element, property.getName());
      this.myProperty = property;
    }

    protected TextRange calculateDefaultRangeInElement() {
      TextRange defaultRange = super.calculateDefaultRangeInElement();
      MetaConfigKey configKey = getResolvedKey();
      if (configKey != null && !configKey.isAccessType(MetaConfigKey.AccessType.NORMAL)) {
        String keyText = defaultRange.substring(this.myElement.getText());
        ConfigKeyParts parts = ConfigKeyParts.splitToParts(configKey, keyText, false);
        if (parts != null) {
          return TextRange.allOf(parts.getConfigKey());
        }
      }
      return defaultRange;
    }

    public String getReferenceDisplayText() {
      return this.myProperty.getText();
    }

    public Object[] getVariants() {
      Set<String> existingKeys = ContainerUtil.map2Set(getExistingProperties(), IProperty::getKey);
      char delimiterChar = PropertiesCodeStyleSettings.getInstance(this.myElement.getProject()).getDelimiter();
      CharTailType defaultDelimiterType = new CharTailType(delimiterChar) {
        public int processTail(Editor editor, int tailOffset) {
          int offset = super.processTail(editor, tailOffset);
          AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
          return offset;
        }
      };
      List<? extends MetaConfigKey> configKeys = InfraApplicationMetaConfigKeyManager.of().getAllMetaConfigKeys(getElement());
      List<LookupElement> result = new ArrayList<>(configKeys.size());
      for (MetaConfigKey configKey : configKeys) {
        String name = configKey.getName();
        if (!existingKeys.contains(name)) {
          LookupElementBuilder builder = configKey.getPresentation().getLookupElement();
          TailTypeDecorator<LookupElementBuilder> tailTypeDecorator = TailTypeDecorator.withTail(builder,
                  configKey.isAccessType(MetaConfigKey.AccessType.MAP_GROUP) ? MAP_DOT_TAIL : defaultDelimiterType);
          result.add(configKey.getPresentation().tuneLookupElement(tailTypeDecorator));
        }
      }
      return result.toArray(LookupElement.EMPTY_ARRAY);
    }

    private List<? extends IProperty> getExistingProperties() {
      Module module = ModuleUtilCore.findModuleForPsiElement(this.myProperty);
      if (InfraApplicationPropertiesUtil.isSupportMultiDocuments(module)) {
        return InfraApplicationPropertiesUtil.getDocument(this.myProperty);
      }
      return this.myProperty.getPropertiesFile().getProperties();
    }
  }
}
