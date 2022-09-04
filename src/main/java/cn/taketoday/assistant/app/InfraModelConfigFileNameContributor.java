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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.ContainerUtil;

import org.gradle.internal.impldep.org.jetbrains.annotations.Nls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import cn.taketoday.assistant.facet.InfraFacet;
import cn.taketoday.assistant.facet.InfraFileSet;
import cn.taketoday.assistant.facet.beans.CustomSetting;
import cn.taketoday.lang.Nullable;

public abstract class InfraModelConfigFileNameContributor {

  public static final ExtensionPointName<InfraModelConfigFileNameContributor> EP_NAME
          = ExtensionPointName.create("cn.taketoday.assistant.modelConfigFileNameContributor");

  private final CustomSettingDescriptor myCustomNameSettingDescriptor;
  private final CustomSettingDescriptor myCustomFilesSettingDescriptor;
  private final CustomizationPresentation myCustomizationPresentation;
  private final Icon myFileIcon;

  protected InfraModelConfigFileNameContributor(CustomSettingDescriptor customNameSettingDescriptor,
          CustomSettingDescriptor customFilesSettingDescriptor, CustomizationPresentation presentation, Icon fileIcon) {
    this.myCustomNameSettingDescriptor = customNameSettingDescriptor;
    this.myCustomFilesSettingDescriptor = customFilesSettingDescriptor;
    this.myCustomizationPresentation = presentation;
    this.myFileIcon = fileIcon;
  }

  public final CustomSettingDescriptor getCustomNameSettingDescriptor() {
    return myCustomNameSettingDescriptor;
  }

  public final CustomSettingDescriptor getCustomFilesSettingDescriptor() {
    return myCustomFilesSettingDescriptor;
  }

  public CustomizationPresentation getCustomizationPresentation() {
    return myCustomizationPresentation;
  }

  public Icon getFileIcon() {
    return myFileIcon;
  }

  public abstract boolean accept(InfraFileSet var1);

  public abstract boolean accept(Module var1);

  public String getInfraConfigName(Module module) {
    InfraFacet facet = getRelevantFacet(module);
    if (facet == null) {
      return myCustomNameSettingDescriptor.defaultValue;
    }
    else {
      CustomSetting.STRING setting = facet.findSetting(this.myCustomNameSettingDescriptor.key);
      return StringUtil.notNullize(setting.getStringValue(), setting.getDefaultValue());
    }
  }

  public List<String> getCustomConfigFileUrls(Module module) {
    InfraFacet InfraFacet = getRelevantFacet(module);
    if (InfraFacet == null) {
      return Collections.emptyList();
    }
    else {
      CustomSetting.STRING setting = InfraFacet.findSetting(this.myCustomFilesSettingDescriptor.key);
      assert setting != null;
      String value = setting.getStringValue();
      if (value == null) {
        return Collections.emptyList();
      }
      else {
        return StringUtil.split(value, ";");
      }
    }
  }

  public List<VirtualFile> findCustomConfigFiles(Module module) {
    List<String> urls = getCustomConfigFileUrls(module);
    if (urls.isEmpty()) {
      return Collections.emptyList();
    }
    else {
      List<VirtualFile> files = new ArrayList<>(urls.size());
      for (String url : urls) {
        VirtualFile configFile = VirtualFileManager.getInstance().findFileByUrl(url);
        ContainerUtil.addIfNotNull(files, configFile);
      }

      return files;
    }
  }

  @Nullable
  private static InfraFacet getRelevantFacet(Module module) {
    InfraFacet facet = findApplicationFacet(module);
    if (facet != null) {
      return facet;
    }
    else if (module.isDisposed()) {
      return null;
    }
    else {
      Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
      for (Module dependentModule : dependencies) {
        InfraFacet dependentFacet = findApplicationFacet(dependentModule);
        if (dependentFacet != null) {
          return dependentFacet;
        }
      }

      return null;
    }
  }

  @Nullable
  private static InfraFacet findApplicationFacet(Module module) {
    InfraFacet facet = InfraFacet.from(module);
    if (facet == null) {
      return null;
    }
    else {
      List<PsiClass> applications = InfraApplicationService.of().getInfraApplications(module);
      return applications.isEmpty() ? null : facet;
    }
  }

  public static class CustomizationPresentation {
    public final String configFileKey;
    public final String customizationPanelTitle;

    public CustomizationPresentation(String configFileKey, String customizationPanelTitle) {
      this.configFileKey = configFileKey;
      this.customizationPanelTitle = customizationPanelTitle;
    }
  }

  public static class CustomSettingDescriptor {
    public final Key<CustomSetting.STRING> key;
    public final @Nls String description;
    public final String defaultValue;

    public CustomSettingDescriptor(Key<CustomSetting.STRING> key, @Nls String description, String defaultValue) {
      this.key = key;
      this.description = description;
      this.defaultValue = defaultValue;
    }

    public CustomSetting.STRING createCustomSetting() {
      return new CustomSetting.STRING(this.key, this.description, this.defaultValue);
    }
  }
}
