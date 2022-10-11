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

import com.intellij.microservices.jvm.config.ConfigKeyDocumentationProviderBase;
import com.intellij.microservices.jvm.config.MetaConfigKeyManager;
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.Nls;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import cn.taketoday.assistant.app.application.metadata.InfraApplicationMetaConfigKeyManager;
import cn.taketoday.lang.Nullable;

public class InfraApplicationYamlDocumentationProvider extends ConfigKeyDocumentationProviderBase {

  protected MetaConfigKeyManager getConfigManager() {
    return InfraApplicationMetaConfigKeyManager.of();
  }

  @Nls
  @Nullable
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if ((element instanceof YAMLKeyValue) && InfraApplicationYamlUtil.isInsideApplicationYamlFile(element)) {
      return StringUtil.wrapWithDoubleQuote(((YAMLKeyValue) element).getValueText());
    }
    return super.getQuickNavigateInfo(element, originalElement);
  }

  @Nullable
  protected String getConfigKey(PsiElement configKeyElement) {
    YAMLKeyValue yamlKeyValue;
    if (!(configKeyElement instanceof LeafPsiElement)) {
      if ((configKeyElement instanceof YAMLKeyValue) && InfraApplicationYamlUtil.isInsideApplicationYamlFile(configKeyElement)) {
        return ConfigYamlUtils.getQualifiedConfigKeyName((YAMLKeyValue) configKeyElement);
      }
      return null;
    }
    else if (((LeafPsiElement) configKeyElement).getElementType() != YAMLTokenTypes.SCALAR_KEY || !InfraApplicationYamlUtil.isInsideApplicationYamlFile(
            configKeyElement) || (yamlKeyValue = PsiTreeUtil.getParentOfType(configKeyElement, YAMLKeyValue.class)) == null) {
      return null;
    }
    else {
      return ConfigYamlUtils.getQualifiedConfigKeyName(yamlKeyValue);
    }
  }
}
