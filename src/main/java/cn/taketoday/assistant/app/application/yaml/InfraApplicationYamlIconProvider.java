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

import com.intellij.ide.IconProvider;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import javax.swing.Icon;

import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.InfraConfigurationFileService;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;

public class InfraApplicationYamlIconProvider extends IconProvider {

  @Nullable
  public Icon getIcon(PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof YAMLFile) {
      if (!InfraUtils.hasFacets(element.getProject()) || !InfraLibraryUtil.hasFrameworkLibrary(element.getProject())) {
        return null;
      }
      return InfraConfigurationFileService.of().getApplicationConfigurationFileIcon((YAMLFile) element);
    }
    else if (element instanceof YAMLKeyValue keyValue) {
      PsiFile containingFile = element.getContainingFile();
      if (!(containingFile instanceof YAMLFile) || !InfraUtils.hasFacets(element.getProject()) || !InfraLibraryUtil.hasFrameworkLibrary(
              element.getProject()) || !InfraConfigurationFileService.of().isApplicationConfigurationFile(containingFile)) {
        return null;
      }
      MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(keyValue);
      if (configKey != null) {
        return configKey.getPresentation().getIcon();
      }
      return null;
    }
    else {
      return null;
    }
  }
}
