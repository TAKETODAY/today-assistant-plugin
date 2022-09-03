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

package cn.taketoday.assistant.app;

import com.intellij.icons.AllIcons.FileTypes;
import com.intellij.ide.IconProvider;
import com.intellij.json.psi.JsonFile;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.microservices.jvm.config.MetaConfigKey;
import com.intellij.microservices.jvm.config.MetaConfigKeyReference;
import com.intellij.openapi.util.Iconable.IconFlags;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.ui.LayeredIcon;

import javax.swing.Icon;

import cn.taketoday.assistant.Icons;
import cn.taketoday.assistant.InfraLibraryUtil;
import cn.taketoday.assistant.app.application.metadata.additional.InfraAdditionalConfigUtils;
import cn.taketoday.assistant.util.InfraUtils;
import cn.taketoday.lang.Nullable;
import icons.JavaUltimateIcons.Javaee;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/31 02:05
 */
public class InfraIconProvider extends IconProvider {
  public static final LayeredIcon ADDITIONAL_JSON_ICON;
  public static final LayeredIcon BANNER_ICON;

  @Nullable
  public Icon getIcon(PsiElement element, @IconFlags int flags) {
    if (InfraUtils.hasFacets(element.getProject())
            && InfraLibraryUtil.hasFrameworkLibrary(element.getProject())) {
      if (element instanceof PropertyImpl property) {
        if (InfraConfigurationFileService.of().isApplicationConfigurationFile(property.getContainingFile())) {
          ASTNode keyNode = property.getKeyNode();
          if (keyNode == null) {
            return null;
          }

          MetaConfigKey configKey = MetaConfigKeyReference.getResolvedMetaConfigKey(keyNode.getPsi());
          if (configKey != null) {
            return configKey.getPresentation().getIcon();
          }
        }

        return null;
      }
      else if (element instanceof PropertiesFile) {
        return InfraConfigurationFileService.of().getApplicationConfigurationFileIcon(((PropertiesFile) element).getContainingFile());
      }
      else {
        if (element instanceof JsonFile jsonFile) {
          if (InfraAdditionalConfigUtils.isAdditionalMetadataFile(jsonFile)) {
            return ADDITIONAL_JSON_ICON;
          }
        }
        else if (element instanceof PsiClass psiClass) {
          if (InfraUtils.isBeanCandidateClass(psiClass)
                  && InfraApplicationService.of().isInfraApplication(psiClass)) {
            return new LayeredIcon(PsiClassImplUtil.getClassIcon(flags, psiClass), Icons.TodayOverlay);
          }
        }
        // FIXME BANNER_ICON
//        else if (element instanceof PsiPlainTextFile && Holder.PATTERN.accepts(element)) {
//          return BANNER_ICON;
//        }

        return null;
      }
    }
    else {
      return null;
    }
  }

  static {
    ADDITIONAL_JSON_ICON = new LayeredIcon(cn.taketoday.assistant.Icons.Today, Javaee.InheritedAttributeOverlay);
    BANNER_ICON = new LayeredIcon(FileTypes.Text, Icons.TodayOverlay);
  }
}
